package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait RunSnippetTestDefinitions { _: RunTestDefinitions =>
  test("correctly run a script snippet") {
    emptyInputs.fromRoot { root =>
      val msg       = "Hello world"
      val quotation = TestUtil.argQuotationMark
      val res =
        os.proc(
          TestUtil.cli,
          "run",
          "--script-snippet",
          s"println($quotation$msg$quotation)",
          extraOptions
        )
          .call(cwd = root)
      expect(res.out.trim() == msg)
    }
  }

  test("correctly run a scala snippet") {
    emptyInputs.fromRoot { root =>
      val msg       = "Hello world"
      val quotation = TestUtil.argQuotationMark
      val res =
        os.proc(
          TestUtil.cli,
          "run",
          "--scala-snippet",
          s"object Hello extends App { println($quotation$msg$quotation) }",
          extraOptions
        )
          .call(cwd = root)
      expect(res.out.trim() == msg)
    }
  }

  test("correctly run a java snippet") {
    emptyInputs.fromRoot { root =>
      val quotation = TestUtil.argQuotationMark
      val msg       = "Hello world"
      val res = os.proc(
        TestUtil.cli,
        "run",
        "--java-snippet",
        s"public class Main { public static void main(String[] args) { System.out.println($quotation$msg$quotation); } }",
        extraOptions
      )
        .call(cwd = root)
      expect(res.out.trim() == msg)
    }
  }

  test("correctly run a markdown snippet") {
    emptyInputs.fromRoot { root =>
      val msg       = "Hello world"
      val quotation = TestUtil.argQuotationMark
      val res =
        os.proc(
          TestUtil.cli,
          "run",
          "--markdown-snippet",
          s"""# A Markdown snippet
             |With some scala code
             |```scala
             |println($quotation$msg$quotation)
             |```""".stripMargin,
          extraOptions
        )
          .call(cwd = root)
      expect(res.out.trim() == msg)
    }
  }

  test("correctly run multiple snippets") {
    emptyInputs.fromRoot { root =>
      val quotation = TestUtil.argQuotationMark

      val scriptSnippetOption = "--script-snippet"
      val scriptMessages @ Seq(scriptMsg1, scriptMsg2, scriptMsg3) =
        Seq("hello script 1", "hello script 2", "hello script 3")
      val script0 =
        s"""def printAll(): Unit = println(
           |  Seq(
           |    snippet1.scriptMsg1, snippet2.scriptMsg2, snippet3.scriptMsg3,
           |    ScalaSnippet1.msg, ScalaSnippet2.msg, ScalaSnippet3.msg,
           |    JavaSnippet1.msg, JavaSnippet2.msg, JavaSnippet3.msg
           |  ).mkString($quotation;$quotation)
           |)""".stripMargin
      val script1 = s"def scriptMsg1: String = $quotation$scriptMsg1$quotation"
      val script2 = s"def scriptMsg2: String = $quotation$scriptMsg2$quotation"
      val script3 = s"def scriptMsg3: String = $quotation$scriptMsg3$quotation"

      val scalaSnippetOption = "--scala-snippet"
      val scalaMessages @ Seq(scalaMsg1, scalaMsg2, scalaMsg3) =
        Seq("hello scala 1", "hello scala 2", "hello scala 3")
      val scala0 = "object SnippetMain extends App { snippet.printAll() }"
      val scala1 = s"object ScalaSnippet1 { def msg: String = $quotation$scalaMsg1$quotation }"
      val scala2 = s"object ScalaSnippet2 { def msg: String = $quotation$scalaMsg2$quotation }"
      val scala3 = s"object ScalaSnippet3 { def msg: String = $quotation$scalaMsg3$quotation }"

      val javaSnippetOption = "--java-snippet"
      val javaMessages @ Seq(javaMsg1, javaMsg2, javaMsg3) =
        Seq("hello scala 1", "hello scala 2", "hello scala 3")
      val java1 =
        s"public class JavaSnippet1 { public static String msg = $quotation$javaMsg1$quotation; }"
      val java2 =
        s"public class JavaSnippet2 { public static String msg = $quotation$javaMsg2$quotation; }"
      val java3 =
        s"public class JavaSnippet3 { public static String msg = $quotation$javaMsg3$quotation; }"

      val expectedOutput = (scriptMessages ++ scalaMessages ++ javaMessages).mkString(";")

      val res = os.proc(
        TestUtil.cli,
        "run",
        scriptSnippetOption,
        script0,
        scriptSnippetOption,
        script1,
        scriptSnippetOption,
        script2,
        scriptSnippetOption,
        script3,
        scalaSnippetOption,
        scala0,
        scalaSnippetOption,
        scala1,
        scalaSnippetOption,
        scala2,
        scalaSnippetOption,
        scala3,
        javaSnippetOption,
        java1,
        javaSnippetOption,
        java2,
        javaSnippetOption,
        java3,
        extraOptions
      )
        .call(cwd = root)
      expect(res.out.trim() == expectedOutput)
    }
  }
}
