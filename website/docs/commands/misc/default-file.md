---
title: Default File
sidebar_position: 1
---

The `default-file` sub-command provides sensible default content for files
such as `.gitignore` or for GitHub actions workflows, for Scala CLI projects.

To list the available files, pass it `--list`:
```text
$ scala-cli default-file --list
.gitignore
.github/workflows/ci.yml
```

Get the content of a default file with
```text
$ scala-cli default-file .gitignore
/.bsp/
/.scala-build/
```

Optionally, write the content of one or more default files by passing `--write`:
```text
$ scala-cli default-file --write .gitignore .github/workflows/ci.yml
Wrote .gitignore
Wrote .github/workflows/ci.yml
```
