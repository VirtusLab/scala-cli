#!/usr/bin/env bash
set -eu

snapcraft -d
mkdir .snapcraft    
echo $SNAPCRAFT_LOGIN_FILE | base64 --decode --ignore-garbage > .snapcraft/snapcraft.cfg
snapcraft login --with .snapcraft/snapcraft.cfg
snapcraft push *.snap --release stable