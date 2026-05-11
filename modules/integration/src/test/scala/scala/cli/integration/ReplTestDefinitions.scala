package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors
import scala.util.{Properties, Try}

abstract class ReplTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  this: TestScalaVersion =>
  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

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

  protected lazy val canRunInRepl: Boolean =
    (actualScalaVersion.startsWith("3.3") &&
      actualScalaVersion.coursierVersion >= "3.3.7".coursierVersion) ||
    actualScalaVersion.startsWith("3.7") ||
    actualScalaVersion.coursierVersion >= "3.7.0-RC1".coursierVersion

  protected val dryRunPrefix: String    = "Dry run:"
  protected val runInReplPrefix: String = "Running in Scala REPL:"

  def runInRepl(
    codeToRunInRepl: String,
    testInputs: TestInputs = TestInputs.empty,
    cliOptions: Seq[String] = Seq.empty,
    shouldPipeStdErr: Boolean = false,
    check: Boolean = true,
    skipScalaVersionArgs: Boolean = false,
    env: Map[String, String] = Map.empty,
    initScriptFromFile: Boolean = false
  )(
    runAfterRepl: os.CommandResult => Unit,
    runBeforeReplAndGetExtraCliOpts: () => Seq[os.Shellable] = () => Seq.empty
  ): Unit = {
    testInputs.fromRoot { root =>
      val potentiallyExtraCliOpts = runBeforeReplAndGetExtraCliOpts()
      val initScriptArgs          =
        if initScriptFromFile then {
          val initScriptFile = root / ".scala-cli-repl-init.sc"
          os.write.over(initScriptFile, codeToRunInRepl)
          Seq("--repl-init-script-file", initScriptFile.toString)
        }
        else Seq("--repl-init-script", codeToRunInRepl)
      runAfterRepl(
        os.proc(
          TestUtil.cli,
          "repl",
          ".",
          "--repl-quit-after-init",
          initScriptArgs,
          if skipScalaVersionArgs then TestUtil.extraOptions else extraOptions,
          cliOptions,
          potentiallyExtraCliOpts
        )
          .call(
            cwd = root,
            stderr = if shouldPipeStdErr then os.Pipe else os.Inherit,
            env = env,
            check = check
          )
      )
    }
  }

  def dryRun(
    testInputs: TestInputs = TestInputs.empty,
    cliOptions: Seq[String] = Seq.empty,
    useExtraOptions: Boolean = true
  ): Unit = {
    testInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "repl",
        "--repl-dry-run",
        cliOptions,
        if useExtraOptions then extraOptions else Seq.empty
      )
        .call(cwd = root)
    }
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

  private def jshellOutput(res: os.CommandResult): String =
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

  test(s"$dryRunPrefix default")(dryRun())

  test(s"$dryRunPrefix with main scope sources") {
    dryRun(
      TestInputs(
        os.rel / "Example.scala" ->
          """object Example extends App {
            |  println("Hello")
            |}
            |""".stripMargin
      )
    )
  }

  test(s"$dryRunPrefix with main and test scope sources, and the --test flag") {
    dryRun(
      TestInputs(
        os.rel / "Example.scala" ->
          """object Example extends App {
            |  println("Hello")
            |}
            |""".stripMargin,
        os.rel / "Example.test.scala" ->
          s"""//> using dep org.scalameta::munit::${Constants.munitVersion}
             |
             |class Example extends munit.FunSuite {
             |  test("is true true") { assert(true) }
             |}
             |""".stripMargin
      )
    )
  }

  test(s"$dryRunPrefix calling repl with a directory with no scala artifacts") {
    dryRun(TestInputs(os.rel / "Testing.java" -> "public class Testing {}"))
  }

  test("default scala version in help") {
    TestInputs.empty.fromRoot { root =>
      val res              = os.proc(TestUtil.cli, "repl", extraOptions, "--help").call(cwd = root)
      val lines            = removeAnsiColors(res.out.trim()).linesIterator.toVector
      val scalaVersionHelp = lines.find(_.contains("--scala-version")).getOrElse("")
      expect(scalaVersionHelp.contains(s"(${Constants.defaultScala} by default)"))
    }
  }

  test("calling repl with -Xshow-phases flag") {
    val cmd = Seq[os.Shellable](
      TestUtil.cli,
      "repl",
      "-Xshow-phases",
      extraOptions
    )

    val res = os.proc(cmd).call(mergeErrIntoOut = true)
    expect(res.exitCode == 0)
    val output = res.out.text()
    expect(output.contains("parser"))
    expect(output.contains("typer"))
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

  if canRunInRepl then {
    test(s"$runInReplPrefix simple") {
      val expectedMessage = "1337"
      runInRepl(s"""println($expectedMessage)""")(r =>
        expect(r.out.trim() == expectedMessage)
      )
    }

    test(s"$runInReplPrefix init script file with quotes and newlines") {
      runInRepl(
        codeToRunInRepl =
          """val message = "hello from file"
            |println(message)
            |""".stripMargin,
        initScriptFromFile = true
      )(r => expect(r.out.trim().linesIterator.exists(_.trim == "hello from file")))
    }

    test(s"$runInReplPrefix verify Scala version from the REPL") {
      val opts = if actualScalaVersion.startsWith("3") && !isScala38OrNewer then
        Seq("--with-compiler")
      else Seq.empty
      runInRepl(
        codeToRunInRepl = s"""println($retrieveScalaVersionCode)""",
        cliOptions = opts
      )(r => expect(r.out.trim() == actualScalaVersion))
    }

    test(s"$runInReplPrefix test scope") {
      val message = "something something something REPL"
      runInRepl(
        codeToRunInRepl = "println(example.TestScopeExample.message)",
        testInputs = TestInputs(
          os.rel / "example" / "TestScopeExample.test.scala" ->
            s"""package example
               |
               |object TestScopeExample {
               |  def message: String = "$message"
               |}
               |""".stripMargin
        ),
        cliOptions = Seq("--test")
      )(r => expect(r.out.trim() == message))
    }

    test(s"$runInReplPrefix https://github.com/scala/scala3/issues/21229") {
      runInRepl(
        codeToRunInRepl = "println(stuff.baz)",
        testInputs = TestInputs(
          os.rel / "Pprint.scala" ->
            """//> using dep com.lihaoyi::pprint::0.9.0
              |package stuff
              |import scala.quoted.*
              |def foo = pprint(1)
              |inline def bar = pprint(1)
              |inline def baz = ${ bazImpl }
              |def bazImpl(using Quotes) = '{ pprint(1) }
              |""".stripMargin
        )
      )(res => expect(res.out.trim().nonEmpty))
    }

    if !Properties.isWin then {
      test(s"$runInReplPrefix ScalaPy") {
        val opts =
          if actualScalaVersion.startsWith("3") && !isScala38OrNewer then Seq("--with-compiler")
          else Seq.empty
        runInRepl(
          codeToRunInRepl =
            s"""import me.shadaj.scalapy.py
               |println("Hello" + " from Scala " + $retrieveScalaVersionCode)
               |val sth = py.module("foo.something")
               |py.Dynamic.global.applyDynamicNamed("print")("" -> sth.messageStart, "" -> sth.messageEnd, "flush" -> py.Any.from(true))
               |""".stripMargin,
          testInputs = TestInputs(
            os.rel / "foo" / "something.py" ->
              """messageStart = 'Hello from'
                |messageEnd = 'ScalaPy'
                |""".stripMargin
          ),
          cliOptions = Seq("--python", "--power") ++ opts,
          shouldPipeStdErr = true
        ) { res =>
          val output = res.out.trim().linesIterator.toVector.take(2).mkString("\n")
          expect(output ==
            s"""Hello from Scala $actualScalaVersion
               |Hello from ScalaPy""".stripMargin)
        }
      }

      test(s"$runInReplPrefix ScalaPy with PYTHONSAFEPATH") {
        val opts =
          if actualScalaVersion.startsWith("3") && !isScala38OrNewer then Seq("--with-compiler")
          else Seq.empty
        runInRepl(
          codeToRunInRepl =
            s"""import me.shadaj.scalapy.py
               |println("Hello" + " from Scala " + $retrieveScalaVersionCode)
               |val sth = py.module("foo.something")
               |py.Dynamic.global.applyDynamicNamed("print")("" -> sth.messageStart, "" -> sth.messageEnd, "flush" -> py.Any.from(true))
               |""".stripMargin,
          testInputs = TestInputs(
            os.rel / "foo" / "something.py" ->
              """messageStart = 'Hello from'
                |messageEnd = 'ScalaPy'
                |""".stripMargin
          ),
          cliOptions = Seq("--python", "--power") ++ opts,
          shouldPipeStdErr = true,
          // check = false, // technically should be an error, but the REPL itself doesn't return it as such.
          env = Map("PYTHONSAFEPATH" -> "foo")
        ) { errorRes =>
          // expect(errorRes.exitCode != 0) // technically should be an error, but the REPL itself doesn't return it as such.
          val errorOutput = TestUtil.removeAnsiColors(errorRes.err.trim() + errorRes.out.trim())
          expect(errorOutput.contains("No module named 'foo'"))
        }
      }

      test(s"$runInReplPrefix with extra JAR") {
        runInRepl(codeToRunInRepl =
          """import shapeless._; println("Here's an HList: " + (2 :: true :: "a" :: HNil))"""
        )(
          runBeforeReplAndGetExtraCliOpts = () =>
            val shapelessJar =
              os.proc(TestUtil.cs, "fetch", "--intransitive", "com.chuusai:shapeless_2.13:2.3.7")
                .call()
                .out
                .text()
                .trim
            Seq("--jar", shapelessJar)
          ,
          runAfterRepl = res => expect(res.out.trim() == "Here's an HList: 2 :: true :: a :: HNil")
        )
      }

      if !isScala38OrNewer then
        // TODO rewrite this test to work with Scala 3.8+ once 3.8.0 stable is out
        test(s"$runInReplPrefix as jar") {
          val inputs = TestInputs(
            os.rel / "CheckCp.scala" ->
              """//> using dep com.lihaoyi::os-lib:0.9.1
                |package checkcp
                |class CheckCp
                |object CheckCp {
                |  def hasDir: Boolean = {
                |    val uri: java.net.URI = classOf[checkcp.CheckCp].getProtectionDomain.getCodeSource.getLocation.toURI
                |    os.isDir(os.Path(java.nio.file.Paths.get(uri)))
                |  }
                |}
                |""".stripMargin
          )
          val code = """println("hasDir=" + checkcp.CheckCp.hasDir)"""
          runInRepl(codeToRunInRepl = code, testInputs = inputs) {
            res => expect(res.out.trim().contains("hasDir=true"))
          }
          runInRepl(
            codeToRunInRepl = code,
            testInputs = inputs,
            cliOptions = Seq("--as-jar", "--power")
          ) {
            res => expect(res.out.trim().contains("hasDir=false"))
          }

        }
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

    if !Properties.isWin then
      test(s"$runInJShellPrefix direct --repl-init-script string form") {
        val initScript =
          """System.out.println("hi from string");
            |var c = Class.forName("java.lang.String");
            |System.out.println(c.getName());
            |""".stripMargin
        TestInputs.empty.fromRoot { root =>
          val res = os.proc(
            TestUtil.cli,
            "--power",
            "repl",
            ".",
            "--jshell",
            "--repl-quit-after-init",
            "--repl-init-script",
            initScript,
            extraOptions
          ).call(cwd = root, mergeErrIntoOut = true)
          val out = jshellOutput(res)
          expect(out.contains("hi from string"))
          expect(out.contains("java.lang.String"))
        }
      }

    test(s"$runInJShellPrefix verify JVM version respects --jvm") {
      runInJShell(
        initScript = """System.out.println(System.getProperty("java.specification.version"));""",
        cliOptions = Seq("--jvm", "17")
      )(res =>
        expect(
          jshellOutput(res).trim().linesIterator.exists { line =>
            val t = line.trim
            t == "17" || t.startsWith("17.")
          }
        )
      )
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
