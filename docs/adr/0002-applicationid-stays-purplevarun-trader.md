# The applicationId stays `com.purplevarun.trader`

The game was renamed to Saudagar and the repo to `saudagar_android`, but the applicationId keeps
the old `trader` name — deliberately. Android treats applicationId as identity: changing it makes
the update an uninstall-reinstall, wiping every installed save and breaking the signed-update
path. It is invisible to players (the launcher shows `app_name`, which is "Saudagar"), so the
cleanup buys nothing and costs real user data. Do not "fix" it to match the name.
