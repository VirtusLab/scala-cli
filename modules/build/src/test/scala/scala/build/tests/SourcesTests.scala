package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import dependency._

import scala.build.Sources
import scala.build.internal.CustomCodeWrapper
import scala.build.internal.Util._
import scala.build.tests.TestUtil._

class SourcesTests extends munit.FunSuite {

  def scalaVersion       = "2.13.5"
  def scalaParams        = ScalaParameters(scalaVersion)
  def scalaBinaryVersion = scalaParams.scalaBinaryVersion

  test("dependencies in .scala") {
    val testInputs = TestInputs(
      os.rel / "something.scala" ->
        """import $ivy.`org1:name1:1.1`
          |import $ivy.`org2::name2:2.2`
          |import $ivy.`org3:::name3:3.3`
          |import scala.collection.mutable
          |
          |object Something {
          |  def a = 1
          |}
          |""".stripMargin
    )
    val expectedDeps = Seq(
      dep"org1:name1:1.1",
      dep"org2::name2:2.2",
      dep"org3:::name3:3.3"
    )
    testInputs.withInputs { (_, inputs) =>
      val sources = Sources.forInputs(inputs, Sources.defaultPreprocessors(CustomCodeWrapper))

      expect(sources.buildOptions.classPathOptions.extraDependencies == expectedDeps)
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.length == 1)
      expect(sources.inMemory.map(_._2) == Seq(os.rel / "something.scala"))
    }
  }

  test("dependencies in .sc") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """import $ivy.`org1:name1:1.1`
          |import $ivy.`org2::name2:2.2`
          |import $ivy.`org3:::name3:3.3`
          |import scala.collection.mutable
          |
          |def a = 1
          |""".stripMargin
    )
    val expectedDeps = Seq(
      dep"org1:name1:1.1",
      dep"org2::name2:2.2",
      dep"org3:::name3:3.3"
    )
    testInputs.withInputs { (_, inputs) =>
      val sources = Sources.forInputs(inputs, Sources.defaultPreprocessors(CustomCodeWrapper))

      expect(sources.buildOptions.classPathOptions.extraDependencies == expectedDeps)
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.length == 1)
      expect(sources.inMemory.map(_._2) == Seq(os.rel / "something.scala"))
    }
  }

  test("should skip SheBang in .sc") {
    val testInputs = TestInputs(
      os.rel / "something1.sc" ->
        """#!/usr/bin/env scala-cli
          |
          |println("Hello World")
          |""".stripMargin,
      os.rel / "something2.sc" ->
        """#!/usr/bin/scala-cli
          |
          |println("Hello World")
          |""".stripMargin,
      os.rel / "something3.sc" ->
        """#!/usr/bin/scala-cli
          |#! nix-shell -i scala-cli
          |
          |println("Hello World")
          |""".stripMargin,
      os.rel / "something4.sc" ->
        """#!/usr/bin/scala-cli
          |#! nix-shell -i scala-cli
          |
          |!#
          |
          |println("Hello World")
          |""".stripMargin
    )
    val expectedParsedCodes = Seq(
      """
        |println("Hello World")""".stripMargin,
      """
        |println("Hello World")""".stripMargin,
      """
        |
        |
        |println("Hello World")""".stripMargin,
      """
        |
        |
        |
        |println("Hello World")""".stripMargin
    )

    testInputs.withInputs { (_, inputs) =>
      val sources = Sources.forInputs(inputs, Sources.defaultPreprocessors(CustomCodeWrapper))

      val parsedCodes: Seq[String] = sources.inMemory.map(_._3)

      parsedCodes.zip(expectedParsedCodes).foreach { case (parsedCode, expectedCode) =>
        expect(parsedCode.contains(expectedCode))
      }
    }
  }

}
