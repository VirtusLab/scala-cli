package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import java.io.IOException
import scala.Console.println
import scala.build.Ops.EitherThrowOps
import scala.build.errors.ToolkitDirectiveMissingVersionError
import scala.build.options.{
  BuildOptions,
  InternalOptions,
  MaybeScalaVersion,
  Platform,
  ScalaOptions,
  ScalacOpt,
  Scope,
  ScriptOptions
}
import scala.build.tests.util.BloopServer
import scala.build.{Build, BuildThreads, Directories, LocalRepo, Position, Positioned}

class SourceGeneratorTests extends munit.FunSuite {

  val buildThreads = BuildThreads.create()

  def bloopConfigOpt = Some(BloopServer.bloopConfig)

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)

  override def afterAll(): Unit = {
    TestInputs.tryRemoveAll(extraRepoTmpDir)
    buildThreads.shutdown()
  }

  val baseOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir),
      keepDiagnostics = true
    )
  )

  private def normalizeContents(contents: String): String =
    contents
      .replaceAll(
        "ivy:file:[^\"]*scala-cli-tests-extra-repo[^\"]*/local-repo[^\"]*",
        "ivy:file:.../scala-cli-tests-extra-repo/local-repo/..."
      )
      .replaceAll(
        "ivy:file:[^\"]*\\.ivy2/local[^\"]*",
        "ivy:file:.../.ivy2/local/"
      ).linesWithSeparators
      .filterNot(_.stripLeading().startsWith("/**"))
      .mkString

  def initializeGit(
    cwd: os.Path,
    tag: String = "test-inputs",
    gitUserName: String = "testUser",
    gitUserEmail: String = "testUser@scala-cli-tests.com"
  ): Unit = {
    println(s"Initializing git in $cwd...")
    os.proc("git", "init").call(cwd = cwd)
    println(s"Setting git user.name to $gitUserName")
    os.proc("git", "config", "--local", "user.name", gitUserName).call(cwd = cwd)
    println(s"Setting git user.email to $gitUserEmail")
    os.proc("git", "config", "--local", "user.email", gitUserEmail).call(cwd = cwd)
    println(s"Adding $cwd to git...")
    os.proc("git", "add", ".").call(cwd = cwd)
    println(s"Doing an initial commit...")
    os.proc("git", "commit", "-m", "git init test inputs").call(cwd = cwd)
    println(s"Tagging as $tag...")
    os.proc("git", "tag", tag).call(cwd = cwd)
    println(s"Git initialized at $cwd")
  }

  test("BuildInfo source generated") {
    val inputs = TestInputs(
      os.rel / "main.scala" ->
        """//> using dep com.lihaoyi::os-lib:0.9.1
          |//> using option -Xasync
          |//> using plugin org.wartremover:::wartremover:3.0.9
          |//> using scala 3.2.2
          |//> using jvm 11
          |//> using mainClass Main
          |//> using resourceDir ./resources
          |//> using jar TEST1.jar TEST2.jar
          |
          |//> using buildInfo
          |
          |import scala.cli.build.BuildInfo
          |
          |object Main extends App {
          |  println(s"Scala version: ${BuildInfo.scalaVersion}")
          |  BuildInfo.Main.customJarsDecls.foreach(println)
          |}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      initializeGit(root, "v1.0.0")

      inputs.copy(forceCwd = Some(root))
        .withBuild(baseOptions, buildThreads, bloopConfigOpt, skipCreatingSources = true) {
          (root, _, maybeBuild) =>
            expect(maybeBuild.orThrow.success)
            val projectDir = os.list(root / ".scala-build").filter(
              _.baseName.startsWith(root.baseName + "_")
            )
            expect(projectDir.size == 1)
            val buildInfoPath = projectDir.head / "src_generated" / "main" / "BuildInfo.scala"
            expect(os.isFile(buildInfoPath))

            val buildInfoContent = os.read(buildInfoPath)

            assertNoDiff(
              normalizeContents(buildInfoContent),
              s"""package scala.cli.build
                 |
                 |object BuildInfo {
                 |  val scalaVersion = "3.2.2"
                 |  val platform = "JVM"
                 |  val jvmVersion = Some("11")
                 |  val scalaJsVersion = None
                 |  val jsEsVersion = None
                 |  val scalaNativeVersion = None
                 |  val mainClass = Some("Main")
                 |  val projectVersion = Some("1.0.0")
                 |
                 |  object Main {
                 |    val sources = Seq("${root / "main.scala"}")
                 |    val scalacOptions = Seq("-Xasync")
                 |    val scalaCompilerPlugins = Seq("org.wartremover:wartremover_3.2.2:3.0.9")
                 |    val dependencies = Seq("com.lihaoyi:os-lib_3:0.9.1")
                 |    val resolvers = Seq("ivy:file:.../scala-cli-tests-extra-repo/local-repo/...", "https://repo1.maven.org/maven2", "ivy:file:.../.ivy2/local/")
                 |    val resourceDirs = Seq("${root / "resources"}")
                 |    val customJarsDecls = Seq("${root / "TEST1.jar"}", "${root / "TEST2.jar"}")
                 |  }
                 |
                 |  object Test {
                 |    val sources = Nil
                 |    val scalacOptions = Nil
                 |    val scalaCompilerPlugins = Nil
                 |    val dependencies = Nil
                 |    val resolvers = Nil
                 |    val resourceDirs = Nil
                 |    val customJarsDecls = Nil
                 |  }
                 |}
                 |""".stripMargin
            )
        }
    }

  }

  test("BuildInfo for native") {
    val inputs = TestInputs(
      os.rel / "main.scala" ->
        s"""//> using dep "com.lihaoyi::os-lib:0.9.1"
           |//> using option "-Xasync"
           |//> using plugin "org.wartremover:::wartremover:3.0.9"
           |//> using scala 3.2.2
           |//> using jvm 11
           |//> using mainClass "Main"
           |//> using resourceDir ./resources
           |//> using jar TEST1.jar TEST2.jar
           |//> using platform scala-native
           |//> using nativeVersion 0.4.6
           |
           |//> using buildInfo
           |
           |import scala.cli.build.BuildInfo
           |
           |object Main extends App {
           |  println(s"Scala version: $${BuildInfo.scalaVersion}")
           |  BuildInfo.Main.customJarsDecls.foreach(println)
           |}
           |""".stripMargin
    )

    inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (root, _, maybeBuild) =>
        expect(maybeBuild.orThrow.success)
        val projectDir = os.list(root / ".scala-build").filter(
          _.baseName.startsWith(root.baseName + "_")
        )
        expect(projectDir.size == 1)
        val buildInfoPath = projectDir.head / "src_generated" / "main" / "BuildInfo.scala"
        expect(os.isFile(buildInfoPath))

        val buildInfoContent = os.read(buildInfoPath)

        assertNoDiff(
          normalizeContents(buildInfoContent),
          s"""package scala.cli.build
             |
             |object BuildInfo {
             |  val scalaVersion = "3.2.2"
             |  val platform = "Native"
             |  val jvmVersion = None
             |  val scalaJsVersion = None
             |  val jsEsVersion = None
             |  val scalaNativeVersion = Some("0.4.6")
             |  val mainClass = Some("Main")
             |  val projectVersion = None
             |
             |  object Main {
             |    val sources = Seq("${root / "main.scala"}")
             |    val scalacOptions = Seq("-Xasync")
             |    val scalaCompilerPlugins = Seq("org.wartremover:wartremover_3.2.2:3.0.9")
             |    val dependencies = Seq("com.lihaoyi:os-lib_3:0.9.1")
             |    val resolvers = Seq("ivy:file:.../scala-cli-tests-extra-repo/local-repo/...", "https://repo1.maven.org/maven2", "ivy:file:.../.ivy2/local/")
             |    val resourceDirs = Seq("${root / "resources"}")
             |    val customJarsDecls = Seq("${root / "TEST1.jar"}", "${root / "TEST2.jar"}")
             |  }
             |
             |  object Test {
             |    val sources = Nil
             |    val scalacOptions = Nil
             |    val scalaCompilerPlugins = Nil
             |    val dependencies = Nil
             |    val resolvers = Nil
             |    val resourceDirs = Nil
             |    val customJarsDecls = Nil
             |  }
             |}
             |""".stripMargin
        )
    }
  }

  test("BuildInfo for js") {
    val inputs = TestInputs(
      os.rel / "main.scala" ->
        s"""//> using dep "com.lihaoyi::os-lib:0.9.1"
           |//> using option "-Xasync"
           |//> using plugin "org.wartremover:::wartremover:3.0.9"
           |//> using scala 3.2.2
           |//> using jvm 11
           |//> using mainClass "Main"
           |//> using resourceDir ./resources
           |//> using jar TEST1.jar TEST2.jar
           |//> using platform scala-js
           |//> using jsVersion 1.13.1
           |//> using jsEsVersionStr es2015
           |//> using computeVersion "command:echo TestVersion"
           |
           |//> using buildInfo
           |
           |import scala.cli.build.BuildInfo
           |
           |object Main extends App {
           |  println(s"Scala version: $${BuildInfo.scalaVersion}")
           |  BuildInfo.Main.customJarsDecls.foreach(println)
           |}
           |""".stripMargin
    )

    inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (root, _, maybeBuild) =>
        expect(maybeBuild.orThrow.success)
        val projectDir = os.list(root / ".scala-build").filter(
          _.baseName.startsWith(root.baseName + "_")
        )
        expect(projectDir.size == 1)
        val buildInfoPath = projectDir.head / "src_generated" / "main" / "BuildInfo.scala"
        expect(os.isFile(buildInfoPath))

        val buildInfoContent = os.read(buildInfoPath)

        assertNoDiff(
          normalizeContents(buildInfoContent),
          s"""package scala.cli.build
             |
             |object BuildInfo {
             |  val scalaVersion = "3.2.2"
             |  val platform = "JS"
             |  val jvmVersion = None
             |  val scalaJsVersion = Some("1.13.1")
             |  val jsEsVersion = Some("es2015")
             |  val scalaNativeVersion = None
             |  val mainClass = Some("Main")
             |  val projectVersion = Some("TestVersion")
             |
             |  object Main {
             |    val sources = Seq("${root / "main.scala"}")
             |    val scalacOptions = Seq("-Xasync")
             |    val scalaCompilerPlugins = Seq("org.wartremover:wartremover_3.2.2:3.0.9")
             |    val dependencies = Seq("com.lihaoyi:os-lib_3:0.9.1")
             |    val resolvers = Seq("ivy:file:.../scala-cli-tests-extra-repo/local-repo/...", "https://repo1.maven.org/maven2", "ivy:file:.../.ivy2/local/")
             |    val resourceDirs = Seq("${root / "resources"}")
             |    val customJarsDecls = Seq("${root / "TEST1.jar"}", "${root / "TEST2.jar"}")
             |  }
             |
             |  object Test {
             |    val sources = Nil
             |    val scalacOptions = Nil
             |    val scalaCompilerPlugins = Nil
             |    val dependencies = Nil
             |    val resolvers = Nil
             |    val resourceDirs = Nil
             |    val customJarsDecls = Nil
             |  }
             |}
             |""".stripMargin
        )
    }
  }

  test("BuildInfo for Scala 2") {
    val inputs = TestInputs(
      os.rel / "main.scala" ->
        s"""//> using dep "com.lihaoyi::os-lib:0.9.1"
           |//> using option "-Xasync"
           |//> using plugin "org.wartremover:::wartremover:3.0.9"
           |//> using scala 2.13.6
           |//> using jvm 11
           |//> using mainClass "Main"
           |//> using resourceDir ./resources
           |//> using jar TEST1.jar TEST2.jar
           |//> using computeVersion "command:echo TestVersion"
           |
           |//> using buildInfo
           |
           |import scala.cli.build.BuildInfo
           |
           |object Main extends App {
           |  println(s"Scala version: $${BuildInfo.scalaVersion}")
           |  BuildInfo.Main.customJarsDecls.foreach(println)
           |}
           |""".stripMargin
    )

    inputs.withBuild(baseOptions, buildThreads, bloopConfigOpt) {
      (root, _, maybeBuild) =>
        expect(maybeBuild.orThrow.success)
        val projectDir = os.list(root / ".scala-build").filter(
          _.baseName.startsWith(root.baseName + "_")
        )
        expect(projectDir.size == 1)
        val buildInfoPath = projectDir.head / "src_generated" / "main" / "BuildInfo.scala"
        expect(os.isFile(buildInfoPath))

        val buildInfoContent = os.read(buildInfoPath)

        assertNoDiff(
          normalizeContents(buildInfoContent),
          s"""package scala.cli.build
             |
             |object BuildInfo {
             |  val scalaVersion = "2.13.6"
             |  val platform = "JVM"
             |  val jvmVersion = Some("11")
             |  val scalaJsVersion = None
             |  val jsEsVersion = None
             |  val scalaNativeVersion = None
             |  val mainClass = Some("Main")
             |  val projectVersion = Some("TestVersion")
             |
             |  object Main {
             |    val sources = Seq("${root / "main.scala"}")
             |    val scalacOptions = Seq("-Xasync")
             |    val scalaCompilerPlugins = Seq("org.wartremover:wartremover_2.13.6:3.0.9")
             |    val dependencies = Seq("com.lihaoyi:os-lib_2.13:0.9.1")
             |    val resolvers = Seq("ivy:file:.../scala-cli-tests-extra-repo/local-repo/...", "https://repo1.maven.org/maven2", "ivy:file:.../.ivy2/local/")
             |    val resourceDirs = Seq("${root / "resources"}")
             |    val customJarsDecls = Seq("${root / "TEST1.jar"}", "${root / "TEST2.jar"}")
             |  }
             |
             |  object Test {
             |    val sources = Nil
             |    val scalacOptions = Nil
             |    val scalaCompilerPlugins = Nil
             |    val dependencies = Nil
             |    val resolvers = Nil
             |    val resourceDirs = Nil
             |    val customJarsDecls = Nil
             |  }
             |}
             |""".stripMargin
        )
    }
  }
}
