package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.ArchiveCache

import java.nio.charset.StandardCharsets

import scala.build.Ops.*
import scala.build.internal.{JavaParserProxy, JavaParserProxyBinary, JavaParserProxyJvm}

class JavaParserProxyTests extends TestUtil.ScalaCliBuildSuite {
  // the binary proxy is what native launchers use, it downloads and runs a java-class-name launcher
  private lazy val binaryProxy =
    JavaParserProxyBinary(ArchiveCache(), None, TestLogger(), () => "java")

  private val proxies: Seq[(String, () => JavaParserProxy)] =
    Seq("JVM" -> (() => new JavaParserProxyJvm), "binary" -> (() => binaryProxy))

  /** Checks the class name inferred by both proxies.
    *
    * @param expectedClassNameOpt
    *   the class name expected to be inferred, or `None` when the source file name should be used
    */
  private def classNameTest(
    description: String,
    source: String,
    expectedClassNameOpt: Option[String]
  ): Unit =
    for (proxyName, proxy) <- proxies do
      test(s"$proxyName: $description") {
        val fallbackClassName = "stdin"
        val classNameOpt      = proxy()
          .className(source.getBytes(StandardCharsets.UTF_8), s"$fallbackClassName.java")
          .orThrow
        expect(
          classNameOpt.getOrElse(fallbackClassName) ==
            expectedClassNameOpt.getOrElse(fallbackClassName)
        )
      }

  // https://github.com/VirtusLab/scala-cli/issues/4514
  classNameTest(
    description = "public enum",
    source = "public enum Color { RED, GREEN }",
    expectedClassNameOpt = Some("Color")
  )
  classNameTest(
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
  classNameTest(
    description = "public record with primitive components",
    source =
      """public record Point(int x, int y) {
        |  public static void main(String[] args) { System.out.println(new Point(1, 2)); }
        |}
        |""".stripMargin,
    expectedClassNameOpt = Some("Point")
  )
  // https://github.com/VirtusLab/scala-cli/issues/4515
  classNameTest(
    description = "package-private class in a package",
    source =
      """package demo;
        |class Main { public static void main(String[] args) { System.out.println("Hello"); } }
        |""".stripMargin,
    expectedClassNameOpt = None
  )
  classNameTest(
    description = "public class after a package-private one in a package",
    source =
      """package demo;
        |class Helper { static String greet() { return "Hello"; } }
        |public class Main { public static void main(String[] args) { System.out.println(Helper.greet()); } }
        |""".stripMargin,
    expectedClassNameOpt = Some("Main")
  )
  // JEP 512 compact source files: the implicit class is named after the source file
  classNameTest(
    description = "compact source file",
    source = """void main() { System.out.println("Hello"); }""",
    expectedClassNameOpt = None
  )
  classNameTest(
    description = "compact source file with a public class before main",
    source =
      """public class Helper { static String greet() { return "Hello"; } }
        |void main() { System.out.println(Helper.greet()); }
        |""".stripMargin,
    expectedClassNameOpt = None
  )
}
