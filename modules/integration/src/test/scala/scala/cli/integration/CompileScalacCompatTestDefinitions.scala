package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

/** For the `run` counterpart, refer to [[RunScalacCompatTestDefinitions]] */
trait CompileScalacCompatTestDefinitions { _: CompileTestDefinitions =>
  if (actualScalaVersion.startsWith("3"))
    test("consecutive -language:* flags are not ignored") {
      val sourceFileName = "example.scala"
      TestInputs(os.rel / sourceFileName ->
        s"""//> using scala $actualScalaVersion
           |//> using options -color:never -language:noAutoTupling -language:strictEquality
           |case class Cat(name: String)
           |case class Dog(name: String)
           |def strictEquality(c: Cat, d: Dog):Boolean = c == d
           |def takesTuple(tpl: Tuple) = ???
           |def withTuple() = takesTuple(1, 2)
           |""".stripMargin).fromRoot { root =>
        val res = os.proc(TestUtil.cli, "compile", sourceFileName)
          .call(cwd = root, check = false, stderr = os.Pipe)
        expect(res.exitCode == 1)
        val errOutput = res.err.trim()
        val expectedStrictEqualityError =
          " Values of types Cat and Dog cannot be compared with == or !="
        expect(errOutput.contains(expectedStrictEqualityError))
        val expectedNoAutoTuplingError =
          "too many arguments for method takesTuple: (tpl: Tuple): Nothing"
        expect(errOutput.trim().contains(expectedNoAutoTuplingError))
      }
    }

  // Given the vast number of ways compiler options can be passed from the CLI,
  // we test them all (or most, at the very least), as a (perhaps overkill) sanity check.
  // Pieces of the existing `-language:*` test are reused, but kept separate for clarity.
  {
    val modes @ Seq(viaDirective, viaCli, viaCliWithExplicitOpt, mixed, mixedWithExplicitOpt) =
      Seq("directive", "cli", "cli with -O", "mixed", "mixed with -O")
    for {
      mode <- modes
      if actualScalaVersion == Constants.scala3Next
      dashPrefix <- Seq("-", "--")
      syntaxVariant <- Seq(
        Seq(
          Seq(s"${dashPrefix}color:never"),
          Seq(s"${dashPrefix}language:noAutoTupling"),
          Seq(s"${dashPrefix}language:strictEquality")
        ),
        Seq(
          Seq(s"${dashPrefix}color", "never"),
          Seq(s"${dashPrefix}language", "noAutoTupling"),
          Seq(s"${dashPrefix}language", "strictEquality")
        ),
        Seq(
          Seq(s"${dashPrefix}color:never"),
          Seq(s"\"${dashPrefix}language:noAutoTupling,strictEquality\"")
        ),
        Seq(
          Seq(s"${dashPrefix}color", "never"),
          Seq(s"${dashPrefix}language", "\"noAutoTupling,strictEquality\"")
        )
      )
      (cliOpts, directiveOpts) = {
        val (initialCliOpts, initialDirectiveOpts) = mode match {
          case m if m == mixed => syntaxVariant.splitAt(syntaxVariant.length - 1)
          case m if m == mixedWithExplicitOpt =>
            val (initialCliOpts, initialDirectiveOpts) =
              syntaxVariant.splitAt(syntaxVariant.length - 1)
            initialCliOpts.map(_.flatMap(o => Seq("-O", o))) -> initialDirectiveOpts
          case c if c == viaCli => syntaxVariant -> Nil
          case c if c == viaCliWithExplicitOpt =>
            syntaxVariant.map(_.flatMap(o => Seq("-O", o))) -> Nil
          case _ => Nil -> syntaxVariant
        }
        initialCliOpts.flatten.map(_.filter(_ != '"')) -> initialDirectiveOpts.flatten
      }
      cliOptsString       = cliOpts.mkString(" ")
      directiveOptsString = directiveOpts.mkString(" ")
      includeDirective =
        (mode == viaDirective || mode == mixed || mode == mixedWithExplicitOpt) && directiveOpts.nonEmpty
      directiveString = if (includeDirective) s"//> using options $directiveOptsString" else ""
      allOptsString = mode match {
        case m if m.startsWith(mixed) =>
          s"opts passed via command line: $cliOptsString, opts passed via directive: $directiveString"
        case c if c.startsWith(viaCli) =>
          s"opts passed via command line: $cliOptsString"
        case _ =>
          s"opts passed via directive: $directiveString"
      }
    } test(s"compiler options passed in $mode mode: $allOptsString") {
      val sourceFileName = "example.scala"
      TestInputs(os.rel / sourceFileName ->
        s"""//> using scala $actualScalaVersion
           |$directiveString
           |case class Cat(name: String)
           |case class Dog(name: String)
           |def strictEquality(c: Cat, d: Dog):Boolean = c == d
           |def takesTuple(tpl: Tuple) = ???
           |def withTuple() = takesTuple(1, 2)
           |""".stripMargin).fromRoot { root =>
        val res = os.proc(TestUtil.cli, "compile", sourceFileName, cliOpts)
          .call(cwd = root, check = false, stderr = os.Pipe)
        println(res.err.trim())
        expect(res.exitCode == 1)
        val errOutput = res.err.trim()
        val expectedStrictEqualityError =
          "Values of types Cat and Dog cannot be compared with == or !="
        expect(errOutput.contains(expectedStrictEqualityError))
        val expectedNoAutoTuplingError =
          "too many arguments for method takesTuple: (tpl: Tuple): Nothing"
        expect(errOutput.trim().contains(expectedNoAutoTuplingError))
      }
    }
  }

  for {
    useDirective <- Seq(true, false)
    if !Properties.isWin
    optionsSource = if (useDirective) "using directive" else "command line"
  } test(s"consecutive -Wconf:* flags are not ignored (passed via $optionsSource)") {
    val sv                 = actualScalaVersion
    val sourceFileName     = "example.scala"
    val warningConfOptions = Seq("-Wconf:cat=deprecation:e", "-Wconf:any:s")
    val maybeDirectiveString =
      if (useDirective) s"//> using options ${warningConfOptions.mkString(" ")}" else ""
    TestInputs(os.rel / sourceFileName ->
      s"""//> using scala $sv
         |$maybeDirectiveString
         |object WConfExample extends App {
         |  @deprecated("This method will be removed", "1.0.0")
         |  def oldMethod(): Unit = println("This is an old method.")
         |  oldMethod()
         |}
         |""".stripMargin).fromRoot { root =>
      val localBin = root / "local-bin"
      os.proc(
        TestUtil.cs,
        "install",
        "--install-dir",
        localBin,
        s"scalac:$sv"
      ).call(cwd = root)
      val cliRes =
        os.proc(
          TestUtil.cli,
          "compile",
          sourceFileName,
          "--server=false",
          if (useDirective) Nil else warningConfOptions
        )
          .call(cwd = root, check = false, stderr = os.Pipe)
      val scalacRes = os.proc(localBin / "scalac", warningConfOptions, sourceFileName)
        .call(cwd = root, check = false, stderr = os.Pipe)
      expect(scalacRes.exitCode == cliRes.exitCode)
      val scalacResErr = scalacRes.err.trim()
      if (sv != Constants.scala3Lts) {
        // TODO run this check for LTS when -Wconf gets fixed there
        val cliResErr =
          cliRes.err.trim().linesIterator.toList
            // skip potentially irrelevant logs
            .dropWhile(_.contains("Check"))
            .mkString(System.lineSeparator())
        expect(cliResErr == scalacResErr)
      }
      else expect(
        TestUtil.removeAnsiColors(cliRes.err.trim())
          .contains(
            "method oldMethod in object WConfExample is deprecated since 1.0.0: This method will be removed"
          )
      )
    }
  }
}
