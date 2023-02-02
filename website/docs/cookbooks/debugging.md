---
title: Debugging with Scala CLI
sidebar_position: 10
---

Debugging with Scala CLI is very simple. All one needs to do is to pass the `--debug` option, which is available for the `run` and `test` sub-commands.

## Preparing files to debug

Let's start with creating a few example files, which we will run and debug later on:

```scala title=MyClass.scala
object MyClass extends App  {
  println("Line 1")
  println("Line 2")
  println("Line 3")
}
```

```scala title=MyTests.test.scala
//> using dep "org.scalameta::munit::0.7.27"

class MyTests extends munit.FunSuite {
  test("foo") {
    assert(2 + 2 == 4)
  }
}
```

## VS Code with Metals

### Configuration

If you are using **VS Code with Metals**, you will have to define **launch configurations** in the `launch.json` file inside the `.vscode` directory.

Within each configuration you will have to define the following [configuration attributes](https://code.visualstudio.com/docs/editor/debugging#_launchjson-attributes): `type`, `request`, `name`, `buildTarget`, `hostName` and `port`.

If you don't know what are the exact **build target** names of your project, you can check them in [Metals Doctor](https://scalameta.org/metals/docs/editors/vscode/#run-doctor) in the `Build target` column:

![Metals Doctor view](/img/debugging_run_doctor_view.png)

If **no build targets** have been found, perform the following steps:
- run `scala-cli compile .` in the command line.
- when the compilation is complete, run `Connect to build server` in the Metals **build commands** section.

After these steps, build targets should be visible in the Metals Doctor view.

Example `launch.json` configuration file:

```scala title=.vscode/launch.json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "scala",
      "request": "attach",
      "name": "project",
      "buildTarget": "project_183d125c5c",
      "hostName": "localhost",
      "port": 5005
    },
    {
      "type": "scala",
      "request": "attach",
      "name": "project-test",
      "buildTarget": "project_183d125c5c-test",
      "hostName": "localhost",
      "port": 5005
    }	
  ]
}
```

After defining configurations in the `launch.json` file, you should be able to see them in **Configuration dropdown** in the **Run and Debug** view:

![Configuration dropdown](/img/debugging_configuration_dropdown.png)

After setting up the configuration you can proceed to debugging.

### Debugging

Set [breakpoints](https://code.visualstudio.com/docs/editor/debugging#_breakpoints) for the file you would like to debug:

![Setting breakpoints](/img/debugging_setting_breakpoints_vs_code.png)

Run one of the following commands depending on which file you would like to debug:
- run `scala-cli MyClass.scala --debug` if you would like to debug `MyClass.scala` file
- run `scala-cli test MyTests.test.scala --debug` if you would like to debug `MyTests.test.scala` file

After compilation is completed, Scala CLI should stop and **listen for transport dt_socket at port 5005**.

:::info
Please note that 5005 is the default port for debugging with scala-cli. You can always change it by passing `--debug-port` option.
:::

At this moment go to the **Run and Debug** view, select proper configuration from the **Configuration dropdown** and run debugger by clicking **green arrow** on the side:

![Running debugger](/img/debugging_running_debugger.png)

After all these steps, the debugger should stop at the first breakpoint and you can proceed to **debugging** your code using all features delivered by VS Code. For more information check [this guide](https://code.visualstudio.com/docs/editor/debugging).

## IntelliJ IDEA

### Debugging in the attach mode

The first thing that you need to do to start debugging is [setting breakpoints](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html#setting-breakpoints) for the files you want to debug:

![Setting breakpoints](/img/debugging_setting_breakpoints_intellij.png)

Run one of the following commands depending on which file you would like to debug:
- run `scala-cli MyClass.scala --debug` if you would like to debug `MyClass.scala` file
- run `scala-cli test MyTests.test.scala --debug` if you would like to debug `MyTests.test.scala` file

After compilation is completed, Scala CLI should stop and **listen for transport dt_socket at port 5005**.

:::info
Please note that 5005 is the default port for debugging with scala-cli. You can always change it by passing `--debug-port` option.
:::

At this moment, you can attach to process by clicking **Run -> Attach to Process** and choosing process, which is running at port **5005**:

![Attach to Process](/img/debugging_attach_to_process.png)

After all these steps, the debugger should stop at the first breakpoint and you can proceed to **debug** your code using all features delivered by IntelliJ IDEA. For more information check [this guide](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html#analyzing-state).

### Debugging in the listen mode

If you would like to debug in listen mode, add a new **Remote JVM Debug** [configuration](https://www.jetbrains.com/help/idea/run-debug-configuration.html) with the following setup:

![Listen mode configuration](/img/debugging_listen_mode_config.png)

[Set breakpoints](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html#setting-breakpoints) for the files you want to debug:

![Setting breakpoints](/img/debugging_setting_breakpoints_intellij.png)

Run the previously set configuration by clicking on the **green debug button** on the side:

![Running debug configuration](/img/debugging_running_debug_configuration.png)

Run one of the following commands depending on which file you would like to debug:
- run `scala-cli MyClass.scala --debug-mode listen` if you would like to debug `MyClass.scala` file
- run `scala-cli test MyTests.test.scala --debug-mode listen` if you would like to debug `MyTests.test.scala` file

:::info
`attach` is the default mode for debugging with scala-cli. You can always change it by passing `--debug-mode` option. Available modes are: `attach` and `listen`.
:::

After all these steps the debugger should stop at the first breakpoint and you can proceed to **debug** your code using all features delivered by IntelliJ IDEA. For more information check [this guide](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html#analyzing-state).