#!/usr/bin/env bash
set -e

git config --global user.name "gh-actions"
git config --global user.email "actions@github.com"

cd website
yarn install
yarn build
yarn deploy
