---
title: Working directory
sidebar_position: 5
---

- Scala CLI needs a working directory:
  - to write mapped sources
  - to write class files
  - for Bloop

- default: depends on the first passed element:
  - directory: `./.scala` in it
  - `.scala` / `.sc` / `.java`: `./.scala` in its directory
  - URL / pipe / proc subst: in home dir
