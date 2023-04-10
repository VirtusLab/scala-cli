---
title: Fix
sidebar_position: 20
---

The fix command can be used to perform fixes on a Scala CLI project.

## Migrating directives
It is recommended to keep all `using` directives centralized in one file per scope. Fix command along with the `--migrate-directives` option will migrate all directives of given inputs to the following project files: 
- `project.scala` for main scope sources
- `project.test.scala` for [test scope sources](/docs/commands/test#test-sources)

Note that if the project file will not be found, Scala CLI will create it in the [project root](/docs/reference/root-dir) directory.

Migrating directives is performed for both (main and test) scopes by default. You can pass `--main-scope` or `--test-scope` option to define for which scope directives should be migrated.

### Examples

Let's say we have the following file structure:

```
project
│   project.scala
│
└───dir
│   │   Foo.scala
│   │   Bar.scala
│   
└───test
    │   Baz.scala
    │   Qux.scala
```

#### Default migrate directives

After running

```bash ignore
scala-cli fix --migrate-directives .
```

- Scala CLI will move all `using` directives from `Foo.scala` and `Bar.scala` files to the existing `project.scala` file
- since `project.test.scala` doesn't exist, Scala CLI will create it and move all `using` directives from `Baz.scala` and `Qux.scala` files to it

#### Migrate directives with specified scope

After running

```bash ignore
scala-cli fix --migrate-directives --main-scope .
```

- Scala CLI will move all `using` directives from `Foo.scala` and `Bar.scala` files to the existing `project.scala` file
- since `--main-scope` option has been passed, Scala CLI will not perform directives migration for test scope - `Baz.scala` and `Qux.scala` files will not be changed and no `project.test.scala` will be created
