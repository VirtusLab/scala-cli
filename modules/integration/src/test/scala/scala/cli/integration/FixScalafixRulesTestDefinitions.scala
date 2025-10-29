package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

trait FixScalafixRulesTestDefinitions {
  this: FixTestDefinitions =>
  protected val scalafixConfFileName: String = ".scalafix.conf"
  protected def scalafixUnusedRuleOption: String
  protected def noCrLf(input: String): String =
    input.replaceAll("\r\n", "\n")

  private val simpleInputsOriginalContent: String =
    """package foo
      |
      |final object Hello {
      |  def main(args: Array[String]): Unit = {
      |    println("Hello")
      |  }
      |}
      |""".stripMargin
  private val simpleInputs: TestInputs = TestInputs(
    os.rel / scalafixConfFileName ->
      s"""|rules = [
          |  RedundantSyntax
          |]
          |""".stripMargin,
    os.rel / "Hello.scala" -> simpleInputsOriginalContent
  )
  private val expectedContent: String = noCrLf {
    """package foo
      |
      |object Hello {
      |  def main(args: Array[String]): Unit = {
      |    println("Hello")
      |  }
      |}
      |""".stripMargin
  }

  test("simple") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "fix", ".", "--power", scalaVersionArgs).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("with --check") {
    simpleInputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "fix", "--power", "--check", ".", scalaVersionArgs).call(
        cwd = root,
        check = false
      )
      expect(res.exitCode != 0)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == noCrLf(simpleInputsOriginalContent))
    }
  }

  test("semantic rule") {
    val unusedValueInputsContent: String =
      s"""//> using options $scalafixUnusedRuleOption
         |package foo
         |
         |object Hello {
         |  def main(args: Array[String]): Unit = {
         |    val name = "John"
         |    println("Hello")
         |  }
         |}
         |""".stripMargin
    val semanticRuleInputs: TestInputs = TestInputs(
      os.rel / scalafixConfFileName ->
        s"""|rules = [
            |  RemoveUnused
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" -> unusedValueInputsContent
    )
    val expectedContent: String = noCrLf {
      s"""//> using options $scalafixUnusedRuleOption
         |package foo
         |
         |object Hello {
         |  def main(args: Array[String]): Unit = {
         |    
         |    println("Hello")
         |  }
         |}
         |""".stripMargin
    }

    semanticRuleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "fix", "--power", ".", scalaVersionArgs).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("--rules args") {
    val input = TestInputs(
      os.rel / scalafixConfFileName ->
        s"""|rules = [
            |  RemoveUnused,
            |  RedundantSyntax
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" ->
        s"""|//> using options $scalafixUnusedRuleOption
            |package hello
            |
            |object Hello {
            |  def a = {
            |    val x = 1 // keep unused - exec only RedundantSyntax
            |    s"Foo"
            |  }
            |}
            |""".stripMargin
    )

    input.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "fix",
        ".",
        "--scalafix-rules",
        "RedundantSyntax",
        "--power",
        scalaVersionArgs
      ).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      val expected       = noCrLf {
        s"""|//> using options $scalafixUnusedRuleOption
            |package hello
            |
            |object Hello {
            |  def a = {
            |    val x = 1 // keep unused - exec only RedundantSyntax
            |    "Foo"
            |  }
            |}
            |""".stripMargin
      }

      expect(updatedContent == expected)

    }
  }

  test("--scalafix-arg arg") {
    val original: String =
      """|package foo
         |
         |final object Hello { // keep `final` beucase of parameter finalObject=false
         |  s"Foo"
         |}
         |""".stripMargin
    val inputs: TestInputs = TestInputs(
      os.rel / scalafixConfFileName ->
        s"""|rules = [
            |  RedundantSyntax
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" -> original
    )
    val expectedContent: String = noCrLf {
      """|package foo
         |
         |final object Hello { // keep `final` beucase of parameter finalObject=false
         |  "Foo"
         |}
         |""".stripMargin
    }

    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "fix",
        ".",
        "--scalafix-arg=--settings.RedundantSyntax.finalObject=false",
        "--power",
        scalaVersionArgs
      ).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("--scalafix-conf arg") {
    val original: String =
      """|package foo
         |
         |final object Hello {
         |  s"Foo"
         |}
         |""".stripMargin

    val confFileName       = "unusual-scalafix-filename"
    val inputs: TestInputs = TestInputs(
      os.rel / confFileName ->
        s"""|rules = [
            |  RedundantSyntax
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" -> original
    )
    val expectedContent: String = noCrLf {
      """|package foo
         |
         |object Hello {
         |  "Foo"
         |}
         |""".stripMargin
    }

    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "fix",
        ".",
        s"--scalafix-conf=$confFileName",
        "--power",
        scalaVersionArgs
      ).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("external rule") {
    val directive        = s"//> using scalafixDependency com.github.xuwei-k::scalafix-rules:0.5.1"
    val original: String =
      s"""|$directive
          |
          |object CollectHeadOptionTest {
          |  def x1: Option[String] = List(1, 2, 3).collect { case n if n % 2 == 0 => n.toString }.headOption
          |}
          |""".stripMargin
    val inputs: TestInputs = TestInputs(
      os.rel / scalafixConfFileName ->
        s"""|rules = [
            |  CollectHeadOption
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" -> original
    )
    val expectedContent: String = noCrLf {
      s"""|$directive
          |
          |object CollectHeadOptionTest {
          |  def x1: Option[String] = List(1, 2, 3).collectFirst{ case n if n % 2 == 0 => n.toString }
          |}
          |""".stripMargin
    }

    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "fix", ".", "--power", scalaVersionArgs).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("explicit-result-types") {
    val original: String =
      """|package foo
         |
         |object Hello {
         |  def a(a: Int) = "asdasd" + a.toString
         |}
         |""".stripMargin
    val inputs: TestInputs = TestInputs(
      os.rel / scalafixConfFileName ->
        s"""|rules = [
            |  ExplicitResultTypes
            |]
            |ExplicitResultTypes.fetchScala3CompilerArtifactsOnVersionMismatch = true
            |""".stripMargin,
      os.rel / "Hello.scala" -> original
    )
    val expectedContent: String = noCrLf {
      """|package foo
         |
         |object Hello {
         |  def a(a: Int): String = "asdasd" + a.toString
         |}
         |""".stripMargin
    }

    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "fix", ".", "--power", scalaVersionArgs).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  for {
    (semanticDbOptions, expectedSuccess) <- Seq(
      Nil -> true, // .semanticdb files should be implicitly generated when Scalafix is run
      Seq("--semanticdb")       -> true,
      Seq("--semanticdb=false") -> false
    )
    semanticDbOptionsDescription =
      if (semanticDbOptions.nonEmpty) s" (${semanticDbOptions.mkString(" ")})"
      else ""
    verb = if (expectedSuccess) "run" else "fail"
    if !Properties.isWin || expectedSuccess
  }
    test(
      s"scalafix rules requiring SemanticDB $verb correctly with test scope sources$semanticDbOptionsDescription"
    ) {
      val compilerOptions =
        if (actualScalaVersion.startsWith("2.12")) Seq("-Ywarn-unused-import")
        else Seq("-Wunused:imports")
      TestInputs(
        os.rel / scalafixConfFileName ->
          """rules = [
            |  DisableSyntax,
            |  LeakingImplicitClassVal,
            |  NoValInForComprehension,
            |  ExplicitResultTypes,
            |  OrganizeImports
            |]
            |ExplicitResultTypes.fetchScala3CompilerArtifactsOnVersionMismatch = true
            |""".stripMargin,
        os.rel / projectFileName ->
          s"""//> using test.dep org.scalameta::munit::${Constants.munitVersion}
             |//> using options ${compilerOptions.mkString(" ")}
             |""".stripMargin,
        os.rel / "example.test.scala" ->
          """import munit.FunSuite
            |
            |class Munit extends FunSuite {
            |  test("foo") {
            |    assert(2 + 2 == 4)
            |    println("Hello from Munit")
            |  }
            |}
            |""".stripMargin
      ).fromRoot { root =>
        val res = os.proc(TestUtil.cli, "fix", ".", "--power", semanticDbOptions, extraOptions)
          .call(cwd = root, check = false, stderr = os.Pipe)
        val successful = res.exitCode == 0
        expect(successful == expectedSuccess)
        if (!expectedSuccess)
          expect(
            res.err.trim().contains("SemanticDB files' generation was explicitly set to false")
          )
      }
    }
}
