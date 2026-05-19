package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

trait ReplJShellTestDefinitions { this: ReplTestDefinitions =>
  protected val jshellDryRunPrefix: String = "JShell dry run:"
  protected val runInJShellPrefix: String  = "Running in JShell:"

  protected def dryRunJshell(
    testInputs: TestInputs = TestInputs.empty,
    cliOptions: Seq[String] = Seq.empty,
    useExtraOptions: Boolean = true,
    check: Boolean = true
  ): os.CommandResult =
    testInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
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

  protected def runInJShell(
    initScript: String,
    testInputs: TestInputs = TestInputs.empty,
    cliOptions: Seq[String] = Seq.empty,
    replCliOptions: Seq[String] = extraOptions,
    check: Boolean = true,
    env: Map[String, String] = Map.empty
  )(
    runAfterRepl: (os.CommandResult, os.Path) => Unit,
    runBeforeReplAndGetExtraCliOpts: () => Seq[os.Shellable] = () => Seq.empty
  ): Unit = {
    testInputs.fromRoot { root =>
      val potentiallyExtraCliOpts = runBeforeReplAndGetExtraCliOpts()
      val initScriptFile          = root / ".scala-cli-jshell-init.jsh"
      os.write.over(initScriptFile, initScript)
      runAfterRepl(
        os.proc(
          TestUtil.cli,
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
        ),
        root
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
    runInJShell(initScript = initScript, cliOptions = Seq("--jvm", javaVersion.toString)) {
      (res, _) =>
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
        os.rel / "MainTest.test.java" -> "public class MainTest {}"
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

  test(s"$runInJShellPrefix simple") {
    val expectedMessage = "1337"
    runInJShell(s"""System.out.println("$expectedMessage");""") { (res, _) =>
      expect(jshellOutput(res).contains(expectedMessage))
    }
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
        runAfterRepl = (res, _) =>
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
    )((res, _) => expect(jshellOutput(res).contains("hi-java")))
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
    ) { (res, _) =>
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
    )((res, _) => expect(jshellOutput(res).contains("haha")))
  }

  if !Properties.isWin && canRunInRepl then
    test(s"$runInReplPrefix Running in default Scala REPL: pure Java sources with --jshell=false") {
      val inputs = TestInputs(
        os.rel / "demo" / "Demo.java" ->
          """package demo;
            |public class Demo {
            |  public static String greet() { return "hi-default"; }
            |}
            |""".stripMargin
      )
      // Dry-run: JShell prints "JShell command: …"; the Scala REPL backend does not.
      inputs.fromRoot { root =>
        val dryOut = os
          .proc(TestUtil.cli, "repl", ".", "--jshell=false", "--repl-dry-run", extraOptions)
          .call(cwd = root, mergeErrIntoOut = true)
          .out
          .text()
        expect(!dryOut.contains("JShell command:"))
        expect(dryOut.contains("using the default Scala REPL instead of JShell"))
      }
      // Run with a Scala-only init script (string interpolation would be rejected by JShell).
      runInRepl(
        codeToRunInRepl =
          """import demo.Demo
            |val greeting = Demo.greet()
            |println(s"scala-says:$greeting")
            |""".stripMargin,
        testInputs = inputs,
        cliOptions = Seq("--jshell=false"),
        shouldPipeStdErr = true
      ) { res =>
        val combined = res.out.text() + res.err.text()
        expect(combined.contains("scala-says:hi-default"))
      }
    }

  for {
    (kind, inputs: TestInputs) <- Seq(
      (
        "pure Java",
        TestInputs(
          os.rel / "Demo.java" ->
            """public class Demo {
              |  public static String hi() { return "hi-java"; }
              |}
              |""".stripMargin
        )
      ),
      (
        "pure Scala",
        TestInputs(
          os.rel / "Greet.scala" ->
            """object Greet {
              |  def hi: String = "hi-scala"
              |}
              |""".stripMargin
        )
      ),
      (
        "mixed Java/Scala",
        TestInputs(
          os.rel / "Demo.java" ->
            """public class Demo {
              |  public static String hi() { return "hi-java"; }
              |}
              |""".stripMargin,
          os.rel / "Greet.scala" ->
            """object Greet {
              |  def hi: String = "hi-scala"
              |}
              |""".stripMargin
        )
      )
    )
    osPwdInitScript = """var __pwd = os.package$.MODULE$.pwd();
                        |System.out.println("PWD-MARKER:" + __pwd.toString());
                        |""".stripMargin
    directiveExtension  = if kind == "pure Java" then ".java" else ".scala"
    inputsWithDirective =
      inputs.add((os.rel / s"build$directiveExtension", "//> using dep com.lihaoyi::os-lib:0.9.1"))
    if actualScalaVersion.startsWith("3")
  } {
    test(s"$runInJShellPrefix $kind with dependency via directive") {
      runInJShell(
        initScript = osPwdInitScript,
        testInputs = inputsWithDirective
      ) { (res, root) =>
        expect(jshellOutput(res).contains(s"PWD-MARKER:${root.toString}"))
      }
    }

    test(s"$runInJShellPrefix $kind with Scala Toolkit") {
      runInJShell(
        initScript = osPwdInitScript,
        testInputs = inputs,
        cliOptions = Seq("--toolkit", "default")
      ) { (res, root) =>
        expect(jshellOutput(res).contains(s"PWD-MARKER:${root.toString}"))
      }
    }
  }
}
