---
title: Fix
sidebar_position: 20
---

The fix command can be used to perform fixes on a Scala CLI project.

## Migrating directives
It is recommended to keep all `using` directives centralised in one file. Fix command along with the `--migrate-directives` option will migrate all directives of given inputs to the `project.scala` configuration file (if the file does not exist, Scala CLI will create it in the project root directory).

The following command will move all directives inside the current working directory to the `project.scala` file:
```
scala-cli fix --migrate-directives .
```

Please note that this is equivalent to:
```bash ignore
scala-cli migrate-directives .
```
