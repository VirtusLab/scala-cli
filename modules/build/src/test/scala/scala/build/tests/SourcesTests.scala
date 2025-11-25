package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.Cache.Fetch
import coursier.cache.{ArchiveCache, ArtifactError, Cache}
import coursier.util.{Artifact, EitherT, Task}
import dependency.*

import java.io.File
import java.nio.charset.StandardCharsets

import scala.build.Ops.*
import scala.build.errors.{UsingDirectiveValueNumError, UsingDirectiveWrongValueTypeError}
import scala.build.input.ScalaCliInvokeData
import scala.build.internal.ScalaJsLinkerConfig
import scala.build.options.{BuildOptions, Scope, SuppressWarningOptions}
import scala.build.preprocessing.Preprocessor
import scala.build.{CrossSources, Position, Sources}
import scala.concurrent.ExecutionContext

class SourcesTests extends TestUtil.ScalaCliBuildSuite {
  def scalaVersion: String         = "2.13.5"
  def scalaParams: ScalaParameters = ScalaParameters(scalaVersion)
  def scalaBinaryVersion: String   = scalaParams.scalaBinaryVersion

  given ScalaCliInvokeData = ScalaCliInvokeData.dummy

  val preprocessors: Seq[Preprocessor] = Sources.defaultPreprocessors(
    ArchiveCache().withCache(
      new Cache[Task] {
        def fetch: Fetch[Task] = _ => sys.error("shouldn't be used")
        def file(artifact: Artifact): EitherT[Task, ArtifactError, File] =
          sys.error("shouldn't be used")
        def ec: ExecutionContext = sys.error("shouldn't be used")
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
          s"""//> using $pluralAlias org1:name1:1.1 org2::name2:2.2
             |//> using $singularAlias org3:::name3:3.3
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
        val sources       =
          scopedSources.sources(
            Scope.Main,
            crossSources.sharedOptions(BuildOptions()),
            root,
            TestLogger()
          )
            .orThrow

        val obtainedDeps = sources.buildOptions.classPathOptions.extraDependencies.toSeq.map(
          _.value
        )

        expect(obtainedDeps.sortBy(_.version) == expectedDeps.sortBy(_.version))
        expect(sources.paths.length == 1)
        val path = os.rel / "something.scala"
        expect(sources.paths.map(_._2) == Seq(path))
        expect(sources.inMemory.isEmpty)
      }
    }

  test("dependencies in .scala - using witin tests") {
    val testInputs = TestInputs(
      os.rel / "something.test.scala" ->
        """//> using deps org1:name1:1.1 org2::name2:2.2
          |//> using dep org3:::name3:3.3
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
      val sources       =
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
      expect(sources.inMemory.isEmpty)
    }
  }

  test("dependencies in .scala - using URL with query parameters") {
    val testInputs = TestInputs(
      os.rel / "something.scala" ->
        """| //> using file http://github.com/VirtusLab/scala-cli/blob/main/modules/dummy/amm/src/main/scala/AmmDummy.scala?version=3
           |
           |object Main {
           |}
           |""".stripMargin
    )
    testInputs.withInputs { (root, inputs) =>
      val (crossSources, _) =
        CrossSources.forInputs(
          inputs,
          preprocessors,
          TestLogger(),
          SuppressWarningOptions(),
          download = _ => Right(Array.empty[Byte])
        ).orThrow
      val scopedSources = crossSources.scopedSources(BuildOptions()).orThrow
      val sources       =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        ).orThrow

      expect(sources.paths.length == 1)
      expect(sources.inMemory.length == 1)
      expect(sources.inMemory.head.generatedRelPath.last == "AmmDummy.scala")
    }
  }

  test("dependencies in .test.scala - using") {
    val testInputs = TestInputs(
      os.rel / "something.test.scala" ->
        """//> using deps org1:name1:1.1 org2::name2:2.2
          |//> using dep org3:::name3:3.3
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
      val sources       =
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
      expect(sources.inMemory.isEmpty)
    }
  }

  test("dependencies in test/name.scala") {
    val files = Seq(
      os.rel / "test" / "something.scala" ->
        """//> using deps org1:name1:1.1 org2::name2:2.2
          |//> using dep org3:::name3:3.3
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
      val sources       =
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
        """//> using dep org1:name1:1.1
          |//> using dep org2::name2:2.2
          |//> using dep org3:::name3:3.3
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
      val sources       =
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
      val path = os.rel / "something.scala"
      expect(sources.paths.map(_._2) == Seq(path))
      expect(sources.inMemory.isEmpty)
    }
  }

  test("dependencies in .java - //> using") {
    val testInputs = TestInputs(
      os.rel / "Something.java" ->
        """//> using dep org1:name1:1.1
          |//> using dep org2::name2:2.2
          |//> using dep org3:::name3:3.3
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
      val sources       =
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
      val path = os.rel / "Something.java"
      expect(sources.paths.map(_._2) == Seq(path))
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
      os.rel / "something4.sc" ->
        """#!/usr/bin/scala-cli
          |#! nix-shell -i scala-cli
          |
          |!#
          |
          |println("Hello World")""".stripMargin,
      os.rel / "something5.sc" ->
        """#!/usr/bin/scala-cli
          |
          |println("Hello World #!")""".stripMargin,
      os.rel / "multiline.sc" ->
        """#!/usr/bin/scala-cli
          |# comment
          |VAL=1
          |!#
          |
          |println("Hello World #!")""".stripMargin,
      os.rel / "hasBangHashInComment.sc" ->
        """#!/usr/bin/scala-cli
          |
          |
          |
          |
          |println("Hello World !#")""".stripMargin
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
        |println("Hello World #!")""".stripMargin,
      """
        |
        |
        |
        |
        |println("Hello World !#")""".stripMargin
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
      val sources       =
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
        showDiff(parsedCode, expectedCode)
        expect(parsedCode.contains(expectedCode))
      }
    }
  }
  def showDiff(parsed: String, expected: String): Unit = {
    if (!parsed.contains(expected))
      for (((p, e), i) <- (parsed zip expected).zipWithIndex) {
        val ps = TestUtil.c2s(p)
        val es = TestUtil.c2s(e)
        if (ps != es)
          System.err.printf("%2d: [%s]!=[%s]\n", i, ps, es)
      }
  }

  test("dependencies in .sc - using") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using deps org1:name1:1.1 org2::name2:2.2 org3:::name3:3.3
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
      val sources       =
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
      val path = os.rel / "something.scala"
      expect(sources.inMemory.map(_.generatedRelPath) == Seq(path))
    }
  }

  test("dependencies in .sc - //> using") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using dep org1:name1:1.1
          |//> using dep org2::name2:2.2
          |//> using dep org3:::name3:3.3
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
      val sources       =
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
      val path = os.rel / "something.scala"
      expect(sources.inMemory.map(_.generatedRelPath) == Seq(path))
    }
  }

  test("java props in using directives") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using javaProp foo1
          |//> using javaProp foo2=bar2
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
      val sources       =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow
      val javaOpts = sources.buildOptions.javaOptions.javaOpts.toSeq.sortBy(_.toString)

      val path = root / "something.sc"
      expect(
        javaOpts.head.value.value == "-Dfoo1",
        javaOpts.head.positions == Seq(Position.File(Right(path), (0, 19), (0, 23))),
        javaOpts(1).value.value == "-Dfoo2=bar2",
        javaOpts(1).positions == Seq(Position.File(Right(path), (1, 19), (1, 28)))
      )
    }
  }

  test("java -XX:* options in using directives") {
    val (opt1, opt2, opt3) = (
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+AlwaysPreTouch",
      "-XX:+UseParallelGC"
    )
    val scriptPath = os.rel / "something.sc"
    val testInputs = TestInputs(
      scriptPath ->
        s"""//> using javaOpt $opt1 $opt2 $opt3
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
      val sources       =
        scopedSources.sources(
          Scope.Main,
          crossSources.sharedOptions(BuildOptions()),
          root,
          TestLogger()
        )
          .orThrow
      val javaOpts = sources.buildOptions.javaOptions.javaOpts.toSeq.sortBy(_.toString)

      val scriptAbsolutePath = root / scriptPath
      val startPosX          = 0
      val startPosY1         = 18
      expect(
        javaOpts.head.value.value == opt1,
        javaOpts.head.positions == Seq(Position.File(
          Right(scriptAbsolutePath),
          (startPosX, startPosY1),
          (startPosX, startPosY1 + opt1.length)
        ))
      )
      val startPosY2 = startPosY1 + opt1.length + 1
      expect(
        javaOpts.drop(1).head.value.value == opt2,
        javaOpts.drop(1).head.positions == Seq(Position.File(
          Right(scriptAbsolutePath),
          (startPosX, startPosY2),
          (startPosX, startPosY2 + opt2.length)
        ))
      )
      val startPosY3 = startPosY2 + opt2.length + 1
      expect(
        javaOpts.drop(2).head.value.value == opt3,
        javaOpts.drop(2).head.positions == Seq(Position.File(
          Right(scriptAbsolutePath),
          (startPosX, startPosY3),
          (startPosX, startPosY3 + opt3.length)
        ))
      )
    }
  }

  test("js options in using directives") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using jsVersion 1.8.0
          |//> using jsMode mode
          |//> using jsNoOpt
          |//> using jsModuleKind commonjs
          |//> using jsCheckIr true
          |//> using jsEmitSourceMaps true
          |//> using jsDom true
          |//> using jsHeader "#!/usr/bin/env node\n"
          |//> using jsAllowBigIntsForLongs true
          |//> using jsAvoidClasses false
          |//> using jsAvoidLetsAndConsts false
          |//> using jsModuleSplitStyleStr smallestmodules
          |//> using jsEsVersionStr es2017
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
      val sources       =
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
        jsOptions.version.contains("1.8.0"),
        jsOptions.mode.nameOpt.contains("mode"),
        jsOptions.moduleKindStr.contains("commonjs"),
        jsOptions.checkIr.contains(true),
        jsOptions.emitSourceMaps,
        jsOptions.dom.contains(true),
        jsOptions.noOpt.contains(true)
      )
      expect(
        jsConfig.moduleKind == ScalaJsLinkerConfig.ModuleKind.CommonJSModule,
        jsConfig.checkIR,
        jsConfig.sourceMap,
        jsConfig.jsHeader.contains("#!/usr/bin/env node\n"),
        jsConfig.esFeatures.allowBigIntsForLongs,
        !jsConfig.esFeatures.avoidClasses,
        !jsConfig.esFeatures.avoidLetsAndConsts,
        jsConfig.esFeatures.esVersion == "ES2017",
        jsConfig.moduleSplitStyle == ScalaJsLinkerConfig.ModuleSplitStyle.SmallestModules
      )
    }
  }

  test("js options in using directives failure - multiple values") {
    val testInputs = TestInputs(
      os.rel / "something.sc" ->
        """//> using jsVersion 1.8.0 2.3.4
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
        """//> using jsDom fasle
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
        """//> using dep com.lihaoyi::os-lib::0.8.1
          |//> using file Message.scala
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
      val CrossSources(onDiskSources, _, _, _, _, _) =
        crossSourcesResult.map(_._1)
          .getOrElse(sys.error("should not happen"))
      val onDiskPaths = onDiskSources.map(_.value._1.last)
      expect(onDiskPaths == inputArgs)
    }
  }

}
