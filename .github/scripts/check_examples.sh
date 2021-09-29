#!/usr/bin/env bash

if [[ $(git status --porcelain examples) ]]; then
    echo "There are changes in examples that was not commited:"
    git status --porcelain examples
    exit 1
else
    echo "All is fine!"
fi