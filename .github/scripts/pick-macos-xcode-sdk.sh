#!/bin/bash
set -e
sudo xcode-select -s "/Applications/Xcode_13.1.0.app" || ( ls /Applications && exit 1 )

sudo rm -Rf /Library/Developer/CommandLineTools/SDKs/*
