package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.Cache.Fetch
import coursier.cache.{ArchiveCache, ArtifactError, Cache}
import coursier.util.{Artifact, EitherT, Task}

import java.io.File
import java.nio.charset.StandardCharsets

import scala.build.Ops.*
import scala.build.input.{ScalaCliInvokeData, VirtualJavaFile}
import scala.build.options.SuppressWarningOptions
import scala.build.preprocessing.{JavaPreprocessor, PreprocessedSource}
import scala.concurrent.ExecutionContext

class JavaPreprocessorTests extends TestUtil.ScalaCliBuildSuite {
  private val javaPreprocessor = JavaPreprocessor(
    archiveCache = ArchiveCache().withCache(
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

  private def generatedRelPath(virtualJavaFile: VirtualJavaFile): os.RelPath =
    javaPreprocessor.preprocess(
      virtualJavaFile,
      TestLogger(),
      allowRestrictedFeatures = false,
      suppressWarningOptions = SuppressWarningOptions()
    )(using ScalaCliInvokeData.dummy).get.orThrow match {
      case Seq(inMemory: PreprocessedSource.InMemory) => inMemory.relPath
      case other => fail(s"Expected a single in-memory source, got $other")
    }

  /** Checks the file name generated for a Java source passed via stdin and as a snippet.
    *
    * @param expectedClassNameOpt
    *   the class name expected to be inferred, or `None` for the default file name
    */
  private def generatedFileNameTest(
    description: String,
    source: String,
    expectedClassNameOpt: Option[String]
  ): Unit =
    for (inputKind, virtualSource) <-
        Seq("stdin" -> "<stdin>-java-file", "snippet" -> "<snippet>-java-snippet")
    do
      test(s"$inputKind: $description") {
        val virtualJavaFile =
          VirtualJavaFile(source.getBytes(StandardCharsets.UTF_8), virtualSource)
        val expectedFileName = expectedClassNameOpt
          .map(_ + ".java")
          .getOrElse(virtualJavaFile.generatedSourceFileName)
        expect(generatedRelPath(virtualJavaFile) == os.rel / expectedFileName)
      }

  // https://github.com/VirtusLab/scala-cli/issues/4514
  generatedFileNameTest(
    description = "public enum",
    source = "public enum Color { RED, GREEN }",
    expectedClassNameOpt = Some("Color")
  )
  generatedFileNameTest(
    description = "public enum with methods",
    source =
      """public enum Color {
        |  RED, GREEN;
        |  public static void main(String[] args) { System.out.println(RED); }
        |}
        |""".stripMargin,
    expectedClassNameOpt = Some("Color")
  )
  // https://github.com/VirtusLab/scala-cli/issues/4516
  generatedFileNameTest(
    description = "public record with primitive components",
    source =
      """public record Point(int x, int y) {
        |  public static void main(String[] args) { System.out.println(new Point(1, 2)); }
        |}
        |""".stripMargin,
    expectedClassNameOpt = Some("Point")
  )
  // https://github.com/VirtusLab/scala-cli/issues/4515
  generatedFileNameTest(
    description = "package-private class in a package",
    source =
      """package demo;
        |class Main { public static void main(String[] args) { System.out.println("Hello"); } }
        |""".stripMargin,
    expectedClassNameOpt = None
  )
  generatedFileNameTest(
    description = "public class after a package-private one in a package",
    source =
      """package demo;
        |class Helper { static String greet() { return "Hello"; } }
        |public class Main { public static void main(String[] args) { System.out.println(Helper.greet()); } }
        |""".stripMargin,
    expectedClassNameOpt = Some("Main")
  )
  // JEP 512 compact source files: the implicit class is named after the source file
  generatedFileNameTest(
    description = "compact source file",
    source = """void main() { System.out.println("Hello"); }""",
    expectedClassNameOpt = None
  )
  generatedFileNameTest(
    description = "compact source file with a public class before main",
    source =
      """public class Helper { static String greet() { return "Hello"; } }
        |void main() { System.out.println(Helper.greet()); }
        |""".stripMargin,
    expectedClassNameOpt = None
  )
}
