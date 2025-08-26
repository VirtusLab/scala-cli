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
        val errOutput                   = res.err.trim()
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
      dashPrefix    <- Seq("-", "--")
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
          Seq(s"${dashPrefix}language:noAutoTupling,strictEquality")
        ),
        Seq(
          Seq(s"${dashPrefix}color", "never"),
          Seq(s"${dashPrefix}language", "noAutoTupling,strictEquality")
        )
      )
      (cliOpts, directiveOpts) = {
        val (initialCliOpts, initialDirectiveOpts) = mode match {
          case m if m == mixed                => syntaxVariant.splitAt(syntaxVariant.length - 1)
          case m if m == mixedWithExplicitOpt =>
            val (initialCliOpts, initialDirectiveOpts) =
              syntaxVariant.splitAt(syntaxVariant.length - 1)
            initialCliOpts.map(_.flatMap(o => Seq("-O", o))) -> initialDirectiveOpts
          case c if c == viaCli                => syntaxVariant -> Nil
          case c if c == viaCliWithExplicitOpt =>
            syntaxVariant.map(_.flatMap(o => Seq("-O", o))) -> Nil
          case _ => Nil -> syntaxVariant
        }
        initialCliOpts.flatten.map(_.filter(_ != '"')) -> initialDirectiveOpts.flatten
      }
      cliOptsString       = cliOpts.mkString(" ")
      directiveOptsString = directiveOpts.mkString(" ")
      includeDirective    =
        (mode == viaDirective || mode == mixed || mode == mixedWithExplicitOpt) && directiveOpts.nonEmpty
      directiveString = if (includeDirective) s"//> using options $directiveOptsString" else ""
      allOptsString   = mode match {
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
        val errOutput                   = res.err.trim()
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
    sv            = actualScalaVersion
  } {
    test(s"consecutive -Wconf:* flags are not ignored (passed via $optionsSource)") {
      val sourceFileName       = "example.scala"
      val warningConfOptions   = Seq("-Wconf:cat=deprecation:e", "-Wconf:any:s")
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
        val cliResErr    =
          cliRes.err.trim().linesIterator.toList
            // skip potentially irrelevant logs
            .dropWhile(_.contains("Check"))
            .mkString(System.lineSeparator())
        expect(cliResErr == scalacResErr)
      }
    }

    if (!sv.startsWith("2.12"))
      test(s"consecutive -Wunused:* flags are not ignored (passed via $optionsSource)") {
        val sourceFileName       = "example.scala"
        val unusedLintOptions    = Seq("-Wunused:locals", "-Wunused:privates")
        val maybeDirectiveString =
          if (useDirective) s"//> using options ${unusedLintOptions.mkString(" ")}" else ""
        TestInputs(os.rel / sourceFileName ->
          s"""//> using scala $sv
             |$maybeDirectiveString
             |object WUnusedExample {
             |  private def unusedPrivate(): String = "stuff"
             |  def methodWithUnusedLocal() = {
             |    val smth = "hello"
             |    println("Hello")
             |  }
             |}
             |""".stripMargin).fromRoot { root =>
          val r =
            os.proc(
              TestUtil.cli,
              "compile",
              sourceFileName,
              if (useDirective) Nil else unusedLintOptions
            )
              .call(cwd = root, stderr = os.Pipe)
          val err           = r.err.trim()
          val unusedKeyword = if (sv.startsWith("2")) "never used" else "unused"
          expect(err.linesIterator.exists(l => l.contains(unusedKeyword) && l.contains("local")))
          expect(err.linesIterator.exists(l => l.contains(unusedKeyword) && l.contains("private")))
        }
      }
  }

  {
    val prefixes = Seq("-", "--")
    for {
      prefix1 <- prefixes
      prefix2 <- prefixes
      optionKey = "Werror"
      option1   = prefix1 + optionKey
      option2   = prefix2 + optionKey
      if actualScalaVersion.startsWith("3")
    } test(
      s"allow to override $option1 compiler option passed via directive by passing $option2 from the command line"
    ) {
      val file = "example.scala"
      TestInputs(os.rel / file ->
        s"""//> using options -Wunused:all $option1
           |@main def main() = {
           |  val unused = ""
           |  println("Hello, world!")
           |}
           |""".stripMargin).fromRoot { root =>
        os.proc(
          TestUtil.cli,
          "compile",
          file,
          s"$option2:false",
          extraOptions
        )
          .call(cwd = root, stderr = os.Pipe)
      }
    }
  }

  for {
    scalaVersion <- Seq("3.nightly", "3.8.0-RC1-bin-20250825-ee2f641-NIGHTLY")
    withBloop    <- Seq(false, true)
    withBloopString = if (withBloop) "with Bloop" else "scalac"
    buildServerOpts = if (withBloop) Nil else Seq("--server=false")
  }
    test(s"sanity check for Scala $scalaVersion standard library with cc ($withBloopString)") {
      val input = "example.scala"
      TestInputs(os.rel / input ->
        s"""//> using scala $scalaVersion
           |import language.experimental.captureChecking
           |
           |trait File extends caps.SharedCapability:
           |  def count(): Int
           |
           |def f(file: File): IterableOnce[Int]^{file} =
           |  Iterator(1)
           |    .map(_ + file.count())
           |""".stripMargin).fromRoot { root =>
        os.proc(TestUtil.cli, "compile", input, buildServerOpts).call(cwd = root)
      }
    }
}
