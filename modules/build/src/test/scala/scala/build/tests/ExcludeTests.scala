package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.{ArchiveCache, Cache}
import coursier.util.{Artifact, Task}

import java.io.File
import scala.build.Ops.*
import scala.build.Sources
import scala.build.internal.CustomCodeWrapper
import scala.build.CrossSources
import scala.build.errors.ExcludeDefinitionError
import scala.build.options.{BuildOptions, Scope, SuppressWarningOptions}

class ExcludeTests extends munit.FunSuite {

  val preprocessors = Sources.defaultPreprocessors(
    CustomCodeWrapper,
    ArchiveCache().withCache(
      new Cache[Task] {
        def fetch = _ => sys.error("shouldn't be used")

        def file(artifact: Artifact) = sys.error("shouldn't be used")

        def ec = sys.error("shouldn't be used")
      }
    ),
    None,
    () => sys.error("shouldn't be used")
  )

  test("throw error when exclude found in multiple files") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala" ->
        """//> using exclude "*.sc"
          |""".stripMargin,
      os.rel / "Main.scala" ->
        """//> using exclude "*/test/*"
          |""".stripMargin
    )
    testInputs.withInputs { (_, inputs) =>
      val crossSources =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )
      crossSources match {
        case Left(_: ExcludeDefinitionError) =>
        case o                               => fail("Exception expected", clues(o))
      }
    }
  }

  test("throw error when exclude found in non top-level project.scala and file") {
    val testInputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using exclude "*/test/*"
          |""".stripMargin,
      os.rel / "src" / "project.scala" ->
        s"""//> using exclude "*.sc" """
    )
    testInputs.withInputs { (_, inputs) =>
      val crossSources =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )
      crossSources match {
        case Left(_: ExcludeDefinitionError) =>
        case o                               => fail("Exception expected", clues(o))
      }
    }
  }

  test("exclude relative paths") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala" -> "object Hello",
      os.rel / "Main.scala" ->
        """object Main {
          |}""".stripMargin,
      os.rel / "project.scala" ->
        s"""//> using exclude "Main.scala" """
    )
    testInputs.withInputs { (_, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(sources.paths.nonEmpty)
      expect(sources.paths.length == 2)
      expect(sources.paths.map(_._2) == Seq(os.rel / "Hello.scala", os.rel / "project.scala"))
    }
  }

  test("exclude absolute file paths") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala" -> "object Hello",
      os.rel / "Main.scala" ->
        """object Main {
          |}""".stripMargin,
      os.rel / "project.scala" ->
        s"""//> using exclude "$${.}${File.separator}Main.scala" """
    )
    testInputs.withInputs { (_, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(sources.paths.nonEmpty)
      expect(sources.paths.length == 2)
      expect(sources.paths.map(_._2) == Seq(os.rel / "Hello.scala", os.rel / "project.scala"))
    }
  }

  test("exclude relative directory paths") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala" -> "object Hello",
      os.rel / "src" / "scala" / "Main.scala" ->
        """object Main {
          |}""".stripMargin,
      os.rel / "project.scala" ->
        """//> using exclude "src/*.scala" """
    )
    testInputs.withInputs { (_, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(sources.paths.nonEmpty)
      expect(sources.paths.length == 2)
      expect(sources.paths.map(_._2) == Seq(os.rel / "Hello.scala", os.rel / "project.scala"))
    }
  }

  test("exclude relative directory paths with glob pattern") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala" -> "object Hello",
      os.rel / "src" / "scala" / "Main.scala" ->
        """object Main {
          |}""".stripMargin,
      os.rel / "project.scala" ->
        """//> using exclude "src/*.scala" """
    )
    testInputs.withInputs { (_, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(sources.paths.nonEmpty)
      expect(sources.paths.length == 2)
      expect(sources.paths.map(_._2) == Seq(os.rel / "Hello.scala", os.rel / "project.scala"))
    }
  }

}
