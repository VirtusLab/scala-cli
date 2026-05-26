package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import com.sun.net.httpserver.HttpServer
import coursier.cache.FileCache
import coursier.util.Task
import coursier.version.Version

import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ConcurrentLinkedQueue, Executors}

import scala.build.Ops.*
import scala.build.Position.File
import scala.build.actionable.ActionableDiagnostic.*
import scala.build.actionable.ActionablePreprocessor
import scala.build.options.{BuildOptions, InternalOptions, SuppressWarningOptions}
import scala.build.{BuildThreads, LocalRepo}
import scala.jdk.CollectionConverters.*

class ActionableDiagnosticTests extends TestUtil.ScalaCliBuildSuite {

  val extraRepoTmpDir: os.Path   = os.temp.dir(prefix = "scala-cli-tests-actionable-diagnostic-")
  val testCache: FileCache[Task] =
    FileCache().withLocation((extraRepoTmpDir / "cache").toIO)
  val baseOptions = BuildOptions(
    internal = InternalOptions(
      cache = Some(testCache),
      localRepository = LocalRepo.localRepo(testCache, TestLogger())
    )
  )
  val buildThreads: BuildThreads = BuildThreads.create()

  def path2url(p: os.Path): String = p.toIO.toURI.toURL.toString

  /** Minimal HTTP Maven repo: records every request path, then optional delay on
    * `maven-metadata.xml` when `delayWhen()` is true, then serves a body from `responses` or 404.
    */
  def withRecordingMavenRepo(
    responses: Map[String, Array[Byte]],
    delayOnMetadataMs: Long = 0,
    delayWhen: () => Boolean = () => false
  )(body: (String, ConcurrentLinkedQueue[String]) => Unit): Unit =
    val recorded = new ConcurrentLinkedQueue[String]()
    val address  = "127.0.0.1"
    val server   = HttpServer.create(new InetSocketAddress(address, 0), 0)
    server.setExecutor(Executors.newCachedThreadPool())
    server.createContext(
      "/",
      ex => {
        val path = ex.getRequestURI.getPath
        recorded.offer(path)
        if delayOnMetadataMs > 0 && delayWhen() && path.endsWith("maven-metadata.xml") then
          Thread.sleep(delayOnMetadataMs)
        responses.get(path) match
          case Some(bytes) =>
            ex.getResponseHeaders.set("Content-Type", "application/xml")
            ex.sendResponseHeaders(200, bytes.length)
            ex.getResponseBody.write(bytes)
            ex.getResponseBody.close()
          case None =>
            ex.sendResponseHeaders(404, -1)
        ex.close()
      }
    )
    server.start()
    try
      val base = s"http://$address:${server.getAddress.getPort}/"
      body(base, recorded)
    finally server.stop(0)

  def buildOptionsWithEmptyCoursierCache(opts: BuildOptions): BuildOptions =
    val dir = os.temp.dir(prefix = "scala-cli-actionable-diagnostic-coursier-")
    opts.copy(internal =
      opts.internal.copy(
        cache = Some(FileCache[Task]().withLocation(dir.toString))
      )
    )

  test("using outdated os-lib") {
    val dependencyOsLib = "com.lihaoyi::os-lib:0.7.8"
    val testInputs      = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using dep $dependencyOsLib
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, None, actionableDiagnostics = true) {
      (_, _, maybeBuild) =>
        val build             = maybeBuild.orThrow
        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val osLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }

        expect(osLibDiagnosticOpt.nonEmpty)
        val osLibDiagnostic = osLibDiagnosticOpt.get

        expect(Version(osLibDiagnostic.newVersion) > Version(osLibDiagnostic.currentVersion))
    }
  }

  test("actionable diagnostic report correct position") {
    val dependencyOsLib     = "com.lihaoyi::os-lib:0.7.8"
    val dependencyPprintLib = "com.lihaoyi::pprint:0.6.6"
    val testInputs          = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using dep $dependencyOsLib
           |//> using dep $dependencyPprintLib
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, None, actionableDiagnostics = true) {
      (root, _, maybeBuild) =>
        val build             = maybeBuild.orThrow
        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val actionableDiagnostics = updateDiagnostics.collect {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }

        val osLib = actionableDiagnostics.find(_.suggestion.startsWith("com.lihaoyi::os-lib")).get
        val pprintLib =
          actionableDiagnostics.find(_.suggestion.startsWith("com.lihaoyi::pprint")).get

        val path = root / "Foo.scala"
        expect(osLib.positions == Seq(File(Right(path), (0, 14), (0, 39))))
        expect(pprintLib.positions == Seq(File(Right(path), (1, 14), (1, 39))))
    }
  }

  test("using outdated dependencies with --suppress-outdated-dependency-warning") {
    val dependencyOsLib     = "com.lihaoyi::os-lib:0.7.8"
    val dependencyPprintLib = "com.lihaoyi::pprint:0.6.6"
    val testInputs          = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using dep $dependencyOsLib
           |//> using dep $dependencyPprintLib
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    val optionsWithSuppress = baseOptions.copy(
      suppressWarningOptions = SuppressWarningOptions(
        suppressOutdatedDependencyWarning = Some(true)
      )
    )

    testInputs.withBuild(optionsWithSuppress, buildThreads, None, actionableDiagnostics = true) {
      (_, _, maybeBuild) =>
        val build             = maybeBuild.orThrow
        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val updateDepsDiagnostics = updateDiagnostics.collect {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }

        expect(updateDepsDiagnostics.isEmpty)
    }
  }

  test("actionable actions suggest update only to stable version") {
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using dep test-org::test-name-1:1.0.6
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    // create fake repository which contains hardcoded versions [1.0.6, 1.0.7, 1.0.7-M1] of test-name-1 library
    // scala-cli should skip non-stable version 1.0.7-M1 and suggest update 1.0.7
    val repoTmpDir = os.temp.dir(prefix = "scala-cli-tests-actionable-diagnostic-repo")
    os.write(
      repoTmpDir / "test-org" / "test-name-1_3" / "maven-metadata.xml",
      """<?xml version="1.0" encoding="UTF-8"?>
        |<metadata>
        |  <groupId>test-org</groupId>
        |  <artifactId>test-name-1_3</artifactId>
        |  <versioning>
        |    <latest>1.0.7-M1</latest>
        |    <release>1.0.7-M1</release>
        |    <versions>
        |      <version>1.0.6</version>
        |      <version>1.0.7</version>
        |      <version>1.0.7-M1</version>
        |    </versions>
        |  </versioning>
        |</metadata>
        |""".stripMargin,
      createFolders = true
    )
    os.write(
      repoTmpDir / "test-org" / "test-name-1_3" / "1.0.6" / "test-name-1_3-1.0.6.pom",
      """<?xml version='1.0' encoding='UTF-8'?>
        |<project>
        |    <groupId>test-org</groupId>
        |    <artifactId>test-name-1_3</artifactId>
        |    <version>1.0.6</version>
        |</project>""".stripMargin,
      createFolders = true
    )
    val withRepoBuildOptions = baseOptions.copy(
      classPathOptions =
        baseOptions.classPathOptions.copy(extraRepositories = Seq(path2url(repoTmpDir)))
    )
    testInputs.withBuild(withRepoBuildOptions, buildThreads, None, actionableDiagnostics = true) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow

        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val testLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }
        expect(testLibDiagnosticOpt.nonEmpty)
        val testLibDiagnostic = testLibDiagnosticOpt.get

        expect(testLibDiagnostic.newVersion == "1.0.7")
    }
  }

  test("actionable actions should not suggest update to previous version") {
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using dep test-org::test-name-1:2.0.0-M1
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    // create fake repository which contains hardcoded versions [1.0.0] of test-name-1 library
    val repoTmpDir = os.temp.dir(prefix = "scala-cli-tests-actionable-diagnostic-repo")
    os.write(
      repoTmpDir / "test-org" / "test-name-1_3" / "maven-metadata.xml",
      """<?xml version="1.0" encoding="UTF-8"?>
        |<metadata>
        |  <groupId>test-org</groupId>
        |  <artifactId>test-name-1_3</artifactId>
        |  <versioning>
        |    <latest>2.0.0-M</latest>
        |    <release>2.0.0-M1</release>
        |    <versions>
        |      <version>1.0.0</version>
        |      <version>2.0.0-M1</version>
        |    </versions>
        |  </versioning>
        |</metadata>
        |""".stripMargin,
      createFolders = true
    )
    os.write(
      repoTmpDir / "test-org" / "test-name-1_3" / "2.0.0-M1" / "test-name-1_3-2.0.0-M1.pom",
      """<?xml version='1.0' encoding='UTF-8'?>
        |<project>
        |    <groupId>test-org</groupId>
        |    <artifactId>test-name-1_3</artifactId>
        |    <version>2.0.0-M1</version>
        |</project>""".stripMargin,
      createFolders = true
    )
    val withRepoBuildOptions = baseOptions.copy(
      classPathOptions =
        baseOptions.classPathOptions.copy(extraRepositories =
          Seq(path2url(repoTmpDir))
        )
    )
    testInputs.withBuild(withRepoBuildOptions, buildThreads, None, actionableDiagnostics = true) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow

        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val testLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }
        expect(testLibDiagnosticOpt.isEmpty)
    }
  }

  test("actionable actions should not suggest update if uses version: latest") {
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using toolkit latest
           |
           |object Hello extends App {
           |  os.list(os.pwd).foreach(println)
           |}
           |""".stripMargin
    )
    testInputs.withBuild(baseOptions, buildThreads, None, actionableDiagnostics = true) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow

        val updateDiagnostics =
          ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow

        val testLibDiagnosticOpt = updateDiagnostics.collectFirst {
          case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
        }
        expect(testLibDiagnosticOpt.isEmpty)
    }
  }

  test("actionable outdated check for toolkit skips user repository metadata") {
    val meta =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<metadata>
        |  <groupId>org.scala-lang</groupId>
        |  <artifactId>toolkit_3</artifactId>
        |  <versioning>
        |    <latest>99.0.0</latest>
        |    <release>99.0.0</release>
        |    <versions>
        |      <version>0.3.0</version>
        |      <version>99.0.0</version>
        |    </versions>
        |  </versioning>
        |</metadata>
        |""".stripMargin.getBytes("UTF-8")
    val responses = Map("/org/scala-lang/toolkit_3/maven-metadata.xml" -> meta)
    withRecordingMavenRepo(responses)((repoUrl, recorded) =>
      val testInputs = TestInputs(
        os.rel / "Foo.scala" ->
          """//> using toolkit 0.3.0
            |
            |object Hello extends App {
            |  println("Hello")
            |}
            |""".stripMargin
      )
      val withRepo = baseOptions.copy(
        classPathOptions =
          baseOptions.classPathOptions.copy(extraRepositories = Seq(repoUrl))
      )
      testInputs.withBuild(withRepo, buildThreads, None, actionableDiagnostics = true) {
        (_, _, maybeBuild) =>
          val build = maybeBuild.orThrow
          ActionablePreprocessor
            .generateActionableDiagnostics(buildOptionsWithEmptyCoursierCache(build.options))
            .orThrow
          val paths = recorded.asScala.toSeq
          expect(!paths.exists(_.contains("toolkit_3/maven-metadata.xml")))
      }
    )
  }

  test("actionable outdated check for org.scala-lang skips user repository metadata") {
    val u    = UUID.randomUUID().toString.replace("-", "")
    val art  = s"scala_cli_fake_$u"
    val meta =
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<metadata>
         |  <groupId>org.scala-lang</groupId>
         |  <artifactId>${art}_3</artifactId>
         |  <versioning>
         |    <latest>99.0.0</latest>
         |    <release>99.0.0</release>
         |    <versions>
         |      <version>0.1.0</version>
         |      <version>99.0.0</version>
         |    </versions>
         |  </versioning>
         |</metadata>
         |""".stripMargin.getBytes("UTF-8")
    val pom =
      s"""<?xml version='1.0' encoding='UTF-8'?>
         |<project>
         |    <groupId>org.scala-lang</groupId>
         |    <artifactId>${art}_3</artifactId>
         |    <version>0.1.0</version>
         |</project>""".stripMargin.getBytes("UTF-8")
    val responses = Map(
      s"/org/scala-lang/${art}_3/maven-metadata.xml"       -> meta,
      s"/org/scala-lang/${art}_3/0.1.0/${art}_3-0.1.0.pom" -> pom
    )
    withRecordingMavenRepo(responses)((repoUrl, recorded) =>
      val testInputs = TestInputs(
        os.rel / "Foo.scala" ->
          s"""//> using dep org.scala-lang::$art:0.1.0
             |
             |object Hello extends App {
             |  println("Hello")
             |}
             |""".stripMargin
      )
      val withRepo = baseOptions.copy(
        classPathOptions =
          baseOptions.classPathOptions.copy(extraRepositories = Seq(repoUrl))
      )
      testInputs.withBuild(withRepo, buildThreads, None, actionableDiagnostics = true) {
        (_, _, maybeBuild) =>
          val build = maybeBuild.orThrow
          ActionablePreprocessor
            .generateActionableDiagnostics(buildOptionsWithEmptyCoursierCache(build.options))
            .orThrow
          val paths = recorded.asScala.toSeq
          expect(!paths.exists(p => p.contains(s"${art}_3/") && p.contains("maven-metadata.xml")))
      }
    )
  }

  test("actionable outdated check still consults user repository for other organizations") {
    val u    = UUID.randomUUID().toString.replace("-", "")
    val art  = s"scala_cli_fake_$u"
    val meta =
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<metadata>
         |  <groupId>test-org</groupId>
         |  <artifactId>${art}_3</artifactId>
         |  <versioning>
         |    <latest>99.0.0</latest>
         |    <release>99.0.0</release>
         |    <versions>
         |      <version>0.1.0</version>
         |      <version>99.0.0</version>
         |    </versions>
         |  </versioning>
         |</metadata>
         |""".stripMargin.getBytes("UTF-8")
    val pom =
      s"""<?xml version='1.0' encoding='UTF-8'?>
         |<project>
         |    <groupId>test-org</groupId>
         |    <artifactId>${art}_3</artifactId>
         |    <version>0.1.0</version>
         |</project>""".stripMargin.getBytes("UTF-8")
    val responses = Map(
      s"/test-org/${art}_3/maven-metadata.xml"       -> meta,
      s"/test-org/${art}_3/0.1.0/${art}_3-0.1.0.pom" -> pom
    )
    withRecordingMavenRepo(responses)((repoUrl, recorded) =>
      val testInputs = TestInputs(
        os.rel / "Foo.scala" ->
          s"""//> using dep test-org::$art:0.1.0
             |
             |object Hello extends App {
             |  println("Hello")
             |}
             |""".stripMargin
      )
      val withRepo = baseOptions.copy(
        classPathOptions =
          baseOptions.classPathOptions.copy(extraRepositories = Seq(repoUrl))
      )
      testInputs.withBuild(withRepo, buildThreads, None, actionableDiagnostics = true) {
        (_, _, maybeBuild) =>
          val build = maybeBuild.orThrow
          val paths = recorded.asScala.toSeq
          expect(paths.exists(p => p.contains(s"${art}_3/") && p.contains("maven-metadata.xml")))
          val updateDiagnostics =
            ActionablePreprocessor.generateActionableDiagnostics(build.options).orThrow
          val dOpt = updateDiagnostics.collectFirst {
            case diagnostic: ActionableDependencyUpdateDiagnostic => diagnostic
          }
          expect(dOpt.nonEmpty)
          expect(dOpt.get.newVersion == "99.0.0")
      }
    )
  }

  test("actionable outdated check times out slow user repository") {
    val u    = UUID.randomUUID().toString.replace("-", "")
    val art  = s"scala_cli_fake_$u"
    val meta =
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<metadata>
         |  <groupId>test-org</groupId>
         |  <artifactId>${art}_3</artifactId>
         |  <versioning>
         |    <latest>0.2.0</latest>
         |    <release>0.2.0</release>
         |    <versions>
         |      <version>0.1.0</version>
         |      <version>0.2.0</version>
         |    </versions>
         |  </versioning>
         |</metadata>
         |""".stripMargin.getBytes("UTF-8")
    val pom =
      s"""<?xml version='1.0' encoding='UTF-8'?>
         |<project>
         |    <groupId>test-org</groupId>
         |    <artifactId>${art}_3</artifactId>
         |    <version>0.1.0</version>
         |</project>""".stripMargin.getBytes("UTF-8")
    val responses = Map(
      s"/test-org/${art}_3/maven-metadata.xml"       -> meta,
      s"/test-org/${art}_3/0.1.0/${art}_3-0.1.0.pom" -> pom
    )
    val slowAfterClear = new AtomicBoolean(false)
    withRecordingMavenRepo(
      responses,
      delayOnMetadataMs = 30_000L,
      delayWhen = () => slowAfterClear.get()
    )((repoUrl, _) =>
      val testInputs = TestInputs(
        os.rel / "Foo.scala" ->
          s"""//> using dep test-org::$art:0.1.0
             |
             |object Hello extends App {
             |  println("Hello")
             |}
             |""".stripMargin
      )
      val withRepo = baseOptions.copy(
        classPathOptions =
          baseOptions.classPathOptions.copy(extraRepositories = Seq(repoUrl))
      )
      testInputs.withBuild(withRepo, buildThreads, None, actionableDiagnostics = true) {
        (_, _, maybeBuild) =>
          val build = maybeBuild.orThrow
          slowAfterClear.set(true)
          val t0 = System.nanoTime()
          ActionablePreprocessor
            .generateActionableDiagnostics(buildOptionsWithEmptyCoursierCache(build.options))
            .orThrow
          val elapsedMs = (System.nanoTime() - t0) / 1_000_000
          expect(elapsedMs < 15_000)
      }
    )
  }
}
