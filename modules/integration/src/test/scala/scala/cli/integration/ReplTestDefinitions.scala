package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors
import scala.util.Properties

abstract class ReplTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  this: TestScalaVersion =>
  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

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
    env: Map[String, String] = Map.empty
  )(
    runAfterRepl: os.CommandResult => Unit,
    runBeforeReplAndGetExtraCliOpts: () => Seq[os.Shellable] = () => Seq.empty
  ): Unit = {
    testInputs.fromRoot { root =>
      val potentiallyExtraCliOpts = runBeforeReplAndGetExtraCliOpts()
      runAfterRepl(
        os.proc(
          TestUtil.cli,
          "repl",
          ".",
          "--repl-quit-after-init",
          "--repl-init-script",
          codeToRunInRepl,
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

  if canRunInRepl then {
    test(s"$runInReplPrefix simple") {
      val expectedMessage = "1337"
      runInRepl(s"""println($expectedMessage)""")(r =>
        expect(r.out.trim() == expectedMessage)
      )
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
}
