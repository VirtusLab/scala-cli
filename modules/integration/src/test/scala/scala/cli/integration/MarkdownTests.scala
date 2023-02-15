package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class MarkdownTests extends ScalaCliSuite {
  test("run a simple .md file with a scala script snippet") {
    val expectedOutput = "Hello"
    TestInputs(
      os.rel / "sample.md" ->
        s"""# Sample Markdown file
           |A simple scala script snippet.
           |```scala
           |println("$expectedOutput")
           |```
           |""".stripMargin
    ).fromRoot { root =>
      val result = os.proc(TestUtil.cli, "sample.md").call(cwd = root)
      expect(result.out.trim() == expectedOutput)
    }
  }

  test("run a simple .md file with a scala raw snippet") {
    val expectedOutput = "Hello"
    TestInputs(
      os.rel / "sample.md" ->
        s"""# Sample Markdown file
           |A simple scala raw snippet.
           |```scala raw
           |object Hello extends App {
           |  println("$expectedOutput")
           |}
           |```
           |""".stripMargin
    ).fromRoot { root =>
      val result = os.proc(TestUtil.cli, "sample.md").call(cwd = root)
      expect(result.out.trim() == expectedOutput)
    }
  }

  test("run a simple .md file with multiple interdependent scala snippets") {
    val expectedOutput = "Hello"
    TestInputs(
      os.rel / "sample.md" ->
        s"""# Sample Markdown file
           |## 1
           |message case class
           |```scala
           |case class Message(value: String)
           |```
           |## 2
           |message declaration
           |```scala
           |val message = Message("$expectedOutput")
           |```
           |
           |## 3
           |output
           |```scala
           |println(message.value)
           |```
           |
           |##
           |""".stripMargin
    ).fromRoot { root =>
      val result = os.proc(TestUtil.cli, "sample.md").call(cwd = root)
      expect(result.out.trim() == expectedOutput)
    }
  }

  test("run a simple .md file with multiple scala snippets resetting the context") {
    val msg1           = "Hello"
    val msg2           = "world"
    val expectedOutput = s"$msg1 $msg2"
    TestInputs(
      os.rel / "sample.md" ->
        s"""# Sample Markdown file
           |## Scope 0
           |```scala
           |val msg = "$msg1"
           |```
           |
           |## Scope 1
           |```scala reset
           |val msg = " "
           |```
           |
           |## Scope 2
           |```scala reset
           |val msg = "$msg2"
           |```
           |
           |## Output
           |```scala reset
           |val msg = Scope.msg + Scope1.msg + Scope2.msg
           |println(msg)
           |```
           |##
           |""".stripMargin
    ).fromRoot { root =>
      val result = os.proc(TestUtil.cli, "sample.md").call(cwd = root)
      expect(result.out.trim() == expectedOutput)
    }
  }

  test("run markdown alongside other sources") {
    val msg1           = "Hello"
    val msg2           = " "
    val msg3           = "world"
    val msg4           = "!"
    val expectedOutput = msg1 + msg2 + msg3 + msg4
    TestInputs(
      os.rel / "ScalaMessage.scala" -> "case class ScalaMessage(value: String)",
      os.rel / "JavaMessage.java" ->
        """public class JavaMessage {
          |  public String value;
          |  public JavaMessage(String value) {
          |    this.value = value;
          |  }
          |}
          |""".stripMargin,
      os.rel / "scripts" / "script.sc" -> "case class ScriptMessage(value: String)",
      os.rel / "Main.md" ->
        s"""# Main
           |Run it all from a snippet.
           |```scala
           |val javaMsg = new JavaMessage("$msg1")
           |val scalaMsg = ScalaMessage("$msg2")
           |val snippetMsg = snippet.SnippetMessage("$msg3")
           |val scriptMsg = scripts.script.ScriptMessage("$msg4")
           |val msg = javaMsg.value + scalaMsg.value + snippetMsg.value + scriptMsg.value
           |println(msg)
           |```
           |""".stripMargin
    ).fromRoot { root =>
      val snippetCode = "case class SnippetMessage(value: String)"
      val result =
        os.proc(
          TestUtil.cli,
          "--power",
          ".",
          "-e",
          snippetCode,
          "--markdown",
          "--main-class",
          "Main_md"
        )
          .call(cwd = root)
      expect(result.out.trim() == expectedOutput)
    }
  }
  test("run a simple .md file with a using directive in a scala script snippet") {
    TestInputs(
      os.rel / "sample.md" ->
        s"""# Sample Markdown file
           |A simple scala script snippet.
           |```scala
           |//> using dep "com.lihaoyi::os-lib:0.8.1"
           |println(os.pwd)
           |```
           |""".stripMargin
    ).fromRoot { root =>
      val result = os.proc(TestUtil.cli, "sample.md").call(cwd = root)
      expect(result.out.trim() == root.toString())
    }
  }

  test("run a simple .md file with a using directive in a scala raw snippet") {
    TestInputs(
      os.rel / "sample.md" ->
        s"""# Sample Markdown file
           |A simple scala raw snippet.
           |```scala raw
           |//> using dep "com.lihaoyi::os-lib:0.8.1"
           |object Hello extends App {
           |  println(os.pwd)
           |}
           |```
           |""".stripMargin
    ).fromRoot { root =>
      val result = os.proc(TestUtil.cli, "sample.md").call(cwd = root)
      expect(result.out.trim() == root.toString())
    }
  }

  test("run a simple .md file with multiple using directives spread across scala script snippets") {
    val msg1 = "Hello"
    val msg2 = "world"
    TestInputs(
      os.rel / "sample.md" ->
        s"""# Sample Markdown file
           |Let's try to spread the using directives into multiple simple `scala` code blocks of various kinds.
           |
           |## Circe
           |Let's depend on `circe-parser` in this one.
           |```scala
           |//> using dep "io.circe::circe-parser:0.14.3"
           |import io.circe._, io.circe.parser._
           |val json = \"\"\"{ "message": "$msg1"}\"\"\"
           |val parsed = parse(json).getOrElse(Json.Null)
           |val msg = parsed.hcursor.downField("message").as[String].getOrElse("")
           |println(msg)
           |```
           |
           |## `pprint`
           |And `pprint`, too.
           |```scala
           |//> using dep "com.lihaoyi::pprint:0.8.0"
           |pprint.PPrinter.BlackWhite.pprintln("$msg2")
           |```
           |
           |## `os-lib`
           |And then on `os-lib`, just because.
           |And let's reset the scope for good measure, too.
           |```scala reset
           |//> using dep "com.lihaoyi::os-lib:0.8.1"
           |val msg = os.pwd.toString
           |println(msg)
           |```
           |""".stripMargin
    ).fromRoot { root =>
      val result = os.proc(TestUtil.cli, "sample.md").call(cwd = root)
      val expectedOutput =
        s"""$msg1
           |"$msg2"
           |$root""".stripMargin
      expect(result.out.trim() == expectedOutput)
    }
  }

  test("run a simple .md file with multiple using directives spread across scala raw snippets") {
    val msg1 = "Hello"
    val msg2 = "world"
    TestInputs(
      os.rel / "sample.md" ->
        s"""# Sample Markdown file
           |Let's try to spread the using directives into multiple simple `scala` code blocks of various kinds.
           |
           |## Circe
           |Let's depend on `circe-parser` in this one.
           |```scala raw
           |//> using dep "io.circe::circe-parser:0.14.3"
           |
           |object CirceSnippet {
           |  import io.circe._, io.circe.parser._
           |
           |  def printStuff(): Unit = {
           |    val json = \"\"\"{ "message": "$msg1"}\"\"\"
           |    val parsed = parse(json).getOrElse(Json.Null)
           |    val msg = parsed.hcursor.downField("message").as[String].getOrElse("")
           |    println(msg)
           |  }
           |}
           |```
           |
           |## `pprint`
           |And `pprint`, too.
           |```scala raw
           |//> using dep "com.lihaoyi::pprint:0.8.0"
           |object PprintSnippet {
           |  def printStuff(): Unit =
           |    pprint.PPrinter.BlackWhite.pprintln("$msg2")
           |}
           |```
           |
           |## `os-lib`
           |And then on `os-lib`, just because.
           |And let's reset the scope for good measure, too.
           |```scala raw
           |//> using dep "com.lihaoyi::os-lib:0.8.1"
           |
           |object OsLibSnippet extends App {
           |  CirceSnippet.printStuff()
           |  PprintSnippet.printStuff()
           |  println(os.pwd)
           |}
           |```
           |""".stripMargin
    ).fromRoot { root =>
      val result = os.proc(TestUtil.cli, "sample.md").call(cwd = root)
      val expectedOutput =
        s"""$msg1
           |"$msg2"
           |$root""".stripMargin
      expect(result.out.trim() == expectedOutput)
    }
  }
}
