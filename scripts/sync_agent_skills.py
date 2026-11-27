#!/usr/bin/env python3
"""Vendors Matt Pocock's agent skills into this repo, for every coding agent.

The skills come from https://github.com/mattpocock/skills (MIT licence), pinned to one commit.
The same files are written to each folder an agent reads project skills from:

  .agents/skills/    GitHub Copilot, Codex, Cursor, Gemini CLI, OpenCode, Amp and others
  .claude/skills/    Claude Code
  .devin/skills/     Devin
  .windsurf/skills/  Windsurf

Usage:
  python scripts/sync_agent_skills.py          download the pinned commit and rewrite every folder
  python scripts/sync_agent_skills.py --check  offline: fail if a folder is missing, malformed or
                                               differs from the others (CI runs this)

To update the skills, change UPSTREAM_COMMIT and run without --check.
"""

import argparse
import json
import shutil
import sys
import tarfile
import tempfile
import urllib.request
from pathlib import Path

UPSTREAM_REPO = "mattpocock/skills"
UPSTREAM_COMMIT = "6f7f7831a625f040a8e06a0e7954826b87ce775a"

REPO_ROOT = Path(__file__).resolve().parent.parent
SKILL_FOLDERS = {
    ".agents/skills": "GitHub Copilot, Codex, Cursor, Gemini CLI, OpenCode, Amp and others",
    ".claude/skills": "Claude Code",
    ".devin/skills": "Devin",
    ".windsurf/skills": "Windsurf",
}
MANIFEST_NAME = "vendored-skills.json"
LICENSE_NAME = "LICENSE-mattpocock-skills.txt"
README_NAME = "README.md"


def download_upstream(workdir: Path) -> Path:
    """Downloads and unpacks the pinned commit into an empty folder; returns the unpacked root."""
    url = f"https://codeload.github.com/{UPSTREAM_REPO}/tar.gz/{UPSTREAM_COMMIT}"
    archive = workdir / "upstream.tar.gz"
    request = urllib.request.Request(url, headers={"User-Agent": "sync-agent-skills"})
    with urllib.request.urlopen(request, timeout=60) as response, open(archive, "wb") as out:
        shutil.copyfileobj(response, out)
    unpacked = workdir / "unpacked"
    unpacked.mkdir()
    with tarfile.open(archive) as tar:
        # The "data" filter refuses absolute paths, links out of the folder and special files.
        tar.extractall(unpacked, filter="data")
    (root,) = [entry for entry in unpacked.iterdir() if entry.is_dir()]
    return root


def promoted_skills(upstream: Path) -> list[Path]:
    """The skills the upstream repo ships to users: the ones its Claude Code plugin lists."""
    plugin = json.loads((upstream / ".claude-plugin" / "plugin.json").read_text(encoding="utf-8"))
    return [upstream / relative for relative in plugin["skills"]]


def readme_text(names: list[str]) -> str:
    folders = "\n".join(f"- `{folder}/` ({agents})" for folder, agents in SKILL_FOLDERS.items())
    skills = "\n".join(f"- `{name}`" for name in names)
    return f"""# Agent skills (vendored)

Matt Pocock's agent skills, copied from https://github.com/{UPSTREAM_REPO} at commit
`{UPSTREAM_COMMIT}`, under the MIT licence in `{LICENSE_NAME}`.

Do not edit these files here. To update them, change `UPSTREAM_COMMIT` in
`scripts/sync_agent_skills.py` and run it; it rewrites this folder and every copy of it.

The same files are in each folder a coding agent reads skills from:

{folders}

Skills included:

{skills}
"""


def write_folder(target: Path, upstream: Path, skills: list[Path]) -> None:
    names = [skill.name for skill in skills]
    target.mkdir(parents=True, exist_ok=True)
    # Remove only what this script put here before, so a project's own skills are left alone.
    previous = target / MANIFEST_NAME
    if previous.is_file():
        for name in json.loads(previous.read_text(encoding="utf-8"))["skills"]:
            shutil.rmtree(target / name, ignore_errors=True)
    for skill in skills:
        shutil.copytree(skill, target / skill.name)
    shutil.copyfile(upstream / "LICENSE", target / LICENSE_NAME)
    (target / README_NAME).write_text(readme_text(names), encoding="utf-8", newline="\n")
    manifest = {"upstream": UPSTREAM_REPO, "commit": UPSTREAM_COMMIT, "skills": names}
    (target / MANIFEST_NAME).write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8", newline="\n")


def sync() -> None:
    with tempfile.TemporaryDirectory(prefix="agent-skills-") as workdir:
        upstream = download_upstream(Path(workdir))
        skills = promoted_skills(upstream)
        for folder in SKILL_FOLDERS:
            write_folder(REPO_ROOT / folder, upstream, skills)
    print(f"Wrote {len(skills)} skills from {UPSTREAM_REPO}@{UPSTREAM_COMMIT[:7]} to {len(SKILL_FOLDERS)} folders.")


def vendored_files(folder: Path, names: list[str]) -> dict[str, bytes]:
    """Every vendored file in a folder, keyed by relative path, with line endings normalised."""
    files = {}
    for entry in [*names, LICENSE_NAME, README_NAME, MANIFEST_NAME]:
        path = folder / entry
        paths = [path] if path.is_file() else sorted(p for p in path.rglob("*") if p.is_file())
        for file in paths:
            files[file.relative_to(folder).as_posix()] = file.read_bytes().replace(b"\r\n", b"\n")
    return files


def frontmatter_problems(skill_md: Path, name: str) -> list[str]:
    lines = skill_md.read_text(encoding="utf-8").splitlines()
    if not lines or lines[0].strip() != "---" or "---" not in [line.strip() for line in lines[1:]]:
        return [f"{skill_md}: no frontmatter block"]
    header = lines[1 : [line.strip() for line in lines[1:]].index("---") + 1]
    fields = {}
    for line in header:
        key, _, value = line.partition(":")
        if value:
            fields[key.strip()] = value.strip().strip("'\"")
    problems = []
    if fields.get("name") != name:
        problems.append(f"{skill_md}: frontmatter name is {fields.get('name')!r}, expected {name!r}")
    if not fields.get("description"):
        problems.append(f"{skill_md}: frontmatter has no description")
    return problems


def check() -> list[str]:
    primary_folder = next(iter(SKILL_FOLDERS))
    primary = REPO_ROOT / primary_folder
    manifest_path = primary / MANIFEST_NAME
    if not manifest_path.is_file():
        return [f"{manifest_path} is missing: run scripts/sync_agent_skills.py"]
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    problems = []
    if manifest["commit"] != UPSTREAM_COMMIT:
        problems.append(f"{primary_folder} holds commit {manifest['commit'][:7]}, script pins {UPSTREAM_COMMIT[:7]}")
    names = manifest["skills"]
    for name in names:
        skill_md = primary / name / "SKILL.md"
        if not skill_md.is_file():
            problems.append(f"{skill_md} is missing")
        else:
            problems += frontmatter_problems(skill_md, name)
    expected = vendored_files(primary, names)
    for folder in list(SKILL_FOLDERS)[1:]:
        actual = vendored_files(REPO_ROOT / folder, names)
        for path in sorted(expected.keys() - actual.keys()):
            problems.append(f"{folder}/{path} is missing")
        for path in sorted(actual.keys() - expected.keys()):
            problems.append(f"{folder}/{path} is not in {primary_folder}")
        for path in sorted(expected.keys() & actual.keys()):
            if expected[path] != actual[path]:
                problems.append(f"{folder}/{path} differs from {primary_folder}/{path}")
    return problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="verify the folders without downloading")
    if parser.parse_args().check:
        problems = check()
        for problem in problems:
            print(f"error: {problem}", file=sys.stderr)
        if not problems:
            print(f"All {len(SKILL_FOLDERS)} agent skill folders are present, well formed and identical.")
        return 1 if problems else 0
    sync()
    return 0


if __name__ == "__main__":
    sys.exit(main())
