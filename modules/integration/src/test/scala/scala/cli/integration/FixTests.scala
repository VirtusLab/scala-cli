package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class FixTests extends ScalaCliSuite {
  val projectFileName = Constants.projectFileName

  val fooInput = os.rel / "files" / "Foo.scala" ->
    """//> using dep "com.lihaoyi::os-lib::0.8.1"
      |
      |case class Foo()
      |""".stripMargin

  val barInput = os.rel / "files" / "Bar.scala" ->
    """//> using options "-Xasync"
      |
      |case class Bar()
      |""".stripMargin

  val bazInput = os.rel / "files" / "Baz.scala" -> "case class Baz()"

  val expectedFooFileContent =
    """case class Foo()
      |""".stripMargin

  val expectedBarFileContent =
    """case class Bar()
      |""".stripMargin

  test("migrate-directives with project.scala in inputs") {
    val inputs = TestInputs(
      fooInput,
      barInput,
      os.rel / projectFileName -> "//> using scala \"3.2.0\""
    )

    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "--power", "fix", "--migrate-directives", ".")
        .call(cwd = root, stderr = os.Pipe).err.trim()

      val projectFileContent = noCrLf(os.read(root / projectFileName))
      val fooFileContent     = noCrLf(os.read(root / "files" / "Foo.scala"))
      val barFileContent     = noCrLf(os.read(root / "files" / "Bar.scala"))
      val expectedProjectFileContent =
        """//> using scala "3.2.0"
          |//> using options "-Xasync"
          |
          |
          |//> using dep "com.lihaoyi::os-lib::0.8.1"
          |
          |
          |""".stripMargin

      expect(output.contains("Using directives found in 3 files"))
      expect(output.contains("Found existing project file at"))
      expect(projectFileContent == noCrLf(expectedProjectFileContent))
      expect(fooFileContent == noCrLf(expectedFooFileContent))
      expect(barFileContent == noCrLf(expectedBarFileContent))
    }
  }

  test("migrate-directives without project.scala in inputs") {
    val inputs = TestInputs(fooInput, barInput)

    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "--power", "fix", "--migrate-directives", ".")
        .call(cwd = root, stderr = os.Pipe).err.trim()

      val projectFileContent = noCrLf(os.read(root / projectFileName))
      val fooFileContent     = noCrLf(os.read(root / "files" / "Foo.scala"))
      val barFileContent     = noCrLf(os.read(root / "files" / "Bar.scala"))
      val expectedProjectFileContent =
        """//> using options "-Xasync"
          |
          |
          |//> using dep "com.lihaoyi::os-lib::0.8.1"
          |
          |""".stripMargin

      expect(output.contains("Using directives found in 2 files"))
      expect(output.contains("Creating project file at"))
      expect(os.exists(root / projectFileName))
      expect(projectFileContent == noCrLf(expectedProjectFileContent))
      expect(fooFileContent == noCrLf(expectedFooFileContent))
      expect(barFileContent == noCrLf(expectedBarFileContent))
    }
  }

  test("migrate-directives with one file with using directives") {
    val inputs = TestInputs(fooInput, bazInput)

    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "--power", "fix", "--migrate-directives", ".")
        .call(cwd = root, stderr = os.Pipe).err.trim()

      val fooFileContent = noCrLf(os.read(root / "files" / "Foo.scala"))
      val bazFileContent = noCrLf(os.read(root / "files" / "Baz.scala"))

      expect(output == "All using directives are already in one file")
      expect(!os.exists(root / projectFileName))
      expect(fooFileContent == noCrLf(fooInput._2))
      expect(bazFileContent == noCrLf(bazInput._2))
    }
  }

  test("migrate-directives without files with using directives") {
    val inputs = TestInputs(bazInput, os.rel / projectFileName -> "")

    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "--power", "fix", "--migrate-directives", ".")
        .call(cwd = root, stderr = os.Pipe).err.trim()

      val bazFileContent     = noCrLf(os.read(root / "files" / "Baz.scala"))
      val projectFileContent = noCrLf(os.read(root / projectFileName))

      expect(output == "No using directives have been found")
      expect(bazFileContent == noCrLf(bazInput._2))
      expect(projectFileContent == "")
    }
  }

  private def noCrLf(input: String): String =
    input.replaceAll("\r\n", "\n")
}
