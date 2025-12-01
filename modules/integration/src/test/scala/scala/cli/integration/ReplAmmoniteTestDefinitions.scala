package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.{normalizeArgsForWindows, removeAnsiColors}

trait ReplAmmoniteTestDefinitions { this: ReplTestDefinitions =>
  protected val ammonitePrefix: String        = "Running in Ammonite REPL:"
  def expectedScalaVersionForAmmonite: String =
    actualScalaVersion match {
      case s
          if s.startsWith("2.12") &&
          Constants.maxAmmoniteScala212Version.coursierVersion < s.coursierVersion =>
        Constants.maxAmmoniteScala212Version
      case s
          if s.startsWith("2.13") &&
          Constants.maxAmmoniteScala213Version.coursierVersion < s.coursierVersion =>
        Constants.maxAmmoniteScala213Version
      case s
          if s.startsWith(Constants.scala3LtsPrefix) &&
          Constants.maxAmmoniteScala3LtsVersion.coursierVersion < s.coursierVersion =>
        Constants.maxAmmoniteScala3LtsVersion
      case s
          if s.startsWith("3") &&
          Constants.maxAmmoniteScala3Version.coursierVersion < s.coursierVersion =>
        Constants.maxAmmoniteScala3Version
      case s => s
    }

  def actualMaxAmmoniteScalaVersion: String =
    if actualScalaVersion.startsWith(Constants.scala3LtsPrefix) then
      Constants.maxAmmoniteScala3LtsVersion
    else if actualScalaVersion.startsWith(Constants.scala3NextPrefix) then
      Constants.maxAmmoniteScala3Version
    else if actualScalaVersion.startsWith("2.13") then Constants.maxAmmoniteScala213Version
    else Constants.maxAmmoniteScala212Version

  def shouldUseMaxAmmoniteScalaVersion: Boolean =
    actualScalaVersion.coursierVersion > actualMaxAmmoniteScalaVersion.coursierVersion

  def ammoniteExtraOptions: Seq[String] =
    if !shouldUseMaxAmmoniteScalaVersion then extraOptions
    else Seq("--scala", actualMaxAmmoniteScalaVersion) ++ TestUtil.extraOptions

  def runInAmmoniteRepl(
    codeToRunInRepl: String = "",
    testInputs: TestInputs = TestInputs.empty,
    cliOptions: Seq[String] = Seq.empty,
    extraAmmoniteOptions: Seq[String] = Seq.empty,
    shouldPipeStdErr: Boolean = false,
    check: Boolean = true,
    env: Map[String, String] = Map.empty
  )(
    runAfterRepl: os.CommandResult => Unit,
    runBeforeReplAndGetExtraCliOpts: () => Seq[os.Shellable] = () => Seq.empty
  ): Unit = {
    val ammArgs =
      if codeToRunInRepl.nonEmpty then
        (Seq("-c", codeToRunInRepl) ++ extraAmmoniteOptions)
          .normalizeArgsForWindows
          .flatMap(arg => Seq("--ammonite-arg", arg))
      else Seq.empty
    testInputs.fromRoot { root =>
      val potentiallyExtraCliOpts = runBeforeReplAndGetExtraCliOpts()
      runAfterRepl(
        os.proc(
          TestUtil.cli,
          "--power",
          "repl",
          ".",
          "--ammonite",
          ammArgs,
          ammoniteExtraOptions,
          cliOptions,
          potentiallyExtraCliOpts
        ).call(
          cwd = root,
          env = env,
          check = check,
          stderr = if shouldPipeStdErr then os.Pipe else os.Inherit
        )
      )
    }
  }

  def ammoniteTest(): Unit = {
    TestInputs.empty.fromRoot { root =>
      runInAmmoniteRepl(
        codeToRunInRepl = s"""println("Hello" + " from Scala " + $retrieveScalaVersionCode)""",
        shouldPipeStdErr = true
      ) { res =>
        val output     = res.out.trim()
        val expectedSv =
          if shouldUseMaxAmmoniteScalaVersion then actualMaxAmmoniteScalaVersion
          else expectedScalaVersionForAmmonite
        expect(output == s"Hello from Scala $expectedSv")
        if shouldUseMaxAmmoniteScalaVersion then {
          // the maximum Scala version supported by ammonite is being used, so we shouldn't downgrade
          val errOutput = res.err.trim()
          expect(!errOutput.contains("not yet supported with this version of Ammonite"))
        }
      }
    }
  }

  def ammoniteTestScope(): Unit = {
    val message = "something something ammonite"
    runInAmmoniteRepl(
      codeToRunInRepl = "println(example.TestScopeExample.message)",
      testInputs =
        TestInputs(
          os.rel / "example" / "TestScopeExample.test.scala" ->
            s"""package example
               |
               |object TestScopeExample {
               |  def message: String = "$message"
               |}
               |""".stripMargin
        ),
      cliOptions = Seq("--test"),
      shouldPipeStdErr = true
    ) { res =>
      val output = res.out.trim()
      expect(output == message)
      if shouldUseMaxAmmoniteScalaVersion then {
        // the maximum Scala version supported by ammonite is being used, so we shouldn't downgrade
        val errOutput = res.err.trim()
        expect(!errOutput.contains("not yet supported with this version of Ammonite"))
      }
    }
  }

  def ammoniteScalapyTest(): Unit = {
    val codeToRunInRepl =
      s"""println("Hello" + " from Scala " + $retrieveScalaVersionCode)
         |val sth = py.module("foo.something")
         |py.Dynamic.global.applyDynamicNamed("print")("" -> sth.messageStart, "" -> sth.messageEnd, "flush" -> py.Any.from(true))
         |""".stripMargin
    val inputs =
      TestInputs(
        os.rel / "foo" / "something.py" ->
          """messageStart = 'Hello from'
            |messageEnd = 'ScalaPy'
            |""".stripMargin
      )
    runInAmmoniteRepl(
      codeToRunInRepl = codeToRunInRepl,
      testInputs = inputs,
      cliOptions = Seq("--python"),
      shouldPipeStdErr = true,
      check = false,
      env = Map("PYTHONSAFEPATH" -> "foo")
    ) { errorRes =>
      expect(errorRes.exitCode != 0)
      val errorOutput = errorRes.err.trim() + errorRes.out.trim()
      expect(errorOutput.contains("No module named 'foo'"))
    }
    runInAmmoniteRepl(
      codeToRunInRepl = codeToRunInRepl,
      testInputs = inputs,
      cliOptions = Seq("--python"),
      shouldPipeStdErr = true
    ) { res =>
      val expectedSv =
        if shouldUseMaxAmmoniteScalaVersion then actualMaxAmmoniteScalaVersion
        else expectedScalaVersionForAmmonite
      val lines = res.out.trim().linesIterator.toVector
      expect(lines == Seq(s"Hello from Scala $expectedSv", "Hello from ScalaPy"))
      if shouldUseMaxAmmoniteScalaVersion then
        // the maximum Scala version supported by ammonite is being used, so we shouldn't downgrade
        expect(!res.err.trim().contains("not yet supported with this version of Ammonite"))
    }
  }

  def ammoniteMaxVersionString: String =
    if actualScalaVersion <= actualMaxAmmoniteScalaVersion then s" with Scala $actualScalaVersion"
    else s" with Scala $actualMaxAmmoniteScalaVersion (the latest supported version)"

  test(s"$ammonitePrefix simple $ammoniteMaxVersionString")(ammoniteTest())
  test(s"$ammonitePrefix scalapy$ammoniteMaxVersionString")(ammoniteScalapyTest())
  test(s"$ammonitePrefix with test scope sources$ammoniteMaxVersionString")(ammoniteTestScope())

  test(s"$ammonitePrefix ammonite version in help$ammoniteMaxVersionString") {
    runInAmmoniteRepl(cliOptions = Seq("--help")) { res =>
      val lines          = removeAnsiColors(res.out.trim()).linesIterator.toVector
      val ammVersionHelp = lines.find(_.contains("--ammonite-ver")).getOrElse("")
      expect(ammVersionHelp.contains(s"(${Constants.ammoniteVersion} by default)"))
    }
  }

  def ammoniteWithExtraJarTest(): Unit = {
    runInAmmoniteRepl(codeToRunInRepl =
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

  if actualScalaVersion.startsWith("2.13") then
    test(s"$ammonitePrefix with extra JAR$ammoniteMaxVersionString") {
      ammoniteWithExtraJarTest()
    }
}
