package scala.build.tests.markdown

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.Cache.Fetch
import coursier.cache.{ArchiveCache, ArtifactError, Cache}
import coursier.util.{Artifact, EitherT, Task}

import java.io.File
import java.nio.charset.StandardCharsets

import scala.build.Ops.*
import scala.build.input.ScalaCliInvokeData
import scala.build.options.{BuildOptions, Scope, SuppressWarningOptions}
import scala.build.preprocessing.Preprocessor
import scala.build.tests.{TestInputs, TestLogger, TestUtil}
import scala.build.{CrossSources, Sources}
import scala.concurrent.ExecutionContext

class MarkdownPreprocessorTests extends TestUtil.ScalaCliBuildSuite {
  given ScalaCliInvokeData = ScalaCliInvokeData.dummy

  private val preprocessors: Seq[Preprocessor] = Sources.defaultPreprocessors(
    ArchiveCache().copy(cache =
      new Cache[Task] {
        def fetch: Fetch[Task] = _ => sys.error("shouldn't be used")
        def file(artifact: Artifact): EitherT[Task, ArtifactError, File] =
          sys.error("shouldn't be used")
        def ec: ExecutionContext = sys.error("shouldn't be used")
      }
    ),
    javaClassNameVersionOpt = None,
    javaCommand = () => sys.error("shouldn't be used")
  )

  test("a markdown file with a public Java class emits a matching .java source") {
    val mdPath = os.rel / "Example.md"
    TestInputs(
      mdPath ->
        """# Example
          |
          |```java
          |public class Foo {
          |  public static void main(String[] args) {
          |    System.out.println("Hello");
          |  }
          |}
          |```""".stripMargin
    ).withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val mainSources =
        crossSources.scopedSources(BuildOptions()).orThrow
          .sources(Scope.Main, crossSources.sharedOptions(BuildOptions()), root, TestLogger())
          .orThrow

      val generatedRelPath = mainSources.inMemory.head.generatedRelPath
      val expectedRelPath  = os.rel / "Foo.java"
      expect(mainSources.paths.isEmpty)
      expect(mainSources.inMemory.length == 1)
      expect(generatedRelPath == expectedRelPath)
      expect(
        new String(mainSources.inMemory.head.content, StandardCharsets.UTF_8).contains("class Foo")
      )
    }
  }

  test(
    "a markdown file with a Java snippet without a public class emits a synthetic .java source"
  ) {
    val mdPath = os.rel / "Example.md"
    TestInputs(
      mdPath ->
        """# Example
          |
          |```java
          |class Helper {
          |  static String msg() { return "Hello"; }
          |}
          |```""".stripMargin
    ).withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val mainSources =
        crossSources.scopedSources(BuildOptions()).orThrow
          .sources(Scope.Main, crossSources.sharedOptions(BuildOptions()), root, TestLogger())
          .orThrow

      val generatedRelPath = mainSources.inMemory.head.generatedRelPath
      val expectedRelPath  = os.rel / "Example_md_snippet0.java"
      expect(mainSources.inMemory.length == 1)
      expect(generatedRelPath == expectedRelPath)
    }
  }

  test("a markdown file with a java test snippet is routed to the test scope") {
    val mdPath = os.rel / "Example.md"
    TestInputs(
      mdPath ->
        """# Example
          |
          |```java test
          |public class BarTest {
          |  public void test() {}
          |}
          |```""".stripMargin
    ).withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val testSources =
        crossSources.scopedSources(BuildOptions()).orThrow
          .sources(Scope.Test, crossSources.sharedOptions(BuildOptions()), root, TestLogger())
          .orThrow

      val generatedRelPath = testSources.inMemory.head.generatedRelPath
      val expectedRelPath  = os.rel / "BarTest.java"
      expect(testSources.paths.isEmpty)
      expect(testSources.inMemory.length == 1)
      expect(generatedRelPath == expectedRelPath)
    }
  }

  test("a markdown file ignores java snippets tagged as ignore") {
    val mdPath = os.rel / "Example.md"
    TestInputs(
      mdPath ->
        """# Example
          |
          |```java ignore
          |public class Ignored {
          |  public static void main(String[] args) {}
          |}
          |```
          |
          |```scala
          |println("Hello")
          |```""".stripMargin
    ).withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val mainSources =
        crossSources.scopedSources(BuildOptions()).orThrow
          .sources(Scope.Main, crossSources.sharedOptions(BuildOptions()), root, TestLogger())
          .orThrow

      expect(mainSources.inMemory.forall(!_.generatedRelPath.last.endsWith(".java")))
    }
  }

  /** Checks the file name generated for a Java snippet in a Markdown file.
    *
    * @param expectedClassNameOpt
    *   the class name expected to be inferred, or `None` for the default file name
    */
  private def javaSnippetFileNameTest(
    description: String,
    source: String,
    expectedClassNameOpt: Option[String]
  ): Unit =
    test(s"a markdown file with a Java snippet: $description") {
      TestInputs(
        os.rel / "Example.md" ->
          s"""# Example
             |
             |```java
             |$source
             |```""".stripMargin
      ).withInputs { (root, inputs) =>
        val (crossSources, _) =
          CrossSources.forInputs(
            inputs,
            preprocessors,
            TestLogger(),
            SuppressWarningOptions()
          ).orThrow

        val mainSources =
          crossSources.scopedSources(BuildOptions()).orThrow
            .sources(Scope.Main, crossSources.sharedOptions(BuildOptions()), root, TestLogger())
            .orThrow

        val expectedFileName = expectedClassNameOpt.getOrElse("Example_md_snippet0")
        expect(mainSources.inMemory.map(_.generatedRelPath) ==
          Seq(os.rel / s"$expectedFileName.java"))
      }
    }

  // https://github.com/VirtusLab/scala-cli/issues/4514
  javaSnippetFileNameTest(
    description = "public enum",
    source = "public enum Color { RED, GREEN }",
    expectedClassNameOpt = Some("Color")
  )
  javaSnippetFileNameTest(
    description = "public enum with methods",
    source =
      """public enum Color {
        |  RED, GREEN;
        |  public static void main(String[] args) { System.out.println(RED); }
        |}""".stripMargin,
    expectedClassNameOpt = Some("Color")
  )
  // https://github.com/VirtusLab/scala-cli/issues/4516
  javaSnippetFileNameTest(
    description = "public record with primitive components",
    source =
      """public record Point(int x, int y) {
        |  public static void main(String[] args) { System.out.println(new Point(1, 2)); }
        |}""".stripMargin,
    expectedClassNameOpt = Some("Point")
  )
  // https://github.com/VirtusLab/scala-cli/issues/4515
  javaSnippetFileNameTest(
    description = "package-private class in a package",
    source =
      """package demo;
        |class Main { public static void main(String[] args) { System.out.println("Hello"); } }""".stripMargin,
    expectedClassNameOpt = None
  )
  javaSnippetFileNameTest(
    description = "public class after a package-private one in a package",
    source =
      """package demo;
        |class Helper { static String greet() { return "Hello"; } }
        |public class Main { public static void main(String[] args) { System.out.println(Helper.greet()); } }""".stripMargin,
    expectedClassNameOpt = Some("Main")
  )
  // JEP 512 compact source files: the implicit class is named after the source file
  javaSnippetFileNameTest(
    description = "compact source file",
    source = """void main() { System.out.println("Hello"); }""",
    expectedClassNameOpt = None
  )
  javaSnippetFileNameTest(
    description = "compact source file with a public class before main",
    source =
      """public class Helper { static String greet() { return "Hello"; } }
        |void main() { System.out.println(Helper.greet()); }""".stripMargin,
    expectedClassNameOpt = None
  )
}
