package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

trait RunPipedSourcesTestDefinitions { this: RunTestDefinitions =>
  def piping(): Unit = {
    emptyInputs.fromRoot { root =>
      val cliCmd         = (TestUtil.cli ++ extraOptions).mkString(" ")
      val cmd            = s""" echo 'println("Hello" + " from pipe")' | $cliCmd _.sc """
      val res            = os.proc("bash", "-c", cmd).call(cwd = root)
      val expectedOutput = "Hello from pipe" + System.lineSeparator()
      expect(res.out.text() == expectedOutput)
    }
  }

  if (!Properties.isWin) {
    test("piping") {
      piping()
    }
    test("Scripts accepted as piped input") {
      val message = "Hello"
      val input   = s"println(\"$message\")"
      emptyInputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, "-", extraOptions)
          .call(cwd = root, stdin = input)
          .out.trim()
        expect(output == message)
      }
    }
    test("Scala code accepted as piped input") {
      val expectedOutput = "Hello"
      val pipedInput     = s"object Test extends App { println(\"$expectedOutput\") }"
      emptyInputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, "_.scala", extraOptions)
          .call(cwd = root, stdin = pipedInput)
          .out.trim()
        expect(output == expectedOutput)
      }
    }
    test("Scala code with references to existing files accepted as piped input") {
      val expectedOutput = "Hello"
      val pipedInput     =
        s"""object Test extends App {
           |  val data = SomeData(value = "$expectedOutput")
           |  println(data.value)
           |}""".stripMargin
      val inputs = TestInputs(os.rel / "SomeData.scala" -> "case class SomeData(value: String)")
      inputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, ".", "_.scala", extraOptions)
          .call(cwd = root, stdin = pipedInput)
          .out.trim()
        expect(output == expectedOutput)
      }
    }
    test("Java code accepted as piped input") {
      val expectedOutput = "Hello"
      val pipedInput     =
        s"""public class Main {
           |    public static void main(String[] args) {
           |        System.out.println("$expectedOutput");
           |    }
           |}
           |""".stripMargin
      emptyInputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, "_.java", extraOptions)
          .call(cwd = root, stdin = pipedInput)
          .out.trim()
        expect(output == expectedOutput)
      }
    }

    for (javaVersion <- Constants.allJavaVersions.filter(_ >= Constants.jep512MinJavaVersion))
      test(s"piped compact Java source runs on JDK $javaVersion") {
        TestUtil.retryOnCi() {
          val pipedInput = """void main() { System.out.println("piped"); }"""
          emptyInputs.fromRoot { root =>
            val res = os.proc(TestUtil.cli, "_.java", "--jvm", TestUtil.jvmId(javaVersion))
              .call(cwd = root, stdin = pipedInput)
            expect(TestUtil.stableStdout(res) == "piped")
          }
        }
      }
    // java-class-name regressions are independent of the Scala version
    if scalaVersionArgs.isEmpty then {

      /** Runs a Java source whose class name has to be inferred by java-class-name. The sources
        * avoid string literals, so that they can be passed on the command line on all platforms.
        */
      def pipedJavaSourceTest(
        description: String,
        source: String,
        expectedOutput: String,
        jvmOptions: Seq[String] = Nil
      ): Unit =
        test(s"piped Java source: $description") {
          emptyInputs.fromRoot { root =>
            val res = os.proc(TestUtil.cli, "_.java", extraOptions, jvmOptions)
              .call(cwd = root, stdin = source, stderr = os.Pipe)
            expect(res.out.trim() == expectedOutput)
            // java-class-name used to report Java parser errors (e.g. for compact sources) to stderr
            expect(!res.err.text().contains("-- Error:"))
          }
        }

      // https://github.com/VirtusLab/scala-cli/issues/4514
      pipedJavaSourceTest(
        description = "public enum with methods",
        source =
          "public enum Color { RED, GREEN; public static void main(String[] args) { System.out.println(RED); } }",
        expectedOutput = "RED"
      )
      // https://github.com/VirtusLab/scala-cli/issues/4516
      pipedJavaSourceTest(
        description = "public record with primitive components",
        source =
          "public record Point(int x, int y) { public static void main(String[] args) { System.out.println(new Point(1, 2)); } }",
        expectedOutput = "Point[x=1, y=2]"
      )
      // https://github.com/VirtusLab/scala-cli/issues/4515
      pipedJavaSourceTest(
        description = "public class after a package-private one in a package",
        source =
          """package demo;
            |class Helper { static int answer() { return 42; } }
            |public class Main { public static void main(String[] args) { System.out.println(Helper.answer()); } }
            |""".stripMargin,
        expectedOutput = "42"
      )
      // JEP 512: the implicit class of a compact source file is named after the generated source file
      pipedJavaSourceTest(
        description = "compact source file with a public class before main",
        source =
          """public class Helper { static int answer() { return 42; } }
            |void main() { System.out.println(getClass().getName() + ' ' + Helper.answer()); }
            |""".stripMargin,
        expectedOutput = "stdin 42",
        jvmOptions = Seq("--jvm", TestUtil.jvmId(Constants.jep512MinJavaVersion))
      )
    }
    test("Java code with multiple classes accepted as piped input") {
      val expectedOutput = "Hello"
      val pipedInput     =
        s"""class OtherClass {
           |    public String message;
           |    public OtherClass(String message) {
           |      this.message = message;
           |    }
           |}
           |
           |public class Main {
           |    public static void main(String[] args) {
           |        OtherClass obj = new OtherClass("$expectedOutput");
           |        System.out.println(obj.message);
           |    }
           |}
           |""".stripMargin
      emptyInputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, "_.java", extraOptions)
          .call(cwd = root, stdin = pipedInput)
          .out.trim()
        expect(output == expectedOutput)
      }
    }
    test(
      "snippets mixed with piped Scala code and existing sources allow for cross-references"
    ) {
      val hello          = "Hello"
      val comma          = ", "
      val world          = "World"
      val exclamation    = "!"
      val expectedOutput = hello + comma + world + exclamation
      val scriptSnippet  = s"def world = \"$world\""
      val scalaSnippet   = "case class ScalaSnippetData(value: String)"
      val javaSnippet    =
        s"public class JavaSnippet { public static String exclamation = \"$exclamation\"; }"
      val pipedInput = s"def hello = \"$hello\""
      val inputs     =
        TestInputs(os.rel / "Main.scala" ->
          s"""object Main extends App {
             |  val hello = stdin.hello
             |  val comma = ScalaSnippetData(value = "$comma").value
             |  val world = snippet.world
             |  val exclamation = JavaSnippet.exclamation
             |  println(hello + comma + world + exclamation)
             |}
             |""".stripMargin)
      inputs.fromRoot { root =>
        val output =
          os.proc(
            TestUtil.cli,
            ".",
            "_.sc",
            "--script-snippet",
            scriptSnippet,
            "--scala-snippet",
            scalaSnippet,
            "--java-snippet",
            javaSnippet,
            extraOptions
          )
            .call(cwd = root, stdin = pipedInput)
            .out.trim()
        expect(output == expectedOutput)
      }
    }
    test("pick .scala main class over in-context scripts, including piped ones") {
      val inputs = TestInputs(
        os.rel / "Hello.scala" ->
          """object Hello extends App {
            |  println(s"${stdin.hello} ${scripts.Script.world}")
            |}
            |""".stripMargin,
        os.rel / "scripts" / "Script.sc" -> """def world: String = "world""""
      )
      val pipedInput = """def hello: String = "Hello""""
      inputs.fromRoot { root =>
        val res = os.proc(
          TestUtil.cli,
          "run",
          extraOptions,
          ".",
          "_.sc"
        )
          .call(cwd = root, stdin = pipedInput)
        expect(res.out.trim() == "Hello world")
      }
    }
    test("pick piped .scala main class over in-context scripts") {
      val inputs = TestInputs(
        os.rel / "Hello.scala" ->
          """object Hello {
            |  def hello: String = "Hello"
            |}
            |""".stripMargin,
        os.rel / "scripts" / "Script.sc" -> """def world: String = "world""""
      )
      val pipedInput =
        """object Main extends App {
          |  println(s"${Hello.hello} ${scripts.Script.world}")
          |}
          |""".stripMargin
      inputs.fromRoot { root =>
        val res = os.proc(
          TestUtil.cli,
          "run",
          extraOptions,
          ".",
          "_.scala"
        )
          .call(cwd = root, stdin = pipedInput)
        expect(res.out.trim() == "Hello world")
      }
    }
  }

  test("Markdown code with a scala snippet accepted as piped input") {
    val expectedOutput = "Hello"
    val pipedInput     =
      s"""# Piped Markdown
         |A simple `scala` snippet
         |```scala
         |println("$expectedOutput")
         |```
         |""".stripMargin
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "_.md", extraOptions)
        .call(cwd = root, stdin = pipedInput)
        .out.trim()
      expect(output == expectedOutput)
    }
  }
}
