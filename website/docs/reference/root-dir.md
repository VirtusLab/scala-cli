---
title: Project root directory
sidebar_position: 5
---

## Usage

Scala CLI needs a root directory:
  - to write mapped sources
  - to write class files
  - for Bloop

## Setting root directory

First of all, Scala CLI checks every passed input (in the same order in which inputs were passed) for the `project.settings.scala` file:
- If the `project.settings.scala` file is passed explicitly as a **source**, Scala CLI sets its parent directory as the root directory.
- If the input is a **directory**, Scala CLI looks for the `project.settings.scala` inside this directory. If the file is found, Scala CLI sets the passed directory as the root directory.

If more than one `project.settings.scala` file is found, Scala CLI uses only **the first one** to set the root directory and raises **warning** saying which one was used.

If no `project.settings.scala` files are found, Scala CLI sets the root directory based on the first file/directory input:
- If the input is a **directory**, it is set as the root directory. 
- If the input is a **file**, Scala CLI sets its parent directory as the root directory. 

If more then one file/directory input has ben passed Scala CLI raises the warning saying which directory has been set as the project root directory.

If no `project.settings.scala` files are found and no file/directory inputs have ben passed, Scala CLI sets the current working directory (where Scala CLI was invoked from) as the project root directory.

#### Example

Let's say we have the following file structure:

```
project
│   project.settings.scala
│
└───dir1
│   │   file1.scala
│   │
│   └───dir2
│       │   project.settings.scala
│       │   file2.scala
│   
└───dir3
    │   project.settings.scala
    │   file3.scala
```

And user runs the following command:
```
project> scala-cli dir1/file1.scala dir1/dir2 dir3/project.settings.scala
```

Scala CLI will find 2 `project.settings.scala` files:
- inside `dir2`, since this directory was passed as an input and it has `project.settings.scala` inside.
- inside `dir3`, since `dir3/project.settings.scala` was passed explicitly as a source

`dir1/dir2` was passed before `dir3/project.settings.scala`, so `dir2` will be set as the **root** directory for this build. 

Since more than one `project.settings.scala` has been found, Scala CLI will raise the warning saying that more than one `project.settings.scala` file has been found and `dir1/dir2` has been set as the project root directory.
