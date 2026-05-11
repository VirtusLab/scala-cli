package scala.cli.commands.tests

import com.eed3si9n.expecty.Expecty.assert as expect

import scala.build.options.BuildOptions
import scala.cli.commands.repl.{JShellRunner, Repl}

class JShellRunnerTests extends munit.FunSuite {

  private def withTempJavaHome[T](isWindows: Boolean =
    false)(f: BuildOptions.JavaHomeInfo => T): T = {
    val ext      = if (isWindows) ".exe" else ""
    val javaHome = os.temp.dir(prefix = "scala-cli-jshell-test-jdk-", deleteOnExit = false)
    os.makeDir.all(javaHome / "bin")
    os.write.over(javaHome / "bin" / s"java$ext", "")
    os.write.over(javaHome / "bin" / s"jshell$ext", "")
    val info = BuildOptions.JavaHomeInfo(
      javaHome = javaHome,
      javaCommand = (javaHome / "bin" / s"java$ext").toString,
      version = 17
    )
    f(info)
  }

  test("parse repl args extracts init script and quit flag") {
    val args = Seq(
      "--repl-init-script",
      "System.out.println(1);",
      "--repl-quit-after-init",
      "-Xfatal-warnings"
    )
    val parsed = JShellRunner.parseReplArgs(args).toOption.get
    expect(parsed.initScriptOpt.contains("System.out.println(1);"))
    expect(parsed.quitAfterInit)
    expect(parsed.remainingArgs == Seq("-Xfatal-warnings"))
  }

  test("parse repl args extracts init script from file") {
    val initScriptFile = os.temp(prefix = "scala-cli-jshell-init-test-", suffix = ".jsh")
    val initScript     =
      """System.out.println("from file");
        |System.out.println(2);
        |""".stripMargin
    os.write.over(initScriptFile, initScript)
    val parsed = JShellRunner
      .parseReplArgs(Seq("--repl-init-script-file", initScriptFile.toString))
      .toOption
      .get
    expect(parsed.initScriptOpt.contains(initScript))
    expect(parsed.remainingArgs.isEmpty)
  }

  test("parse repl args rejects init script string and file together") {
    val initScriptFile = os.temp(prefix = "scala-cli-jshell-init-test-", suffix = ".jsh")
    os.write.over(initScriptFile, "System.out.println(1);")
    val parsed = JShellRunner.parseReplArgs(Seq(
      "--repl-init-script",
      "System.out.println(1);",
      "--repl-init-script-file",
      initScriptFile.toString
    ))
    expect(parsed.isLeft)
  }

  test("parse repl args rejects missing init script file") {
    val missing = os.temp(prefix = "scala-cli-jshell-init-test-", suffix = ".jsh")
    os.remove(missing)
    val parsed = JShellRunner.parseReplArgs(Seq("--repl-init-script-file", missing.toString))
    expect(parsed.isLeft)
  }

  test("default REPL maps init script file option to the Scala REPL init script flag") {
    val initScriptFile = os.temp(prefix = "scala-cli-repl-init-test-", suffix = ".sc")
    val otherInitFile  = os.temp(prefix = "scala-cli-repl-init-test-other-", suffix = ".sc")
    os.write.over(initScriptFile, """println("first")""")
    os.write.over(otherInitFile, """println("second")""")
    val args = Repl.defaultReplArgs(
      additionalArgs =
        Seq("--repl-init-script-file", initScriptFile.toString, "--repl-quit-after-init"),
      replInitScriptFileOpt = Some(otherInitFile.toString)
    ).toOption.get
    expect(args == Seq(
      "--repl-init-script",
      """println("first")""",
      "--repl-quit-after-init",
      "--repl-init-script",
      """println("second")"""
    ))
  }

  test("commandFor adds classpath java opts startup and load files") {
    withTempJavaHome() { javaHomeInfo =>
      val cpRoot = os.temp.dir(prefix = "scala-cli-jshell-cp-", deleteOnExit = false)
      val cp     = Seq(cpRoot / "cp1", cpRoot / "cp2")
      val res    = JShellRunner.commandFor(
        javaHomeInfo = javaHomeInfo,
        javaOpts = Seq("-Xmx1g", "-Dfoo=bar"),
        classPath = cp,
        programArgs = Seq("load.jsh"),
        initScriptOpt = Some("""System.out.println("hello");"""),
        quitAfterInit = true,
        currentEnv = Map("PATH" -> "/usr/bin")
      )
      assert(res.isRight)
      val command = res.toOption.get
      expect(command.jshellCommand.endsWith("/bin/jshell"))
      expect(command.args.contains("--class-path"))
      expect(command.args.contains("-J-Xmx1g"))
      expect(command.args.contains("-J-Dfoo=bar"))
      expect(command.args.contains("load.jsh"))
      expect(command.args.contains("--startup"))
      expect(command.args.last.endsWith(".jsh"))
      expect(command.extraEnv.contains("JAVA_HOME"))
      expect(command.extraEnv.contains("PATH"))
    }
  }

  test("commandFor uses .exe on Windows") {
    withTempJavaHome(isWindows = true) { javaHomeInfo =>
      val res = JShellRunner.commandFor(
        javaHomeInfo = javaHomeInfo,
        javaOpts = Nil,
        classPath = Nil,
        programArgs = Nil,
        initScriptOpt = None,
        quitAfterInit = false,
        currentEnv = Map.empty,
        isWindows = true
      )
      assert(res.isRight)
      expect(res.toOption.get.jshellCommand.endsWith("""\bin\jshell.exe""") ||
        res.toOption.get.jshellCommand
          .endsWith("/bin/jshell.exe"))
    }
  }

  test("commandFor fails for JDK 8") {
    withTempJavaHome() { javaHomeInfo =>
      val res = JShellRunner.commandFor(
        javaHomeInfo = javaHomeInfo.copy(version = 8),
        javaOpts = Nil,
        classPath = Nil,
        programArgs = Nil,
        initScriptOpt = None,
        quitAfterInit = false,
        currentEnv = Map.empty
      )
      assert(res.isLeft)
    }
  }
}
