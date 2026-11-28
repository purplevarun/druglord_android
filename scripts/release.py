#!/usr/bin/env python3
"""Cut a release: bump the version, run the suite, build the signed APK, tag, ship it.

    python scripts/release.py 1.2.3            # bump, test, build, tag v1.2.3, release
    python scripts/release.py 1.2.3 --dry-run  # show the plan, change nothing

The version lives in app/build.gradle.kts — the script rewrites versionCode and
versionName there and commits the bump. versionCode is major*10000 + minor*100 +
patch, so it rises with every semver bump (until a digit overflows; patch caps at 99,
minor at 99). The release APK must be signed: signing needs release.keystore and
keystore.properties at the repo root. Pushes main and the tag, then creates a GitHub
release named after the version with saudagar-<version>.apk attached.
"""

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADLE_FILE = ROOT / "app" / "build.gradle.kts"
APK_BUILT = ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk"


def run(cmd, check=True, capture=False):
    print("+", " ".join(str(c) for c in cmd))
    result = subprocess.run(
        [str(c) for c in cmd],
        cwd=ROOT,
        capture_output=capture,
        text=capture,
    )
    if check and result.returncode != 0:
        if capture:
            print(result.stdout)
            print(result.stderr, file=sys.stderr)
        sys.exit(f"command failed: {' '.join(str(c) for c in cmd)}")
    return result


def parse_version(text):
    match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)", text)
    if not match:
        sys.exit(f"version must be major.minor.patch, got {text!r}")
    major, minor, patch = (int(part) for part in match.groups())
    if minor > 99 or patch > 99:
        sys.exit("minor and patch must stay under 100 — they each get two versionCode digits")
    return major, minor, patch


def version_code(major, minor, patch):
    return major * 10_000 + minor * 100 + patch


def current_version():
    source = GRADLE_FILE.read_text()
    code = re.search(r"versionCode = (\d+)", source)
    name = re.search(r'versionName = "([^"]+)"', source)
    if not code or not name:
        sys.exit("could not read versionCode/versionName from app/build.gradle.kts")
    return int(code.group(1)), name.group(1)


def bump_version(version, code):
    source = GRADLE_FILE.read_text()
    source = re.sub(r"versionCode = \d+", f"versionCode = {code}", source, count=1)
    source = re.sub(r'versionName = "[^"]+"', f'versionName = "{version}"', source, count=1)
    GRADLE_FILE.write_text(source)


def previous_tag():
    result = run(["git", "tag", "--sort=-v:refname"], capture=True)
    tags = result.stdout.split()
    return tags[0] if tags else None


def release_notes(version, since_tag):
    log = run(
        ["git", "log", "--format=- %s", f"{since_tag}..HEAD"] if since_tag else
        ["git", "log", "--format=- %s", "-20"],
        capture=True,
    )
    commits = log.stdout.strip() or "- Maintenance release"
    lines = [
        f"Saudagar {version}: an offline trading game of trains and cities, India 2000.",
        "",
        "Sideload the attached APK — no ads, no permissions, no network.",
        "",
        "### Changes",
        "",
        commits,
    ]
    if since_tag:
        lines.append("")
        lines.append(f"Full history: {since_tag}...v{version}")
    return "\n".join(lines)


def sdk_dir():
    local = ROOT / "local.properties"
    if local.exists():
        for line in local.read_text().splitlines():
            if line.startswith("sdk.dir="):
                return Path(line.split("=", 1)[1])
    return Path.home() / "Library" / "Android" / "sdk"


def apk_is_signed(apk):
    tools = sdk_dir() / "build-tools"
    candidates = sorted(tools.glob("*/apksigner"), reverse=True) if tools.exists() else []
    apksigner = candidates[0] if candidates else "apksigner"
    result = subprocess.run(
        [str(apksigner), "verify", "--print-certs", str(apk)],
        capture_output=True,
        text=True,
    )
    return result.returncode == 0 and "Signer #1 certificate" in result.stdout


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version", help="major.minor.patch, e.g. 1.0.0")
    parser.add_argument("--dry-run", action="store_true", help="show the plan and stop")
    parser.add_argument(
        "--allow-unsigned", action="store_true",
        help="ship the APK even if no signing key was found (updates will not install over signed copies)",
    )
    args = parser.parse_args()

    major, minor, patch = parse_version(args.version)
    code = version_code(major, minor, patch)
    old_code, old_name = current_version()
    if code <= old_code:
        sys.exit(f"version {args.version} (code {code}) must beat the current {old_name} (code {old_code})")

    tag = f"v{args.version}"
    out_dir = ROOT / "build" / "releases"
    apk = out_dir / f"saudagar-{args.version}.apk"
    since = previous_tag()
    dirty = run(["git", "status", "--porcelain"], capture=True).stdout.strip()
    print(f"plan: {old_name} (code {old_code}) -> {args.version} (code {code}), tag {tag}")
    print(f"      apk -> {apk}, release on GitHub, notes since {since or 'the start'}")
    if args.dry_run:
        return
    if dirty:
        sys.exit("worktree is dirty — commit or stash before releasing")
    run(["git", "fetch", "--tags"])

    print("\n== tests ==")
    run(["./gradlew", "test"])
    bump_version(args.version, code)
    print("\n== build ==")
    run(["./gradlew", ":app:assembleRelease"])
    if not APK_BUILT.exists():
        sys.exit(f"expected {APK_BUILT}")
    if not apk_is_signed(APK_BUILT):
        if not args.allow_unsigned:
            sys.exit(
                "the APK is unsigned — release.keystore/keystore.properties are missing or "
                "misread. Refusing to ship an update no signed copy can install over "
                "(pass --allow-unsigned to ship anyway)",
            )
        print("warning: shipping an unsigned APK")
    out_dir.mkdir(parents=True, exist_ok=True)
    apk.write_bytes(APK_BUILT.read_bytes())

    print("\n== tag and push ==")
    run(["git", "add", str(GRADLE_FILE.relative_to(ROOT))])
    run(["git", "commit", "-m", f"Saudagar {args.version}"])
    run(["git", "tag", tag])
    run(["git", "push", "origin", "main", tag])

    print("\n== GitHub release ==")
    notes = release_notes(args.version, since)
    run([
        "gh", "release", "create", tag, str(apk),
        "--title", f"Saudagar {args.version}",
        "--notes", notes,
    ])
    print(f"\nshipped {tag} — {apk} is attached to the release and kept locally too")


if __name__ == "__main__":
    main()
