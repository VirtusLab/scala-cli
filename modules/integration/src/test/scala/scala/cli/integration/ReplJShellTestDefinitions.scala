package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.{Properties, Try}

trait ReplJShellTestDefinitions { this: ReplTestDefinitions =>
  protected val jshellDryRunPrefix: String = "JShell dry run:"
  protected val runInJShellPrefix: String  = "Running in JShell:"

  /** JShell integration tests need a JDK (resolved via JAVA_HOME, then the test JVM) with
    * `bin/jshell` and spec version >= 9.
    */
  protected lazy val jshellAvailable: Boolean = {
    val javaHomeOpt: Option[os.Path] =
      sys.env.get("JAVA_HOME").filter(_.nonEmpty).map(os.Path(_, os.pwd))
        .orElse(Option(sys.props("java.home")).filter(_.nonEmpty).map(os.Path(_, os.pwd)))
    val jshellExe          = if Properties.isWin then "jshell.exe" else "jshell"
    val jshellExists       = javaHomeOpt.exists(h => os.exists(h / "bin" / jshellExe))
    val javaSpecVersionOpt =
      Try(sys.props("java.specification.version").toInt).toOption.orElse {
        Try(sys.props("java.specification.version").stripPrefix("1.").toInt).toOption
      }
    jshellExists && javaSpecVersionOpt.exists(_ >= 9)
  }

  /** Dry-run `repl` with `--power --jshell` (mirrors [[dryRun]] for the JShell backend). */
  protected def dryRunJshell(
    testInputs: TestInputs = TestInputs.empty,
    cliOptions: Seq[String] = Seq.empty,
    useExtraOptions: Boolean = true,
    check: Boolean = true
  ): os.CommandResult =
    testInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "repl",
        ".",
        "--jshell",
        "--repl-dry-run",
        cliOptions,
        if useExtraOptions then extraOptions else Seq.empty
      ).call(cwd = root, mergeErrIntoOut = true, check = check)
    }

  protected def jshellOutput(res: os.CommandResult): String =
    TestUtil.removeAnsiColors(res.out.text())

  /** Run JShell via `repl --jshell` with `--repl-init-script-file` + `--repl-quit-after-init`.
    *
    * @param replCliOptions
    *   forwarded after [[initScript]] (defaults to [[extraOptions]]). Use [[TestUtil.extraOptions]]
    *   alone when inputs compile Scala 2.x sources: Scala 2 scalac rejects `--repl-quit-after-init`
    *   / `--repl-init-script-file`, while default Scala (see launcher) accepts them for JShell
    *   mapping.
    */
  protected def runInJShell(
    initScript: String,
    testInputs: TestInputs = TestInputs.empty,
    cliOptions: Seq[String] = Seq.empty,
    replCliOptions: Seq[String] = extraOptions,
    check: Boolean = true,
    env: Map[String, String] = Map.empty
  )(
    runAfterRepl: os.CommandResult => Unit,
    runBeforeReplAndGetExtraCliOpts: () => Seq[os.Shellable] = () => Seq.empty
  ): Unit = {
    testInputs.fromRoot { root =>
      val potentiallyExtraCliOpts = runBeforeReplAndGetExtraCliOpts()
      val initScriptFile          = root / ".scala-cli-jshell-init.jsh"
      os.write.over(initScriptFile, initScript)
      runAfterRepl(
        os.proc(
          TestUtil.cli,
          "--power",
          "repl",
          ".",
          "--jshell",
          "--repl-quit-after-init",
          "--repl-init-script-file",
          initScriptFile.toString,
          replCliOptions,
          cliOptions,
          potentiallyExtraCliOpts
        ).call(
          cwd = root,
          mergeErrIntoOut = true,
          env = env,
          check = check
        )
      )
    }
  }

  protected def runInJShellOnJvm(javaVersion: Int, extraInitScript: String = "")(
    check: os.CommandResult => Unit
  ): Unit = {
    val sentinel   = s"java-feature=$javaVersion"
    val initScript =
      s"""int __feature = Runtime.version().feature();
         |if (__feature != $javaVersion) {
         |  throw new RuntimeException("Unexpected JDK feature: " + __feature);
         |}
         |else {
         |$extraInitScript
         |  System.out.println("$sentinel");
         |}
         |""".stripMargin
    runInJShell(initScript = initScript, cliOptions = Seq("--jvm", javaVersion.toString)) { res =>
      val out = jshellOutput(res)
      expect(out.contains(sentinel))
      check(res)
    }
  }

  test(s"$jshellDryRunPrefix default") {
    val res = dryRunJshell(TestInputs.empty)
    expect(res.exitCode == 0)
    expect(res.out.text().toLowerCase.contains("jshell"))
  }

  test(s"$jshellDryRunPrefix pure Java defaults to JShell") {
    TestInputs(
      os.rel / "Main.java" ->
        """public class Main {
          |  public static void main(String[] args) {}
          |}
          |""".stripMargin
    ).fromRoot { root =>
      val res = os
        .proc(TestUtil.cli, "repl", ".", "--repl-dry-run", extraOptions)
        .call(cwd = root, mergeErrIntoOut = true)
      expect(res.out.text().toLowerCase.contains("jshell"))
    }
  }

  test(s"$jshellDryRunPrefix mixed Scala/Java defaults to Scala REPL, --jshell switches backend") {
    val inputs = TestInputs(
      os.rel / "Main.java" ->
        """public class Main {
          |  public static void main(String[] args) {}
          |}
          |""".stripMargin,
      os.rel / "Main.scala" ->
        """object MainScala {
          |  def value = 1
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val defaultRes = os
        .proc(TestUtil.cli, "repl", ".", "--repl-dry-run", extraOptions)
        .call(cwd = root, mergeErrIntoOut = true)
      expect(!defaultRes.out.text().toLowerCase.contains("jshell"))
      val jshellRes = os
        .proc(
          TestUtil.cli,
          "--power",
          "repl",
          ".",
          "--jshell",
          "--repl-dry-run",
          extraOptions
        )
        .call(cwd = root, mergeErrIntoOut = true)
      expect(jshellRes.out.text().toLowerCase.contains("jshell"))
    }
  }

  test(s"$jshellDryRunPrefix calling repl with a directory with no scala artifacts") {
    val res =
      dryRunJshell(TestInputs(os.rel / "Testing.java" -> "public class Testing {}"))
    expect(res.exitCode == 0)
    expect(res.out.text().toLowerCase.contains("jshell"))
  }

  test(s"$jshellDryRunPrefix --jshell with --test scope") {
    val res = dryRunJshell(
      TestInputs(
        os.rel / "Main.java" ->
          """public class Main {
            |  public static void main(String[] args) {}
            |}
            |""".stripMargin,
        os.rel / "Main.test.java" -> "public class MainTest {}"
      ),
      cliOptions = Seq("--test")
    )
    expect(res.exitCode == 0)
    expect(res.out.text().toLowerCase.contains("jshell"))
  }

  test("JShell rejects --ammonite combination") {
    TestInputs.empty.fromRoot { root =>
      val res = os
        .proc(
          TestUtil.cli,
          "--power",
          "repl",
          ".",
          "--jshell",
          "--ammonite",
          "--repl-dry-run",
          extraOptions
        )
        .call(cwd = root, mergeErrIntoOut = true, check = false)
      expect(res.exitCode != 0)
      expect(res.out.text().contains("--jshell cannot be used together with --ammonite"))
    }
  }

  // --Xshow-phases / ScalaPy / Ammonite-specific scenarios do not apply to JShell (Java REPL).
  if jshellAvailable then {
    test(s"$runInJShellPrefix simple") {
      val expectedMessage = "1337"
      runInJShell(s"""System.out.println("$expectedMessage");""")(res =>
        expect(jshellOutput(res).contains(expectedMessage))
      )
    }

    for javaVersion <- Constants.allJavaVersions.filter(_ >= 11) do
      test(s"$runInJShellPrefix simple on JDK $javaVersion") {
        val versionSpecific = javaVersion match {
          case v if v >= 23 =>
            """System.out.println(javax.print.attribute.standard.OutputBin.LEFT);"""
          case v if v >= 21 =>
            """System.out.println(Thread.ofVirtual().unstarted(() -> {}).getClass().getName());"""
          case v if v >= 17 =>
            """System.out.println(java.util.HexFormat.of().toHexDigits(255));"""
          case v if v >= 16 =>
            """System.out.println(java.util.stream.Stream.of(1, 2, 3).toList());"""
          case _ =>
            """System.out.println(java.util.Optional.of(1).isEmpty());"""
        }
        runInJShellOnJvm(javaVersion, extraInitScript = versionSpecific)(_ => ())
      }

    if !Properties.isWin then
      test(s"$runInJShellPrefix with extra JAR") {
        runInJShell(
          initScript =
            """System.out.println(Class.forName("org.slf4j.Logger").getName());"""
        )(
          runBeforeReplAndGetExtraCliOpts = () => {
            val jar =
              os.proc(TestUtil.cs, "fetch", "--intransitive", "org.slf4j:slf4j-api:2.0.13")
                .call()
                .out
                .text()
                .trim
            Seq("--jar", jar)
          },
          runAfterRepl = res =>
            expect(jshellOutput(res).contains("org.slf4j.Logger"))
        )
      }

    test(s"$runInJShellPrefix pure Java project") {
      runInJShell(
        initScript = """System.out.println(demo.Demo.greet());""",
        testInputs = TestInputs(
          os.rel / "Demo.java" ->
            """package demo;
              |public class Demo {
              |  public static String greet() { return "hi-java"; }
              |}
              |""".stripMargin
        )
      )(res => expect(jshellOutput(res).contains("hi-java")))
    }

    test(s"$runInJShellPrefix mixed Java/Scala project, JShell sees both") {
      runInJShell(
        initScript =
          """System.out.println(demo.JavaPart.hello());
            |var c = Class.forName("demo.ScalaPart$");
            |var inst = c.getField("MODULE$").get(null);
            |System.out.println(c.getMethod("hello").invoke(inst));
            |""".stripMargin,
        replCliOptions = TestUtil.extraOptions,
        testInputs = TestInputs(
          os.rel / "demo" / "JavaPart.java" ->
            """package demo;
              |public class JavaPart {
              |  public static String hello() { return "hi-java"; }
              |}
              |""".stripMargin,
          os.rel / "demo" / "ScalaPart.scala" ->
            """package demo
              |
              |object ScalaPart {
              |  def hello: String = "hi-scala"
              |}
              |""".stripMargin
        )
      ) { res =>
        val out = jshellOutput(res)
        expect(out.contains("hi-java"))
        expect(out.contains("hi-scala"))
      }
    }

    test(s"$runInJShellPrefix Scala project exposed to JShell via reflection") {
      runInJShell(
        initScript =
          """var c = Class.forName("demo.Smth$");
            |var inst = c.getField("MODULE$").get(null);
            |System.out.println(c.getMethod("smth").invoke(inst));
            |""".stripMargin,
        replCliOptions = TestUtil.extraOptions,
        testInputs = TestInputs(
          os.rel / "Smth.scala" ->
            """package demo
              |
              |object Smth {
              |  def smth: String = "haha"
              |}
              |""".stripMargin
        )
      )(res => expect(jshellOutput(res).contains("haha")))
    }
  }
}
