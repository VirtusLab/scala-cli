---
title: SBT and Mill
sidebar_position: 12
---

Scala CLI allows to export you current build into sbt or mill. It means that if your project will need something that Scala CLI does not provide like second module, you can export your project to a build tool of choice.

Why do we need that? We do not want to block development of your project and at the same time we do not want to introduce complexity that multi-module build or task and plugin system introduce.

To export the project just run `scala-cli export --sbt <standard-options>` or `scala-cli export --mill <standard-options>` to export your project to sbt or mill. This command will create a copy of your sources, resources and local jars. It will also download gist and other non-local inputs. By default project is exported to `dest` directory but you can control that with `-o` option.