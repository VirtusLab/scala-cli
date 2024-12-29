package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class ScalafixTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  _: TestScalaVersion =>
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  protected val confFileName: String = ".scalafix.conf"

  protected val unusedRuleOption: String

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
    os.rel / confFileName ->
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
      os.proc(TestUtil.cli, "scalafix", ".", "--power", scalaVersionArgs).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("with --check") {
    simpleInputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "scalafix", "--power", "--check", ".", scalaVersionArgs).call(
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
      s"""//> using options $unusedRuleOption
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
      os.rel / confFileName ->
        s"""|rules = [
            |  RemoveUnused
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" -> unusedValueInputsContent
    )
    val expectedContent: String = noCrLf {
      s"""//> using options $unusedRuleOption
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
      os.proc(TestUtil.cli, "scalafix", "--power", ".", scalaVersionArgs).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("--rules args") {
    val input = TestInputs(
      os.rel / confFileName ->
        s"""|rules = [
            |  RemoveUnused,
            |  RedundantSyntax
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" ->
        s"""|//> using options $unusedRuleOption
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
        "scalafix",
        ".",
        "--rules",
        "RedundantSyntax",
        "--power",
        scalaVersionArgs
      ).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      val expected = noCrLf {
        s"""|//> using options $unusedRuleOption
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
         |final object Hello { // keep `final` beucase of parameter finalObject=false
         |  "Foo"
         |}
         |""".stripMargin
    }

    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "scalafix",
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

    val confFileName = "unusual-scalafix-filename"
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
        "scalafix",
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
    val original: String =
      """|//> using scalafix.dep com.github.xuwei-k::scalafix-rules:0.5.1
         |
         |object CollectHeadOptionTest {
         |  def x1: Option[String] = List(1, 2, 3).collect { case n if n % 2 == 0 => n.toString }.headOption
         |}
         |""".stripMargin
    val inputs: TestInputs = TestInputs(
      os.rel / confFileName ->
        s"""|rules = [
            |  CollectHeadOption
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" -> original
    )
    val expectedContent: String = noCrLf {
      """|//> using scalafix.dep com.github.xuwei-k::scalafix-rules:0.5.1
         |
         |object CollectHeadOptionTest {
         |  def x1: Option[String] = List(1, 2, 3).collectFirst{ case n if n % 2 == 0 => n.toString }
         |}
         |""".stripMargin
    }

    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "scalafix", ".", "--power", scalaVersionArgs).call(cwd = root)
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
      os.rel / confFileName ->
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
      os.proc(TestUtil.cli, "scalafix", ".", "--power", scalaVersionArgs).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }
}
