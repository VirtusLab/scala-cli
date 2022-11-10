package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

object ExportTestProjects {

  def jvmTest(scalaVersion: String): TestInputs = {

    val mainFile =
      if (scalaVersion.startsWith("3."))
        s"""//> using scala "$scalaVersion"
           |//> using resourceDir "./input"
           |//> using lib "org.scala-lang::scala3-compiler:$scalaVersion"
           |//> using option "-deprecation"
           |
           |import scala.io.Source
           |
           |object Hello {
           |  def main(args: Array[String]): Unit = {
           |    val message = "Hello from " + dotty.tools.dotc.config.Properties.simpleVersionString
           |    println(message)
           |    val inputs = Source.fromResource("input").getLines.map(_.toInt).toSeq
           |    println(s"resource:$${inputs.mkString(",")}")
           |  }
           |}
           |""".stripMargin
      else
        s"""//> using scala "$scalaVersion"
           |//> using resourceDir "./input"
           |//> using option "-deprecation"
           |
           |import scala.io.Source
           |
           |object Hello {
           |  def main(args: Array[String]): Unit = {
           |    val message = "Hello from " + scala.util.Properties.versionNumberString
           |    println(message)
           |    val inputs = Source.fromResource("input").getLines.map(_.toInt).toSeq
           |    println(s"resource:$${inputs.mkString(",")}")
           |  }
           |}
           |""".stripMargin
    TestInputs(
      os.rel / "Hello.scala" -> mainFile,
      os.rel / "Zio.test.scala" ->
        """|//> using lib "dev.zio::zio::1.0.8"
           |//> using lib "dev.zio::zio-test-sbt::1.0.8"
           |
           |import zio._
           |import zio.test._
           |import zio.test.Assertion.equalTo
           |
           |object HelloWorldSpec extends DefaultRunnableSpec {
           |  def spec = suite("associativity")(
           |    testM("associativity") {
           |      check(Gen.anyInt, Gen.anyInt, Gen.anyInt) { (x, y, z) =>
           |        assert((x + y) + z)(equalTo(x + (y + z)))
           |      }
           |    }
           |  )
           |}
           |""".stripMargin,
      os.rel / "input" / "input" ->
        """|1
           |2""".stripMargin
    )
  }

  def jsTest(scalaVersion: String): TestInputs = {

    val testFile =
      if (scalaVersion.startsWith("3."))
        s"""//> using scala "$scalaVersion"
           |//> using platform "scala-js"
           |
           |import scala.scalajs.js
           |
           |object Test:
           |  def main(args: Array[String]): Unit =
           |    val console = js.Dynamic.global.console
           |    console.log("Hello from " + "exported Scala CLI project")
           |""".stripMargin
      else
        s"""//> using scala "$scalaVersion"
           |//> using platform "scala-js"
           |
           |import scala.scalajs.js
           |
           |object Test {
           |  def main(args: Array[String]): Unit = {
           |    val console = js.Dynamic.global.console
           |    console.log("Hello from " + "exported Scala CLI project")
           |  }
           |}
           |""".stripMargin
    TestInputs(os.rel / "Test.scala" -> testFile)
  }

  def nativeTest(scalaVersion: String): TestInputs = {
    val nl = "\\n"
    val testFile =
      if (scalaVersion.startsWith("3."))
        s"""//> using scala "$scalaVersion"
           |//> using platform "scala-native"
           |
           |import scala.scalanative.libc._
           |import scala.scalanative.unsafe._
           |
           |object Test:
           |  def main(args: Array[String]): Unit =
           |    val message = "Hello from " + "exported Scala CLI project" + "$nl"
           |    Zone { implicit z =>
           |      stdio.printf(toCString(message))
           |    }
           |""".stripMargin
      else
        s"""//> using scala "$scalaVersion"
           |//> using platform "scala-native"
           |
           |import scala.scalanative.libc._
           |import scala.scalanative.unsafe._
           |
           |object Test {
           |  def main(args: Array[String]): Unit = {
           |    val message = "Hello from " + "exported Scala CLI project" + "$nl"
           |    Zone { implicit z =>
           |      stdio.printf(toCString(message))
           |    }
           |  }
           |}
           |""".stripMargin
    TestInputs(os.rel / "Test.scala" -> testFile)
  }

  def repositoryScala3Test(scalaVersion: String): TestInputs = {
    val testFile =
      s"""//> using scala "$scalaVersion"
         |//> using lib "com.github.jupyter:jvm-repr:0.4.0"
         |//> using repository "jitpack"
         |import jupyter._
         |object Test:
         |  def main(args: Array[String]): Unit =
         |    val message = "Hello from " + "exported Scala CLI project"
         |    println(message)
         |""".stripMargin
    TestInputs(os.rel / "Test.scala" -> testFile)
  }

  def mainClassScala3Test(scalaVersion: String): TestInputs = {
    val testFile =
      s"""//> using scala "$scalaVersion"
         |
         |object Test:
         |  def main(args: Array[String]): Unit =
         |    val message = "Hello from " + "exported Scala CLI project"
         |    println(message)
         |""".stripMargin
    val otherTestFile =
      s"""object Other:
         |  def main(args: Array[String]): Unit =
         |    val message = "Hello from " + "other file"
         |    println(message)
         |""".stripMargin
    TestInputs(
      os.rel / "Test.scala"  -> testFile,
      os.rel / "Other.scala" -> otherTestFile
    )
  }

  def scalacOptionsScala2Test(scalaVersion: String): TestInputs = {
    val testFile =
      s"""//> using scala "$scalaVersion"
         |//> using lib "org.scala-lang.modules::scala-async:0.10.0"
         |//> using lib "org.scala-lang:scala-reflect:$scalaVersion"
         |import scala.async.Async.{async, await}
         |import scala.concurrent.{Await, Future}
         |import scala.concurrent.duration.Duration
         |import scala.concurrent.ExecutionContext.Implicits.global
         |
         |object Test {
         |  def main(args: Array[String]): Unit = {
         |    val messageF = Future.successful(
         |      "Hello from " + "exported Scala CLI project"
         |    )
         |    val f = async {
         |      val message = await(messageF)
         |      println(message)
         |    }
         |    Await.result(f, Duration.Inf)
         |  }
         |}
         |""".stripMargin
    TestInputs(os.rel / "Test.scala" -> testFile)
  }

  def pureJavaTest: TestInputs = {
    val testFile =
      s"""public class ScalaCliJavaTest {
         |  public static void main(String[] args) {
         |    String className = "scala.concurrent.ExecutionContext";
         |    ClassLoader cl = Thread.currentThread().getContextClassLoader();
         |    boolean found = true;
         |    try {
         |      cl.loadClass(className);
         |    } catch (ClassNotFoundException ex) {
         |      found = false;
         |    }
         |    if (found) {
         |      throw new RuntimeException("Didn't expect " + className + " to be in class path.");
         |    }
         |    System.out.println("Hello from " + "exported Scala CLI project");
         |  }
         |}
         |""".stripMargin
    TestInputs(os.rel / "ScalaCliJavaTest.java" -> testFile)
  }

  def testFrameworkTest(scalaVersion: String): TestInputs = {
    val testFile =
      s"""//> using scala "$scalaVersion"
         |//> using lib "com.lihaoyi::utest:0.7.10"
         |//> using test-framework "utest.runner.Framework"
         |
         |import utest._
         |
         |object MyTests extends TestSuite {
         |  val tests = Tests {
         |    test("foo") {
         |      assert(2 + 2 == 4)
         |      println("Hello from " + "exported Scala CLI project")
         |    }
         |  }
         |}
         |""".stripMargin
    TestInputs(os.rel / "MyTests.scala" -> testFile)
  }

  def customJarTest(scalaVersion: String): TestInputs = {
    val shapelessJar = {
      val res = os.proc(
        TestUtil.cs,
        "fetch",
        "--intransitive",
        "com.chuusai::shapeless:2.3.9",
        "--scala",
        scalaVersion
      )
        .call()
      val path  = res.out.trim()
      val path0 = os.Path(path, os.pwd)
      expect(os.isFile(path0))
      path0
    }
    val shapelessJarStr =
      "\"" + shapelessJar.toString.replace("\\", "\\\\") + "\""
    val testFile =
      s"""//> using scala "$scalaVersion"
         |//> using jar $shapelessJarStr
         |
         |import shapeless._
         |
         |object Test {
         |  def main(args: Array[String]): Unit = {
         |    val l = "exported Scala CLI project" :: 2 :: true :: HNil
         |    val messageEnd: String = l.head
         |    println("Hello from " + messageEnd)
         |  }
         |}
         |""".stripMargin
    TestInputs(os.rel / "Test.scala" -> testFile)
  }
}
