package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File
import java.util.regex.Pattern

import scala.cli.integration.util.BloopUtil

abstract class CompileTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {

  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

  private lazy val bloopDaemonDir = BloopUtil.bloopDaemonDir {
    os.proc(TestUtil.cli, "--power", "directories").call().out.text()
  }

  val simpleInputs: TestInputs = TestInputs(
    os.rel / "MyTests.scala" ->
      """//> using dep "com.lihaoyi::os-lib::0.8.1"
        |
        |object MyTests {
        |  def main(args: Array[String]): Unit = {
        |    for (l <- os.list(os.pwd))
        |      println(l.last)
        |  }
        |}
        |""".stripMargin
  )

  val mainAndTestInputs: TestInputs = TestInputs(
    os.rel / "Main.scala" ->
      """//> using dep "com.lihaoyi::utest:0.7.10"
        |
        |object Main {
        |  val err = utest.compileError("pprint.log(2)")
        |  def message = "Hello from " + "tests"
        |  def main(args: Array[String]): Unit = {
        |    println(message)
        |    println(err)
        |  }
        |}
        |""".stripMargin,
    os.rel / "Tests.test.scala" ->
      """//> using dep "com.lihaoyi::pprint:0.6.6"
        |
        |import utest._
        |
        |object Tests extends TestSuite {
        |  val tests = Tests {
        |    test("message") {
        |      assert(Main.message.startsWith("Hello"))
        |    }
        |  }
        |}
        |""".stripMargin
  )

  test(
    "java files with no using directives should not produce warnings about using directives in multiple files"
  ) {
    val inputs = TestInputs(
      os.rel / "Bar.java" ->
        """public class Bar {}
          |""".stripMargin,
      os.rel / "Foo.java" ->
        """public class Foo {}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val warningMessage = "Using directives detected in multiple files"
      val output = os.proc(TestUtil.cli, "compile", extraOptions, ".")
        .call(cwd = root, stderr = os.Pipe).err.trim()
      expect(!output.contains(warningMessage))
    }
  }

  test("with one file per scope, no warning about spread directives should be printed") {
    TestInputs(
      os.rel / "Bar.scala" ->
        """//> using dep "com.lihaoyi::os-lib:0.9.1"
          |
          |object Bar extends App {
          |  println(os.pwd)
          |}
          |""".stripMargin,
      os.rel / "Foo.test.scala" ->
        """//> using dep "org.scalameta::munit:0.7.29"
          |
          |class Foo extends munit.FunSuite {
          |  test("Hello") {
          |    assert(true)
          |  }
          |}
          |""".stripMargin
    ).fromRoot { root =>
      val warningMessage = "Using directives detected in multiple files"
      val output = os.proc(TestUtil.cli, "compile", ".", "--test", extraOptions)
        .call(cwd = root, stderr = os.Pipe).err.trim()
      expect(!output.contains(warningMessage))
    }
  }

  test("with >1 file per scope, the warning about spread directives should be printed") {
    TestInputs(
      os.rel / "Bar.scala" ->
        """//> using dep "com.lihaoyi::os-lib:0.9.1"
          |
          |object Bar extends App {
          |  pprint.pprintln(Foo(os.pwd.toString).value)
          |}
          |""".stripMargin,
      os.rel / "Foo.scala" ->
        """//> using dep "com.lihaoyi::pprint:0.8.1"
          |
          |case class Foo(value: String)
          |""".stripMargin,
      os.rel / "Foo.test.scala" ->
        """//> using dep "org.scalameta::munit:0.7.29"
          |
          |class FooTest extends munit.FunSuite {
          |  test("Hello") {
          |    assert(true)
          |  }
          |}
          |""".stripMargin
    ).fromRoot { root =>
      val warningMessage = "Using directives detected in multiple files"
      val output = os.proc(TestUtil.cli, "compile", ".", "--test", extraOptions)
        .call(cwd = root, stderr = os.Pipe).err.trim()
      expect(output.contains(warningMessage))
    }
  }

  test(
    "target directives in files should not produce warnings about using directives in multiple files"
  ) {
    val inputs = TestInputs(
      os.rel / "Bar.java" ->
        """//> using target.platform "jvm"
          |public class Bar {}
          |""".stripMargin,
      os.rel / "Foo.test.scala" ->
        """//> using target.scala.>= "2.13"
          |class Foo {}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val warningMessage = "Using directives detected in multiple files"
      val output = os.proc(TestUtil.cli, "--power", "compile", extraOptions, ".")
        .call(cwd = root).err.trim()
      expect(!output.contains(warningMessage))
    }
  }

  test(
    "warn about directives in multiple files"
  ) {
    val inputs = TestInputs(
      os.rel / "Bar.java" ->
        """//> using jvm "17"
          |//> using target.scope "test"
          |public class Bar {}
          |""".stripMargin,
      os.rel / "Foo.scala" ->
        """//> using target.scala.>= "2.13"
          |//> using dep "com.lihaoyi::os-lib::0.8.1"
          |class Foo {}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val warningMessage = "Using directives detected in multiple files"
      val output = os.proc(TestUtil.cli, "--power", "compile", extraOptions, ".")
        .call(cwd = root, stderr = os.Pipe).err.trim()
      expect(output.contains(warningMessage))
    }
  }

  test("no arg") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "compile", extraOptions, ".").call(cwd = root)
      val projDirs = os.list(root / Constants.workspaceDirName)
        .filter(_.last.startsWith("project_"))
        .filter(os.isDir(_))
      expect(projDirs.length == 1)
      val projDir     = projDirs.head
      val projDirName = projDir.last
      val elems       = projDirName.stripPrefix("project_").split("[-_]").toSeq
      expect(elems.length == 1)
    }
  }

  test("exit code") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit =
          |    println(nope)
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "compile", extraOptions, ".")
        .call(cwd = root, check = false, stderr = os.Pipe, mergeErrIntoOut = true)
      expect(res.exitCode == 1)
    }
  }

  def checkIfCompileOutputIsCopied(baseName: String, output: os.Path): Unit = {
    val extraExtensions = if (actualScalaVersion.startsWith("2.")) Nil else Seq(".tasty")
    val extensions      = Seq(".class", "$.class") ++ extraExtensions
    val foundFiles      = os.list(output).map(_.relativeTo(output))
    val expectedFiles   = extensions.map(ext => os.rel / s"$baseName$ext")
    expect(foundFiles.toSet == expectedFiles.toSet)
  }

  test("copy compile output") {
    mainAndTestInputs.fromRoot { root =>
      val tempOutput = root / "output"
      os.proc(TestUtil.cli, "compile", "--compile-output", tempOutput, extraOptions, ".").call(cwd =
        root
      )
      checkIfCompileOutputIsCopied("Main", tempOutput)
    }
  }

  test("test scope") {
    mainAndTestInputs.fromRoot { root =>
      val tempOutput = root / "output"
      val output =
        os.proc(
          TestUtil.cli,
          "compile",
          "--test",
          "--compile-output",
          tempOutput,
          "--print-class-path",
          extraOptions,
          "."
        ).call(cwd =
          root
        ).out.trim()
      val classPath = output.split(File.pathSeparator).map(_.trim).filter(_.nonEmpty)
      val isDefinedTestPathInClassPath = // expected test class path - root / Constants.workspaceDirName / project_(hash) / classes / test
        classPath.exists(p =>
          p.startsWith((root / Constants.workspaceDirName).toString()) &&
          p.endsWith(Seq("classes", "test").mkString(File.separator))
        )
      expect(isDefinedTestPathInClassPath)
      checkIfCompileOutputIsCopied("Tests", tempOutput)

      val projDirs = os.list(root / Constants.workspaceDirName)
        .filter(_.last.startsWith("project_"))
        .filter(os.isDir(_))
      expect(projDirs.length == 1)
      val projDir     = projDirs.head
      val projDirName = projDir.last
      val elems       = projDirName.stripPrefix("project_").split("[-_]").toSeq
      expect(elems.length == 2)
      expect(elems.toSet.size == 2)
    }
  }

  test("test scope error") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def message = "Hello from " + "tests"
          |  def main(args: Array[String]): Unit =
          |    println(message)
          |}
          |""".stripMargin,
      os.rel / "Tests.test.scala" ->
        """//> using dep "com.lihaoyi::utest:0.7.10"
          |
          |import utest._
          |
          |object Tests extends TestSuite {
          |  val tests = Tests {
          |    test("message") {
          |      pprint.log(Main.message)
          |      assert(Main.message.startsWith("Hello"))
          |    }
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "compile", "--test", extraOptions, ".")
        .call(cwd = root, check = false, stderr = os.Pipe, mergeErrIntoOut = true)
      expect(res.exitCode == 1)
      val expectedInOutput =
        if (actualScalaVersion.startsWith("2."))
          "not found: value pprint"
        else
          "Not found: pprint"
      expect(res.out.text().contains(expectedInOutput))
    }
  }

  test("code in test error") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def message = "Hello from " + "tests"
          |  def main(args: Array[String]): Unit = {
          |    zz // zz value
          |    println(message)
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "compile", extraOptions, ".")
        .call(cwd = root, check = false, stderr = os.Pipe, mergeErrIntoOut = true)
      expect(res.exitCode == 1)
      val expectedInOutput =
        if (actualScalaVersion.startsWith("2."))
          "not found: value zz"
        else
          "Not found: zz"
      val output = res.out.text()
      expect(output.contains(expectedInOutput))
      // errored line should be printed too
      expect(output.contains("zz // zz value"))
      if (actualScalaVersion.startsWith("2.12."))
        // seems the ranges returned by Bloop / scalac are only one character wide in 2.12
        expect(output.contains("^"))
      else
        // underline should have length 2
        expect(output.contains("^^"))
      expect(!output.contains("^^^"))
    }
  }

  val jvmT = new munit.Tag("jvm-resolution")

  val scalaJvm8Project: TestInputs =
    TestInputs(os.rel / "Main.scala" -> s"object Main{java.util.Optional.of(1).isPresent}")
  val scalaJvm11Project: TestInputs =
    TestInputs(os.rel / "Main.scala" -> s"object Main{java.util.Optional.of(1).isEmpty}")
  val javaJvm8Project: TestInputs =
    TestInputs(os.rel / "Main.java" -> """|public class Main{
                                          |  public static void main(String[] args) {
                                          |      java.util.Optional.of(1).isPresent();
                                          |  }
                                          |}""".stripMargin)

  val javaJvm11Project: TestInputs =
    TestInputs(os.rel / "Main.java" -> """|public class Main{
                                          |  public static void main(String[] args) {
                                          |      java.util.Optional.of(1).isEmpty();
                                          |  }
                                          |}""".stripMargin)

  val inputs: Map[(String, Int), TestInputs] = Map(
    ("scala", 8)  -> scalaJvm8Project,
    ("scala", 11) -> scalaJvm11Project,
    ("java", 8)   -> javaJvm8Project,
    ("java", 11)  -> javaJvm11Project
  )

  for {
    bloopJvm                      <- List(8, 11)
    targetJvm                     <- List(8, 11)
    ((lang, sourcesJvm), project) <- inputs
  } test(s"JvmCompatibilityTest: bloopJvm:$bloopJvm/targetJvm:$targetJvm/lang:$lang/sourcesJvm:$sourcesJvm"
    .tag(jvmT)) {
    compileToADifferentJvmThanBloops(
      bloopJvm.toString,
      targetJvm.toString,
      targetJvm >= sourcesJvm,
      project
    )
  }

  test("Scala CLI should not infer scalac --release if --release is passed".tag(jvmT)) {
    scalaJvm11Project.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "compile",
        extraOptions,
        "--jvm",
        "11",
        "-release",
        "8",
        "."
      ).call(cwd = root, check = false, stderr = os.Pipe)
      expect(res.exitCode != 0)
      val errOutput = res.err.trim()
      expect(errOutput.contains("isEmpty is not a member"))
      expect(errOutput.contains(
        "Warning: different target JVM (11) and scala compiler target JVM (8) were passed."
      ))
    }
  }

  if (actualScalaVersion.startsWith("2.1"))
    test("warn for different target JVMs in --jvm, -target:x and -release".tag(jvmT)) {
      scalaJvm8Project.fromRoot { root =>
        val res = os.proc(
          TestUtil.cli,
          "compile",
          extraOptions,
          "--jvm",
          "11",
          "-release",
          "8",
          "-target:8",
          "."
        ).call(cwd = root, check = false, stderr = os.Pipe)
        expect(res.exitCode == 0)
        val errOutput = res.err.trim()
        expect(errOutput.contains(
          "Warning: different target JVM (11) and scala compiler target JVM (8) were passed."
        ))
      }
    }

  def compileToADifferentJvmThanBloops(
    bloopJvm: String,
    targetJvm: String,
    shouldSucceed: Boolean,
    inputs: TestInputs
  ): Unit =
    inputs.fromRoot { root =>
      val bloop = BloopUtil.bloop(Constants.bloopVersion, bloopDaemonDir, jvm = Some(bloopJvm))
      bloop(Seq("exit")).call(
        cwd = root,
        check = false,
        stdout = os.Inherit
      )
      bloop(Seq("about")).call(
        cwd = root,
        check = false,
        stdout = os.Inherit
      )
      val res = os.proc(TestUtil.cli, "compile", extraOptions, "--jvm", targetJvm, ".")
        .call(cwd = root, check = false, stderr = os.Pipe)
      expect((res.exitCode == 0) == shouldSucceed)
      if (!shouldSucceed)
        expect(
          res.err.text().contains("value isEmpty is not a member") || res.err.text().contains(
            "cannot find symbol"
          )
        )
    }
  if (actualScalaVersion.startsWith("2.12"))
    test("JVM options only for JVM platform") {
      val inputs = TestInputs(os.rel / "Main.scala" -> "//> using `java-opt` \"-Xss1g\"")
      inputs.fromRoot { root =>
        val res = os.proc(TestUtil.cli, "compile", extraOptions, "--native", ".").call(
          cwd = root,
          stderr = os.Pipe
        )
        val stderr = res.err.text()
        expect(s"\\[.*warn.*].*Conflicting options.*".r.findFirstMatchIn(stderr).isDefined)

      }
    }

  test("Manual javac SemanticDB") {
    val inputs = TestInputs(
      os.rel / "foo" / "Test.java" ->
        """package foo;
          |
          |public class Test {
          |  public static void main(String[] args) {
          |    System.err.println("Hello");
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val compilerPackages = Seq(
        "com.sun.tools.javac.api",
        "com.sun.tools.javac.code",
        "com.sun.tools.javac.model",
        "com.sun.tools.javac.tree",
        "com.sun.tools.javac.util"
      )
      val exports = compilerPackages
        .flatMap { pkg =>
          Seq("-J--add-exports", s"-Jjdk.compiler/$pkg=ALL-UNNAMED")
        }
        .flatMap(opt => List("--javac-opt", opt))
      val javaSemDbOptions = Seq(
        "--javac-plugin",
        "com.sourcegraph:semanticdb-javac:0.7.4",
        "--javac-opt",
        s"-Xplugin:semanticdb -sourceroot:$root -targetroot:javac-classes-directory"
      ) ++ exports
      os.proc(TestUtil.cli, "compile", extraOptions, javaSemDbOptions, ".")
        .call(cwd = root)

      val files = os.walk(root / Constants.workspaceDirName)
      val semDbFiles = files
        .filter(_.last.endsWith(".semanticdb"))
        .filter(!_.segments.exists(_ == "bloop-internal-classes"))
      expect(semDbFiles.length == 1)
      val semDbFile = semDbFiles.head
      expect(
        semDbFile.endsWith(os.rel / "META-INF" / "semanticdb" / "foo" / "Test.java.semanticdb")
      )
    }
  }

  test("Javac SemanticDB") {
    val inputs = TestInputs(
      os.rel / "foo" / "Test.java" ->
        """package foo;
          |
          |public class Test {
          |  public static void main(String[] args) {
          |    System.err.println("Hello");
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "compile", extraOptions, "--semantic-db", ".")
        .call(cwd = root)

      val files = os.walk(root / Constants.workspaceDirName)
      val semDbFiles = files
        .filter(_.last.endsWith(".semanticdb"))
        .filter(!_.segments.exists(_ == "bloop-internal-classes"))
      expect(semDbFiles.length == 1)
      val semDbFile = semDbFiles.head
      expect(
        semDbFile.endsWith(os.rel / "META-INF" / "semanticdb" / "foo" / "Test.java.semanticdb")
      )
    }
  }

  if (actualScalaVersion.startsWith("3"))
    test("generate scoverage.coverage file") {
      val fileName = "Hello.scala"
      val inputs = TestInputs(
        os.rel / fileName ->
          s"""//> using options "-coverage-out:."
             |
             |@main def main = ()
             |""".stripMargin
      )
      inputs.fromRoot { root =>
        os.proc(TestUtil.cli, "compile", extraOptions, fileName)
          .call(cwd = root)
          .out.trim()

        val expectedCoverageFilePath = root / "scoverage.coverage"
        expect(os.exists(expectedCoverageFilePath))
      }
    }

  if (actualScalaVersion.startsWith("2."))
    test("no duplicates in class path") {
      noDuplicatesInClassPathTest()
    }
  def noDuplicatesInClassPathTest(): Unit = {
    val sparkVersion = "3.3.0"
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using dep "org.apache.spark::spark-sql:$sparkVersion"
           |object Hello {
           |  def main(args: Array[String]): Unit =
           |    println("Hello")
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val res =
        os.proc(
          TestUtil.cli,
          "compile",
          "--print-class-path",
          extraOptions,
          "."
        ).call(cwd = root)
      val classPath          = res.out.trim().split(File.pathSeparator)
      val classPathFileNames = classPath.map(_.split(Pattern.quote(File.separator)).last)
      expect(classPathFileNames.exists(_.startsWith("spark-core_")))
      // usually a duplicate is there if we don't call .distrinct when necessary here or there
      expect(classPathFileNames.exists(_.startsWith("snappy-java")))
      val duplicates =
        classPath.groupBy(identity).view.mapValues(_.length).filter(_._2 > 1).toVector
      expect(duplicates.isEmpty)
    }
  }

  test("override settings from tests") {
    val inputs = TestInputs(
      os.rel / "MainStuff.scala" ->
        """//> using jvm "8"
          |object MainStuff {
          |  def javaVer = sys.props("java.version")
          |  def main(args: Array[String]): Unit = {
          |    println(s"Found Java $javaVer in main scope")
          |    assert(javaVer.startsWith("1.8."))
          |  }
          |}
          |""".stripMargin,
      os.rel / "TestStuff.test.scala" ->
        """//> using jvm "17"
          |//> using dep "org.scalameta::munit:0.7.29"
          |class TestStuff extends munit.FunSuite {
          |  test("the test") {
          |    val javaVer = MainStuff.javaVer
          |    println(s"Found Java $javaVer in test scope")
          |    val javaVer0 = {
          |      val bais = new java.io.ByteArrayInputStream(javaVer.getBytes("UTF-8"))
          |      new String(bais.readAllBytes(), "UTF-8") // readAllBytes available only on Java 17 (not on Java 8)
          |    }
          |    assert(javaVer0 == "17" || javaVer0.startsWith("17."))
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "compile", "--test", ".")
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)
      os.proc(TestUtil.cli, "run", ".")
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)
      os.proc(TestUtil.cli, "test", ".")
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)
    }
  }

  test("scalapy") {

    def maybeScalapyPrefix =
      if (actualScalaVersion.startsWith("2.13.")) ""
      else "import me.shadaj.scalapy.py" + System.lineSeparator()

    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""$maybeScalapyPrefix
           |object Hello {
           |  def main(args: Array[String]): Unit = {
           |    py.Dynamic.global.print("Hello from Python", flush = true)
           |  }
           |}
           |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res =
        os.proc(
          TestUtil.cli,
          "--power",
          "compile",
          "--python",
          "--print-class-path",
          ".",
          extraOptions
        )
          .call(cwd = root)
      val classPath = res.out.trim().split(File.pathSeparator)
      val outputDir = os.Path(classPath.head, root)
      val classFiles = os.walk(outputDir)
        .filter(_.last.endsWith(".class"))
        .filter(os.isFile(_))
        .map(_.relativeTo(outputDir))
      expect(classFiles.contains(os.rel / "Hello.class"))
    }
  }

  private def compilerArtifactName: String =
    if (actualScalaVersion.startsWith("3")) "scala3-compiler" else "scala-compiler"

  test(s"ensure the -with-compiler option adds $compilerArtifactName to the classpath") {
    TestInputs(os.rel / "s.sc" -> """println("Hello")""")
      .fromRoot { root =>
        val compileRes = os.proc(
          TestUtil.cli,
          "compile",
          "s.sc",
          "--print-classpath",
          extraOptions
        )
          .call(cwd = root)
        expect(!compileRes.out.trim().contains(compilerArtifactName))
        val compileWithCompilerRes = os.proc(
          TestUtil.cli,
          "compile",
          "s.sc",
          "-with-compiler",
          "--print-classpath",
          extraOptions
        )
          .call(cwd = root)
        expect(compileWithCompilerRes.out.trim().contains(compilerArtifactName))
      }
  }
}
