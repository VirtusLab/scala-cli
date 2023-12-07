---
title: Bloop ⚡️
sidebar_position: 10
---

Scala CLI by default uses Bloop as a build server for compiling code. This approach has its advantages over the `scalac` compiler such as advanced caching and fast compile times, but the process is more complex.
Fortunately for the users, Scala CLI fully manages the Bloop build server. This includes its whole lifecycle, which starts with fetching the artifacts.
This document showcases the `bloop` subcommand that allows you manually manage the Bloop server.
It also goes through the server's lifecycle and the interactions that Scala CLI has with it.

:::caution
The `bloop` sub-command is restricted and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

### Starting the server

Whenever the code is compiled using Bloop, the first step is checking if the server is online, as it is launched as a daemon thread there's a chance it may have been launched during a past compilation.
However, if the server is offline then Scala CLI needs to start and configure it.

The configuration file for Bloop is created after analyzing the options collected from command line flags and using directives.
The default location of the file is `.scala-build/bloop/project_name.json`.

The last thing before launching the server is downloading its artifacts from Maven Central via Coursier if they are not already present in the local cache.
:::tip
When working in an environment with restricted access to the web, using Bloop can be disabled with the `--server=false` flag. Also, see the [section about the Offline mode](../../guides/power/offline.md).
:::

Bloop is started as a separate JVM process, parameters of this process can be configured using arguments passed to the invoked subcommand ([see compilation server options](../../reference/cli-options.md#compilation-server-options)).
They also depend on the JVM version chosen for building the project, it cannot be higher than the version of the JVM running Bloop. If such a case is detected, the build server has to be restarted with a sufficiently high JVM.
Note that the default version of the JVM for Bloop is 17, so if your `JAVA_HOME` refers to an older version of Java, Scala CLI will fetch the one you need. You can also override the JVM version Bloop runs on with the `--bloop-jvm` option.
To start the Bloop server manually you can use the `bloop start` subcommand:
```bash
scala-cli --power bloop start
```

### Communicating with the server

During the communication process, Scala CLI acts mostly as an intermediary between Bloop and the build client.
The build client can be either the user invoking the tool from the command line or the IDE seeing Scala CLI as a build server.
The behavior is mostly the same in both cases and is based on forwarding the messages. The messages being forwarded need to sometimes be edited as a result of preprocessing Scala CLI does, e.g. generating script sources.

The main difference between running on the command line and serving an IDE is the information that gets through to the client.
While an IDE receives all the messages that Bloop sends, the user only receives the relevant information, like warnings and errors coming from the compilation process.

### Killing the server

In general, the Bloop server is started as a daemon process that sticks around even after Scala CLI exits.
The server can sometimes be automatically killed and restarted if a configuration change requires that, e.g. JVM version requested by the build is too high.

However, sometimes it is needed to restart the Bloop server manually, for that the `bloop exit` subcommand can be used:
```bash
scala-cli --power bloop exit
```
