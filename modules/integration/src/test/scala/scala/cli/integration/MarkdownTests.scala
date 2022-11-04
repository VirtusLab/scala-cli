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
        os.proc(TestUtil.cli, ".", "-e", snippetCode, "--markdown", "--main-class", "Main_md")
          .call(cwd = root)
      expect(result.out.trim() == expectedOutput)
    }
  }
}
