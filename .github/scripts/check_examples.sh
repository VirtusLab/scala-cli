#!/usr/bin/env bash

git add .

if [[ $(git status --porcelain examples) ]]; then
    
    echo "There are changes in examples that was not commited:"
    git diff --cached examples
    echo "Changes above should be commited! Note that your branch may not be rebased on latest master and this may cause difference above."
    echo "Try running rebasing this branch on latast master, run .github/scripts/check_docs.sh and commit all changes in examples directory"
    exit 1
else
    echo "All is fine!"
fi