package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.Cache.Fetch
import coursier.cache.{ArchiveCache, ArtifactError, Cache}
import coursier.util.{Artifact, EitherT, Task}

import java.io.File

import scala.build.Ops.*
import scala.build.errors.ExcludeDefinitionError
import scala.build.input.ScalaCliInvokeData
import scala.build.options.{BuildOptions, Scope, SuppressWarningOptions}
import scala.build.preprocessing.Preprocessor
import scala.build.{CrossSources, Sources}
import scala.concurrent.ExecutionContext

class ExcludeTests extends TestUtil.ScalaCliBuildSuite {
  val preprocessors: Seq[Preprocessor] = Sources.defaultPreprocessors(
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

  test("throw error when exclude found in multiple files") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala" ->
        """//> using exclude *.sc
          |""".stripMargin,
      os.rel / "Main.scala" ->
        """//> using exclude */test/*
          |""".stripMargin
    )
    testInputs.withInputs { (_, inputs) =>
      val crossSources =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy)
      crossSources match {
        case Left(_: ExcludeDefinitionError) =>
        case o                               => fail("Exception expected", clues(o))
      }
    }
  }

  test("throw error when exclude found in non top-level project.scala and file") {
    val testInputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using exclude */test/*
          |""".stripMargin,
      os.rel / "src" / "project.scala" ->
        s"""//> using exclude *.sc"""
    )
    testInputs.withInputs { (_, inputs) =>
      val crossSources =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy)
      crossSources match {
        case Left(_: ExcludeDefinitionError) =>
        case o                               => fail("Exception expected", clues(o))
      }
    }
  }

  test("multiple excludes") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala"   -> "object Hello",
      os.rel / "World.scala"   -> "object World",
      os.rel / "Main.scala"    -> "object Main",
      os.rel / "project.scala" -> s"""//> using exclude Hello.scala World.scala"""
    )
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions())
        .orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

      expect(sources.paths.nonEmpty)
      expect(sources.paths.length == 2)
      val paths = Seq(os.rel / "Main.scala", os.rel / "project.scala")
      expect(sources.paths.map(_._2) == paths)
    }
  }

  test("exclude relative paths") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala" -> "object Hello",
      os.rel / "Main.scala"  ->
        """object Main {
          |}""".stripMargin,
      os.rel / "project.scala" ->
        s"""//> using exclude Main.scala"""
    )
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions())
        .orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

      expect(sources.paths.nonEmpty)
      expect(sources.paths.length == 2)
      val paths = Seq(os.rel / "Hello.scala", os.rel / "project.scala")
      expect(sources.paths.map(_._2) == paths)
    }
  }

  test("exclude absolute file paths") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala" -> "object Hello",
      os.rel / "Main.scala"  ->
        """object Main {
          |}""".stripMargin,
      os.rel / "project.scala" ->
        s"""//> using exclude $${.}${File.separator}Main.scala"""
    )
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions())
        .orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

      expect(sources.paths.nonEmpty)
      expect(sources.paths.length == 2)
      val paths = Seq(os.rel / "Hello.scala", os.rel / "project.scala")
      expect(sources.paths.map(_._2) == paths)
    }
  }

  test("exclude relative directory paths") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala"                  -> "object Hello",
      os.rel / "src" / "scala" / "Main.scala" ->
        """object Main {
          |}""".stripMargin,
      os.rel / "project.scala" ->
        """//> using exclude src/*.scala"""
    )
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions())
        .orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

      expect(sources.paths.nonEmpty)
      expect(sources.paths.length == 2)
      val paths = Seq(os.rel / "Hello.scala", os.rel / "project.scala")
      expect(sources.paths.map(_._2) == paths)
    }
  }

  test("exclude relative directory paths with glob pattern") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala"                  -> "object Hello",
      os.rel / "src" / "scala" / "Main.scala" ->
        """object Main {
          |}""".stripMargin,
      os.rel / "project.scala" ->
        """//> using exclude src/*.scala"""
    )
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions())
        .orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

      expect(sources.paths.nonEmpty)
      expect(sources.paths.length == 2)
      val paths = Seq(os.rel / "Hello.scala", os.rel / "project.scala")
      expect(sources.paths.map(_._2) == paths)
    }
  }

  test("exclude in a script") {
    val testInputs = TestInputs(
      os.rel / "Hello.scala" -> "object Hello",
      os.rel / "Other.scala" -> "object Other",
      os.rel / "main.sc"     ->
        """//> using exclude Other.scala
          |println("hi")
          |""".stripMargin
    )
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources       =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        ).orThrow

      val onDiskPaths    = sources.paths.map(_._2)
      val expectedOnDisk = Seq(os.rel / "Hello.scala")
      expect(onDiskPaths == expectedOnDisk)
      val inMemoryPaths    = sources.inMemory.map(_.generatedRelPath)
      val expectedInMemory = Seq(os.rel / "main.scala")
      expect(inMemoryPaths == expectedInMemory)
    }
  }

  test("exclude in a script pulling sources via using file") {
    val testInputs = TestInputs(
      os.rel / "Helper.scala" -> "object Helper",
      os.rel / "Other.scala"  -> "object Other",
      os.rel / "main.sc"      ->
        """//> using file Helper.scala
          |//> using exclude Other.scala
          |println(Helper)
          |""".stripMargin
    )
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources       =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        ).orThrow

      val onDiskPaths    = sources.paths.map(_._2)
      val expectedOnDisk = Seq(os.rel / "Helper.scala")
      expect(onDiskPaths == expectedOnDisk)
      val inMemoryPaths    = sources.inMemory.map(_.generatedRelPath)
      val expectedInMemory = Seq(os.rel / "main.scala")
      expect(inMemoryPaths == expectedInMemory)
    }
  }

  test("error message when exclude is in an unsupported file") {
    val testInputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using exclude Other.scala
          |""".stripMargin,
      os.rel / "Other.scala" -> "object Other"
    )
    testInputs.withInputs { (_, inputs) =>
      val crossSources =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy)
      crossSources match {
        case Left(e: ExcludeDefinitionError) =>
          val msg = e.message
          expect(msg.contains("`.sc` script"))
          expect(msg.contains("project.scala"))
        case o => fail("Exception expected", clues(o))
      }
    }
  }

  test("error when exclude is declared in both project.scala and a script") {
    val testInputs = TestInputs(
      os.rel / "project.scala" -> "//> using exclude Other.scala",
      os.rel / "main.sc"       ->
        """//> using exclude Hello.scala
          |println("hi")
          |""".stripMargin,
      os.rel / "Hello.scala" -> "object Hello",
      os.rel / "Other.scala" -> "object Other"
    )
    testInputs.withInputs { (_, inputs) =>
      val crossSources =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )(using ScalaCliInvokeData.dummy)
      crossSources match {
        case Left(e: ExcludeDefinitionError) =>
          expect(e.message.contains("single source file"))
        case o => fail("Exception expected", clues(o))
      }
    }
  }

}
