# build (from git):
snapcraft

# install:
snap install --devmode imaillist_*.snap

# current config
snap get imaillist

snap set imaillist key=value

# start service
snap start imaillist

# show/follow logs
snap logs [-f] imaillist
