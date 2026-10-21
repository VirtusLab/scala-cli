package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.{Charset, StandardCharsets}

import scala.cli.integration.TestInputs.compress
import scala.cli.integration.TestUtil.stableStdoutOf

trait RunZipTestDefinitions { this: RunTestDefinitions =>
  test("Zip with multiple Scala files") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""object Hello extends App {
           |  println(Messages.hello)
           |}
           |""".stripMargin,
      os.rel / "Messages.scala" ->
        s"""object Messages {
           |  def hello: String = "Hello"
           |}
           |""".stripMargin
    )
    inputs.asZip { (root, zipPath) =>
      val message = "Hello"
      val output  = stableStdoutOf(TestUtil.cli, extraOptions, zipPath.toString)(cwd = root)
      expect(output == message)
    }
  }
  test("load virtual data with UTF_16 encoding") {
    val zipInputs: Seq[(os.RelPath, String, Charset)] = Seq(
      (
        os.rel / "Hello.scala",
        s"""//> using resourceDir ./
           |import scala.io.Source
           |import java.nio.charset.StandardCharsets
           |import java.io.{BufferedReader, InputStreamReader}
           |import java.util.stream.Collectors
           |
           |object Hello extends App {
           |    val inputStream = getClass().getResourceAsStream("input")
           |    val nativeResourceText = new BufferedReader(
           |      new InputStreamReader(inputStream, StandardCharsets.UTF_16)
           |    ).lines().collect(Collectors.joining("\\n"));
           |    println(nativeResourceText)
           |}
           |""".stripMargin,
        StandardCharsets.UTF_8
      ),
      (
        os.rel / "input",
        s"""1
           |2
           |""".stripMargin,
        StandardCharsets.UTF_16
      )
    )
    TestInputs().fromRoot { root =>
      val zipArchivePath = root / "hello.zip"
      compress(zipArchivePath, zipInputs)

      val output = stableStdoutOf(TestUtil.cli, extraOptions, zipArchivePath.toString)(cwd = root)

      val expectedOutput = "1\n2"

      expect(output == expectedOutput)
    }
  }

  test("Zip with Scala containing resource directive") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using resourceDir ./
           |import scala.io.Source
           |
           |object Hello extends App {
           |    val inputs = Source.fromResource("input").getLines.map(_.toInt).toSeq
           |    println(inputs.mkString(","))
           |}
           |""".stripMargin,
      os.rel / "input" ->
        s"""1
           |2
           |""".stripMargin
    )
    inputs.asZip { (root, zipPath) =>
      val message = "1,2"

      val output = stableStdoutOf(TestUtil.cli, extraOptions, zipPath.toString)(cwd = root)
      expect(output == message)
    }
  }

  test("Zip with Scala Script containing resource directive") {
    val inputs = TestInputs(
      os.rel / "hello.sc" ->
        s"""//> using resourceDir ./
           |import scala.io.Source
           |
           |val inputs = Source.fromResource("input").getLines.map(_.toInt).toSeq
           |println(inputs.mkString(","))
           |""".stripMargin,
      os.rel / "input" ->
        s"""1
           |2
           |""".stripMargin
    )
    inputs.asZip { (root, zipPath) =>
      val message = "1,2"

      val output = stableStdoutOf(TestUtil.cli, extraOptions, zipPath.toString)(cwd = root)
      expect(output == message)
    }
  }

  test("Zip with Markdown containing resource directive") {
    val (inputA, inputB) = "1" -> "2"
    val expectedMessage  = s"$inputA,$inputB"
    val inputs           = TestInputs(
      os.rel / "Hello.md" ->
        s"""# Example Markdown file
           |A snippet for printing inputs from resources
           |```scala raw
           |//> using resourceDir ./
           |import scala.io.Source
           |object Hello extends App {
           |  val inputs = Source.fromResource("input").getLines.map(_.toInt).toSeq
           |  println(inputs.mkString(","))
           |}
           |```
           |""".stripMargin,
      os.rel / "input" ->
        s"""$inputA
           |$inputB
           |""".stripMargin
    )
    inputs.asZip { (root, zipPath) =>
      val result =
        stableStdoutOf(TestUtil.cli, "--power", extraOptions, zipPath, "--md")(cwd = root)
      expect(result == expectedMessage)
    }
  }
}
