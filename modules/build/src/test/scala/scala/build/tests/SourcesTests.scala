package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.{ArchiveCache, Cache}
import coursier.util.{Artifact, Task}
import dependency.*

import java.nio.charset.StandardCharsets
import scala.build.Ops.*
import scala.build.{CrossSources, Position, Sources}
import scala.build.errors.{UsingDirectiveValueNumError, UsingDirectiveWrongValueTypeError}
import scala.build.input.ScalaCliInvokeData
import scala.build.options.{BuildOptions, Scope, SuppressWarningOptions}
import scala.build.internal.ScalaJsLinkerConfig

class SourcesTests extends TestUtil.ScalaCliBuildSuite {

  def scalaVersion       = "2.13.5"
  def scalaParams        = ScalaParameters(scalaVersion)
  def scalaBinaryVersion = scalaParams.scalaBinaryVersion

  given ScalaCliInvokeData = ScalaCliInvokeData.dummy

  val preprocessors = Sources.defaultPreprocessors(
    ArchiveCache().withCache(
      new Cache[Task] {
        def fetch                    = _ => sys.error("shouldn't be used")
        def file(artifact: Artifact) = sys.error("shouldn't be used")
        def ec                       = sys.error("shouldn't be used")
      }
    ),
    None,
    () => sys.error("shouldn't be used")
  )

  for (
    (singularAlias, pluralAlias) <-
      List(("lib", "libs"), ("dep", "deps"), ("dependency", "dependencies"))
  )
    test(s"dependencies in .scala - using aliases: $pluralAlias and $singularAlias") {
      val testInputs = TestInputs(
        os.rel / "something.scala" ->
          s"""//> using $pluralAlias "org1:name1:1.1", "org2::name2:2.2"
             |//> using $singularAlias "org3:::name3:3.3"
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
      testInputs.withInputs { (root, inputs) =>
        val (crossSources, _) =
          CrossSources.forInputs(
            inputs,
            preprocessors,
            TestLogger(),
            SuppressWarningOptions()
          ).orThrow

        val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
        val sources =
          scopedSources.sources(
            Scope.Main,
            crossSources.sharedOptions(BuildOptions()),
            root,
            TestLogger()
          )
            .orThrow

        val obtainedDeps = sources.buildOptions.classPathOptions.extraDependencies.toSeq.toSeq.map(
          _.value
        )

        expect(obtainedDeps.sortBy(_.version) == expectedDeps.sortBy(_.version))
        expect(sources.paths.length == 1)
        expect(sources.paths.map(_._2) == Seq(os.rel / "something.scala"))
        expect(sources.inMemory.isEmpty)
      }
    }

  test("dependencies in .scala - using witin tests") {
    val testInputs = TestInputs(
      os.rel / "something.test.scala" ->
        """//> using deps "org1:name1:1.1", "org2::name2:2.2"
          |//> using dep "org3:::name3:3.3"
          |import scala.collection.mutable
          |
          |object Something {
          |  def a = 1
          |}
          |""".stripMargin
    )
    val expectedDeps = Nil
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

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
        """//> using deps "org1:name1:1.1", "org2::name2:2.2"
          |//> using dep "org3:::name3:3.3"
          |import scala.collection.mutable
          |
          |object Something {
          |  def a = 1
          |}
          |""".stripMargin
    )
    val expectedDeps = Nil
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

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
        """//> using deps "org1:name1:1.1", "org2::name2:2.2"
          |//> using dep "org3:::name3:3.3"
          |import scala.collection.mutable
          |
          |object Something {
          |  def a = 1
          |}
          |""".stripMargin
    )
    val testInputs = TestInputs(files, Seq("."))
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

      expect(sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value).isEmpty)
      expect(sources.paths.isEmpty)
      expect(sources.inMemory.isEmpty)
    }
  }

  test("dependencies in .scala - //> using") {
    val testInputs = TestInputs(
      os.rel / "something.scala" ->
        """//> using dep "org1:name1:1.1"
          |//> using dep "org2::name2:2.2"
          |//> using dep "org3:::name3:3.3"
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
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

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
        """//> using dep "org1:name1:1.1"
          |//> using dep "org2::name2:2.2"
          |//> using dep "org3:::name3:3.3"
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
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

      expect(
        sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(_.value) == expectedDeps
      )
      expect(sources.paths.length == 1)
      expect(sources.paths.map(_._2) == Seq(os.rel / "Something.java"))
      expect(sources.inMemory.isEmpty)
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
          |println("Hello World #!")""".stripMargin,
      os.rel / "multiline.scala" ->
        """#!/usr/bin/scala-cli
          |# comment
          |VAL=1
          |!#
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
        |println("Hello World #!")""".stripMargin,
      """
        |
        |
        |
        |
        |println("Hello World #!")""".stripMargin
    )

    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

      val parsedCodes: Seq[String] =
        sources.inMemory.map(_.content).map(s => new String(s, StandardCharsets.UTF_8))

      parsedCodes.zip(expectedParsedCodes).foreach { case (parsedCode, expectedCode) =>
        expect(parsedCode.contains(expectedCode))
      }
    }
  }

  test("dependencies in .sc - using") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using deps "org1:name1:1.1", "org2::name2:2.2", "org3:::name3:3.3"
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
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

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
        """//> using dep "org1:name1:1.1"
          |//> using dep "org2::name2:2.2"
          |//> using dep "org3:::name3:3.3"
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
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

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
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow
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
          |//> using jsNoOpt
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
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        ).orThrow

      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow

      val jsOptions = sources.buildOptions.scalaJsOptions
      val jsConfig  = jsOptions.linkerConfig(TestLogger())
      expect(
        jsOptions.version == Some("1.8.0"),
        jsOptions.mode.nameOpt.contains("mode"),
        jsOptions.moduleKindStr == Some("commonjs"),
        jsOptions.checkIr == Some(true),
        jsOptions.emitSourceMaps == true,
        jsOptions.dom == Some(true),
        jsOptions.noOpt == Some(true)
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
          TestLogger(),
          SuppressWarningOptions()
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
          TestLogger(),
          SuppressWarningOptions()
        )
      crossSources match {
        case Left(_: UsingDirectiveWrongValueTypeError) =>
        case o                                          => fail("Exception expected", clues(o))
      }
    }
  }

  test("CrossSources.forInputs respects the order of inputs passed") {
    val inputArgs @ Seq(project, main, abc, message) =
      Seq("project.scala", "Main.scala", "Abc.scala", "Message.scala")
    val testInputs = TestInputs(
      os.rel / project ->
        """//> using dep "com.lihaoyi::os-lib::0.8.1"
          |//> using file "Message.scala"
          |""".stripMargin,
      os.rel / main ->
        """object Main extends App {
          |  println(Message(Abc.hello))
          |}
          |""".stripMargin,
      os.rel / abc ->
        """object Abc {
          |  val hello = "Hello"
          |}
          |""".stripMargin,
      os.rel / message ->
        """case class Message(value: String)
          |""".stripMargin
    )
    testInputs.withInputs { (_, inputs) =>
      val crossSourcesResult =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions()
        )
      assert(crossSourcesResult.isRight)
      val Right(CrossSources(onDiskSources, _, _, _, _, _)) =
        crossSourcesResult.map(_._1)
      val onDiskPaths = onDiskSources.map(_.value._1.last)
      expect(onDiskPaths == inputArgs)
    }
  }

}
