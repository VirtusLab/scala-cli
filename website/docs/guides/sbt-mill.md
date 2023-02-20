---
title: SBT and Mill ⚡️
sidebar_position: 55
---

Scala CLI lets you export your current build into sbt or Mill.
This means that if your project needs something that Scala CLI doesn’t provide — such as a second module — you can export your project to your build tool of choice.

Why do we need this?
Basically we don’t want to block the development of your project.
But at the same time, we don’t want to introduce the complexity that multi-module builds and tasks and plugin systems introduce — at least not until that complexity is needed.

To export a project, run this command to export to sbt:

```sh
scala-cli export --sbt <standard-options>
```

Or use this command to export to Mill:

```sh
scala-cli export --mill <standard-options>
```

These commands create a copy of your sources, resources, and local JARs.
They also download gists and other non-local inputs.
By default the project is exported to a `dest` directory, but you can control that with the `-o` option.
