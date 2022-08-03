package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.{ArchiveCache, Cache}
import coursier.util.{Artifact, Task}
import dependency._

import scala.build.Ops._
import scala.build.Sources
import scala.build.internal.CustomCodeWrapper
import scala.build.CrossSources
import scala.build.Position
import scala.build.errors.{UsingDirectiveValueNumError, UsingDirectiveWrongValueTypeError}
import scala.build.options.{BuildOptions, Scope}
import scala.build.internal.ScalaJsLinkerConfig

class SourcesTests extends munit.FunSuite {

  def scalaVersion       = "2.13.5"
  def scalaParams        = ScalaParameters(scalaVersion)
  def scalaBinaryVersion = scalaParams.scalaBinaryVersion

  val preprocessors = Sources.defaultPreprocessors(
    CustomCodeWrapper,
    ArchiveCache().withCache(
      new Cache[Task] {
        def fetch                    = _ => sys.error("shouldn't be used")
        def file(artifact: Artifact) = sys.error("shouldn't be used")
        def ec                       = sys.error("shouldn't be used")
      }
    ),
    None
  )

  test("dependencies in .scala - $ivy") {
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
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(
        sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value) == expectedDeps
      )
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.length == 1)
      expect(sources.inMemory.map(_.generatedRelPath) == Seq(os.rel / "something.scala"))
    }
  }

  test("dependencies in .scala - using") {
    val testInputs = TestInputs(
      os.rel / "something.scala" ->
        """//> using libs "org1:name1:1.1", "org2::name2:2.2"
          |//> using lib "org3:::name3:3.3"
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
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(sources.buildOptions.classPathOptions.extraDependencies.toSeq.toSeq.map(
        _.value
      ) == expectedDeps)
      expect(sources.paths.length == 1)
      expect(sources.paths.map(_._2) == Seq(os.rel / "something.scala"))
      expect(sources.inMemory.isEmpty)
    }
  }

  test("dependencies in .scala - using witin tests") {
    val testInputs = TestInputs(
      os.rel / "something.scala" ->
        """//> using target.scope "test"
          |//> using libs "org1:name1:1.1", "org2::name2:2.2"
          |//> using lib "org3:::name3:3.3"
          |import scala.collection.mutable
          |
          |object Something {
          |  def a = 1
          |}
          |""".stripMargin
    )
    val expectedDeps = Nil
    testInputs.withInputs { (_, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(
        sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value) == expectedDeps
      )
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.length == 0)
    }
  }

  test("dependencies in .test.scala - using") {
    val testInputs = TestInputs(
      os.rel / "something.test.scala" ->
        """//> using libs "org1:name1:1.1", "org2::name2:2.2"
          |//> using lib "org3:::name3:3.3"
          |import scala.collection.mutable
          |
          |object Something {
          |  def a = 1
          |}
          |""".stripMargin
    )
    val expectedDeps = Nil
    testInputs.withInputs { (_, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(
        sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value) == expectedDeps
      )
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.length == 0)
    }
  }

  test("dependencies in test/name.scala") {
    val files = Seq(
      os.rel / "test" / "something.scala" ->
        """//> using libs "org1:name1:1.1", "org2::name2:2.2"
          |//> using lib "org3:::name3:3.3"
          |import scala.collection.mutable
          |
          |object Something {
          |  def a = 1
          |}
          |""".stripMargin
    )
    val testInputs = TestInputs(files, Seq("."))
    testInputs.withInputs { (_, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value).isEmpty)
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.isEmpty)
    }
  }

  test("dependencies in .scala - //> using") {
    val testInputs = TestInputs(
      os.rel / "something.scala" ->
        """//> using lib "org1:name1:1.1"
          |//> using lib "org2::name2:2.2"
          |//> using lib "org3:::name3:3.3"
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
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(
        sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value) == expectedDeps
      )
      expect(sources.paths.length == 1)
      expect(sources.paths.map(_._2) == Seq(os.rel / "something.scala"))
      expect(sources.inMemory.isEmpty)
    }
  }

  test("dependencies in .java - //> using") {
    val testInputs = TestInputs(
      os.rel / "Something.java" ->
        """//> using lib "org1:name1:1.1"
          |//> using lib "org2::name2:2.2"
          |//> using lib "org3:::name3:3.3"
          |
          |public class Something {
          |  public Int a = 1;
          |}
          |""".stripMargin
    )
    val expectedDeps = Seq(
      dep"org1:name1:1.1",
      dep"org2::name2:2.2",
      dep"org3:::name3:3.3"
    )
    testInputs.withInputs { (_, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(
        sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value) == expectedDeps
      )
      expect(sources.paths.length == 1)
      expect(sources.paths.map(_._2) == Seq(os.rel / "Something.java"))
      expect(sources.inMemory.isEmpty)
    }
  }

  test("should fail dependencies in .java  with using keyword") {
    val testInputs = TestInputs(
      os.rel / "Something.java" ->
        """using lib "org3:::name3:3.3"
          |
          |public class Something {
          |  public Int a = 1;
          |}
          |""".stripMargin
    )
    testInputs.withInputs { (_, inputs) =>
      val crossSources = CrossSources.forInputs(
        inputs,
        preprocessors,
        TestLogger()
      )
      expect(crossSources.isLeft)
    }
  }

  test("dependencies in .sc - $ivy") {
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
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(
        sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value) == expectedDeps
      )
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.length == 1)
      expect(sources.inMemory.map(_.generatedRelPath) == Seq(os.rel / "something.scala"))
    }
  }

  test("should skip SheBang in .sc and .scala") {
    val testInputs = TestInputs(
      os.rel / "something1.sc" ->
        """#!/usr/bin/env scala-cli
          |
          |println("Hello World")""".stripMargin,
      os.rel / "something2.sc" ->
        """#!/usr/bin/scala-cli
          |
          |println("Hello World")""".stripMargin,
      os.rel / "something3.sc" ->
        """#!/usr/bin/scala-cli
          |#! nix-shell -i scala-cli
          |
          |println("Hello World")""".stripMargin,
      os.rel / "something4.scala" ->
        """#!/usr/bin/scala-cli
          |#! nix-shell -i scala-cli
          |
          |!#
          |
          |println("Hello World")""".stripMargin,
      os.rel / "something5.scala" ->
        """#!/usr/bin/scala-cli
          |
          |println("Hello World #!")""".stripMargin
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
        |println("Hello World")""".stripMargin,
      """
        |
        |println("Hello World #!")""".stripMargin
    )

    testInputs.withInputs { (_, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      val parsedCodes: Seq[String] = sources.inMemory.map(_.generatedContent)

      parsedCodes.zip(expectedParsedCodes).foreach { case (parsedCode, expectedCode) =>
        expect(parsedCode.contains(expectedCode))
      }
    }
  }

  test("dependencies in .sc - using") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using libs "org1:name1:1.1", "org2::name2:2.2", "org3:::name3:3.3"
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
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(
        sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value) == expectedDeps
      )
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.length == 1)
      expect(sources.inMemory.map(_.generatedRelPath) == Seq(os.rel / "something.scala"))
    }
  }

  test("dependencies in .sc - //> using") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using lib "org1:name1:1.1"
          |//> using lib "org2::name2:2.2"
          |//> using lib "org3:::name3:3.3"
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
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))

      expect(
        sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value) == expectedDeps
      )
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.length == 1)
      expect(sources.inMemory.map(_.generatedRelPath) == Seq(os.rel / "something.scala"))
    }
  }

  test("java props in using directives") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using javaProp "foo1"
          |//> using javaProp "foo2=bar2"
          |""".stripMargin
    )
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources  = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))
      val javaOpts = sources.buildOptions.javaOptions.javaOpts.toSeq.sortBy(_.toString)

      expect(
        javaOpts(0).value.value == "-Dfoo1",
        javaOpts(0).positions == Seq(Position.File(Right(root / "something.sc"), (0, 20), (0, 24))),
        javaOpts(1).value.value == "-Dfoo2=bar2",
        javaOpts(1).positions == Seq(Position.File(Right(root / "something.sc"), (1, 20), (1, 29)))
      )
    }
  }

  test("js options in using directives") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using jsVersion "1.8.0"
          |//> using jsMode "mode"
          |//> using jsModuleKind "commonjs"
          |//> using jsCheckIr true
          |//> using jsEmitSourceMaps true
          |//> using jsDom true
          |//> using jsHeader "#!/usr/bin/env node\n"
          |//> using jsAllowBigIntsForLongs true
          |//> using jsAvoidClasses false
          |//> using jsAvoidLetsAndConsts false
          |//> using jsModuleSplitStyleStr "smallestmodules"
          |//> using jsEsVersionStr "es2017"
          |""".stripMargin
    )
    testInputs.withInputs { (_, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources   = scopedSources.sources(Scope.Main, crossSources.sharedOptions(BuildOptions()))
      val jsOptions = sources.buildOptions.scalaJsOptions
      val jsConfig  = jsOptions.linkerConfig(TestLogger())
      expect(
        jsOptions.version == Some("1.8.0"),
        jsOptions.mode == Some("mode"),
        jsOptions.moduleKindStr == Some("commonjs"),
        jsOptions.checkIr == Some(true),
        jsOptions.emitSourceMaps == true,
        jsOptions.dom == Some(true)
      )
      expect(
        jsConfig.moduleKind == ScalaJsLinkerConfig.ModuleKind.CommonJSModule,
        jsConfig.checkIR == true,
        jsConfig.sourceMap == true,
        jsConfig.jsHeader == Some("#!/usr/bin/env node\n"),
        jsConfig.esFeatures.allowBigIntsForLongs == true,
        jsConfig.esFeatures.avoidClasses == false,
        jsConfig.esFeatures.avoidLetsAndConsts == false,
        jsConfig.esFeatures.esVersion == "ES2017",
        jsConfig.moduleSplitStyle == ScalaJsLinkerConfig.ModuleSplitStyle.SmallestModules
      )
    }
  }

  test("js options in using directives failure - multiple values") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using jsVersion "1.8.0","2.3.4"
          |""".stripMargin
    )
    testInputs.withInputs { (_, inputs) =>
      val crossSources =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        )
      crossSources match {
        case Left(_: UsingDirectiveValueNumError) =>
        case o                                    => fail("Exception expected", clues(o))
      }
    }
  }

  test("js options in using directives failure - not a boolean") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using jsDom "fasle"
          |""".stripMargin
    )
    testInputs.withInputs { (_, inputs) =>
      val crossSources =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger()
        )
      crossSources match {
        case Left(_: UsingDirectiveWrongValueTypeError) =>
        case o                                          => fail("Exception expected", clues(o))
      }
    }
  }

}
