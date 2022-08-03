package scala.cli.integration

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j as b
import com.eed3si9n.expecty.Expecty.expect
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError

import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.{ExecutorService, ScheduledExecutorService}

import scala.annotation.tailrec
import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future, Promise}
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import scala.util.{Failure, Properties, Success, Try}

abstract class BspTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  import BspTestDefinitions.*

  def initParams(root: os.Path): b.InitializeBuildParams =
    new b.InitializeBuildParams(
      "Scala CLI ITs",
      "0",
      Constants.bspVersion,
      root.toNIO.toUri.toASCIIString,
      new b.BuildClientCapabilities(List("java", "scala").asJava)
    )

  val pool: ExecutorService               = TestUtil.threadPool("bsp-tests-jsonrpc", 4)
  val scheduler: ScheduledExecutorService = TestUtil.scheduler("bsp-tests-scheduler")

  def completeIn(duration: FiniteDuration): Future[Unit] = {
    val p = Promise[Unit]()
    scheduler.schedule(
      new Runnable {
        def run(): Unit =
          try p.success(())
          catch {
            case t: Throwable =>
              System.err.println(s"Caught $t while trying to complete timer, ignoring it")
          }
      },
      duration.length,
      duration.unit
    )
    p.future
  }

  override def afterAll(): Unit = {
    pool.shutdown()
  }

  private def extractMainTargets(targets: Seq[BuildTargetIdentifier]): BuildTargetIdentifier =
    targets.collectFirst {
      case t if !t.getUri.contains("-test") => t
    }.get

  private def extractTestTargets(targets: Seq[BuildTargetIdentifier]): BuildTargetIdentifier =
    targets.collectFirst {
      case t if t.getUri.contains("-test") => t
    }.get

  def withBsp[T](
    inputs: TestInputs,
    args: Seq[String],
    attempts: Int = if (TestUtil.isCI) 3 else 1,
    pauseDuration: FiniteDuration = 5.seconds,
    bspOptions: List[String] = List.empty,
    reuseRoot: Option[os.Path] = None
  )(
    f: (
      os.Path,
      TestBspClient,
      b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer
    ) => Future[T]
  ): T = {

    def attempt(): Try[T] = Try {
      val root = reuseRoot.getOrElse(inputs.root())

      val proc = os.proc(TestUtil.cli, "bsp", bspOptions ++ extraOptions, args)
        .spawn(cwd = root)
      var remoteServer: b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer = null

      try {
        val (localClient, remoteServer0, _) =
          TestBspClient.connect(proc.stdout, proc.stdin, pool)
        remoteServer = remoteServer0
        Await.result(remoteServer.buildInitialize(initParams(root)).asScala, Duration.Inf)
        Await.result(f(root, localClient, remoteServer), Duration.Inf)
      }
      finally {
        if (remoteServer != null)
          try Await.result(remoteServer.buildShutdown().asScala, 20.seconds)
          catch {
            case NonFatal(e) =>
              System.err.println(s"Ignoring $e while shutting down BSP server")
          }
        proc.join(2.seconds.toMillis)
        proc.destroy()
        proc.join(2.seconds.toMillis)
        proc.destroyForcibly()
      }
    }

    @tailrec
    def helper(count: Int): T =
      attempt() match {
        case Success(t) => t
        case Failure(ex) =>
          if (count <= 1)
            throw new Exception(ex)
          else {
            System.err.println(s"Caught $ex, trying again in $pauseDuration…")
            Thread.sleep(pauseDuration.toMillis)
            helper(count - 1)
          }
      }

    helper(attempts)
  }

  def checkTargetUri(root: os.Path, uri: String): Unit = {
    val baseUri =
      TestUtil.normalizeUri((root / Constants.workspaceDirName).toNIO.toUri.toASCIIString)
        .stripSuffix("/")
    val expectedPrefixes = Set(
      baseUri + "?id=",
      baseUri + "/?id="
    )
    expect(expectedPrefixes.exists(uri.startsWith))
  }

  private def readBspConfig(root: os.Path): Details = {
    val bspFile = root / ".bsp" / "scala-cli.json"
    expect(os.isFile(bspFile))
    val content = os.read.bytes(bspFile)
    // check that we can decode the connection details
    readFromArray(content)(detailsCodec)
  }

  test("setup-ide") {
    val inputs = TestInputs(
      os.rel / "simple.sc" ->
        s"""val msg = "Hello"
           |println(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "setup-ide", ".", extraOptions).call(cwd = root, stdout = os.Inherit)
      val details = readBspConfig(root)
      expect(details.argv.length >= 2)
      expect(details.argv(1) == "bsp")
    }
  }

  for (command <- Seq("setup-ide", "compile", "run"))
    test(command + " should result in generated bsp file") {
      val inputs = TestInputs(
        os.rel / "simple.sc" ->
          s"""val msg = "Hello"
             |println(msg)
             |""".stripMargin
      )
      inputs.fromRoot { root =>
        os.proc(TestUtil.cli, command, ".", extraOptions).call(cwd = root, stdout = os.Inherit)
        val details                = readBspConfig(root)
        val expectedIdeOptionsFile = root / Constants.workspaceDirName / "ide-options-v2.json"
        val expectedIdeInputsFile  = root / Constants.workspaceDirName / "ide-inputs.json"
        val expectedArgv = Seq(
          TestUtil.cliPath,
          "bsp",
          "--json-options",
          expectedIdeOptionsFile.toString,
          root.toString
        )
        expect(details.argv == expectedArgv)
        expect(os.isFile(expectedIdeOptionsFile))
        expect(os.isFile(expectedIdeInputsFile))
      }
    }

  val importPprintOnlyProject: TestInputs = TestInputs(
    os.rel / "simple.sc" -> s"import $$ivy.`com.lihaoyi::pprint:${Constants.pprintVersion}`"
  )

  test("setup-ide should have only absolute paths even if relative ones were specified") {
    val path = os.rel / "directory" / "simple.sc"
    val inputs =
      TestInputs(path -> s"import $$ivy.`com.lihaoyi::pprint:${Constants.pprintVersion}`")
    inputs.fromRoot { root =>
      val relativeCliCommand = TestUtil.cliCommand(
        TestUtil.relPathStr(os.Path(TestUtil.cliPath).relativeTo(root))
      )

      val proc =
        if (Properties.isWin && TestUtil.isNativeCli)
          os.proc(
            "cmd",
            "/c",
            (relativeCliCommand ++ Seq("setup-ide", path.toString) ++ extraOptions)
              .mkString(" ")
          )
        else
          os.proc(relativeCliCommand, "setup-ide", path, extraOptions)
      proc.call(cwd = root, stdout = os.Inherit)

      val details = readBspConfig(root / "directory")
      val expectedArgv = List(
        TestUtil.cliPath,
        "bsp",
        "--json-options",
        (root / "directory" / Constants.workspaceDirName / "ide-options-v2.json").toString,
        (root / "directory" / "simple.sc").toString
      )
      expect(details.argv == expectedArgv)
    }
  }

  test("setup-ide should succeed for valid dependencies") {
    importPprintOnlyProject.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "setup-ide",
        ".",
        extraOptions,
        "--dependency",
        s"org.scalameta::munit:${Constants.munitVersion}"
      ).call(cwd = root)
    }
  }

  test("setup-ide should fail for invalid dependencies") {
    importPprintOnlyProject.fromRoot { root =>

      val p = os.proc(
        TestUtil.cli,
        "setup-ide",
        ".",
        extraOptions,
        "--dependency",
        "org.scalameta::munit:0.7.119999"
      ).call(cwd = root, check = false, stderr = os.Pipe)
      expect(p.err.text().contains(s"Error downloading org.scalameta:munit"))
      expect(p.exitCode == 1)
    }
  }

  test("simple") {
    val inputs = TestInputs(
      os.rel / "simple.sc" ->
        s"""val msg = "Hello"
           |println(msg)
           |""".stripMargin
    )

    withBsp(inputs, Seq(".")) { (root, _, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
          expect(targets.length == 2)
          extractMainTargets(targets)
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        {
          val resp = await {
            remoteServer
              .buildTargetDependencySources(new b.DependencySourcesParams(targets))
              .asScala
          }
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundDepSources = resp.getItems.asScala
            .flatMap(_.getSources.asScala)
            .toSeq
            .map { uri =>
              val idx = uri.lastIndexOf('/')
              uri.drop(idx + 1)
            }
          if (actualScalaVersion.startsWith("2.")) {
            expect(foundDepSources.length == 1)
            expect(foundDepSources.forall(_.startsWith("scala-library-")))
          }
          else {
            expect(foundDepSources.length == 2)
            expect(foundDepSources.exists(_.startsWith("scala-library-")))
            expect(foundDepSources.exists(_.startsWith("scala3-library_3-3")))
          }
          expect(foundDepSources.forall(_.endsWith("-sources.jar")))
        }

        {
          val resp = await(remoteServer.buildTargetSources(new b.SourcesParams(targets)).asScala)
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundSources = resp.getItems.asScala
            .map(_.getSources.asScala.map(_.getUri).toSeq)
            .toSeq
            .map(_.map(TestUtil.normalizeUri))
          val expectedSources = Seq(
            Seq(
              TestUtil.normalizeUri((root / "simple.sc").toNIO.toUri.toASCIIString)
            )
          )
          expect(foundSources == expectedSources)
        }

        val scalacOptionsResp = {
          val resp = await {
            remoteServer
              .buildTargetScalacOptions(new b.ScalacOptionsParams(targets))
              .asScala
          }
          val foundTargets = resp
            .getItems
            .asScala
            .map(_.getTarget.getUri)
            .map(TestUtil.normalizeUri)
          expect(foundTargets == Seq(targetUri))
          val foundOptions = resp.getItems.asScala.flatMap(_.getOptions.asScala).toSeq
          if (actualScalaVersion.startsWith("2."))
            expect(foundOptions.exists { opt =>
              opt.startsWith("-Xplugin:") && opt.contains("semanticdb-scalac")
            })
          else
            expect(foundOptions.contains("-Xsemanticdb"))
          resp
        }

        {
          val resp = await {
            remoteServer.buildTargetJavacOptions(new b.JavacOptionsParams(targets)).asScala
          }
          val foundTargets = resp
            .getItems
            .asScala
            .map(_.getTarget.getUri)
            .map(TestUtil.normalizeUri)
          expect(foundTargets == Seq(targetUri))
        }

        val classDir = os.Path(
          Paths.get(new URI(scalacOptionsResp.getItems.asScala.head.getClassDirectory))
        )

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        val compileProducts = os.walk(classDir).filter(os.isFile(_)).map(_.relativeTo(classDir))

        expect(compileProducts.contains(os.rel / "simple.class"))
        expect(
          compileProducts.contains(os.rel / "META-INF" / "semanticdb" / "simple.sc.semanticdb")
        )
      }
    }
  }

  test("diagnostics") {
    val inputs = TestInputs(
      os.rel / "Test.scala" ->
        s"""object Test {
           |  val msg = "Hello"
           |  zz
           |  println(msg)
           |}
           |""".stripMargin
    )

    withBsp(inputs, Seq(".")) { (root, localClient, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
          expect(targets.length == 2)
          extractMainTargets(targets)
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        val compileResp = await {
          remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala
        }
        expect(compileResp.getStatusCode == b.StatusCode.ERROR)

        val diagnosticsParams: b.PublishDiagnosticsParams =
          extractDiagnosticsParams(root / "Test.scala", localClient)
        expect(diagnosticsParams.getBuildTarget.getUri == targetUri)

        val diagnostics = diagnosticsParams.getDiagnostics.asScala.toSeq
        expect(diagnostics.length == 1)

        val (expectedMessage, expectedEndCharacter) =
          if (actualScalaVersion.startsWith("2."))
            "not found: value zz" -> 4
          else if (actualScalaVersion == "3.0.0")
            "Not found: zz" -> 2
          else
            "Not found: zz" -> 4
        checkDiagnostic(
          diagnostic = diagnostics.head,
          expectedMessage = expectedMessage,
          expectedSeverity = b.DiagnosticSeverity.ERROR,
          expectedStartLine = 2,
          expectedStartCharacter = 2,
          expectedEndLine = 2,
          expectedEndCharacter = expectedEndCharacter
        )
      }
    }
  }

  test("diagnostics in script") {
    val inputs = TestInputs(
      os.rel / "test.sc" ->
        s"""val msg = "Hello"
           |zz
           |println(msg)
           |""".stripMargin
    )

    withBsp(inputs, Seq(".")) { (root, localClient, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
          expect(targets.length == 2)
          extractMainTargets(targets)
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        val compileResp = await {
          remoteServer
            .buildTargetCompile(new b.CompileParams(targets))
            .asScala
        }
        expect(compileResp.getStatusCode == b.StatusCode.ERROR)

        val diagnosticsParams = {
          val diagnostics = localClient.diagnostics()
          val params      = diagnostics(2)
          expect(params.getBuildTarget.getUri == targetUri)
          expect(
            TestUtil.normalizeUri(params.getTextDocument.getUri) ==
              TestUtil.normalizeUri((root / "test.sc").toNIO.toUri.toASCIIString)
          )
          params
        }

        val diagnostics = diagnosticsParams.getDiagnostics.asScala.toSeq
        expect(diagnostics.length == 1)

        val (expectedMessage, expectedEndCharacter) =
          if (actualScalaVersion.startsWith("2."))
            "not found: value zz" -> 2
          else if (actualScalaVersion == "3.0.0")
            "Not found: zz" -> 0
          else
            "Not found: zz" -> 2
        checkDiagnostic(
          diagnostic = diagnostics.head,
          expectedMessage = expectedMessage,
          expectedSeverity = b.DiagnosticSeverity.ERROR,
          expectedStartLine = 1,
          expectedStartCharacter = 0,
          expectedEndLine = 1,
          expectedEndCharacter = expectedEndCharacter
        )
      }
    }
  }

  test("invalid diagnostics at startup") {
    val inputs = TestInputs(
      os.rel / "A.scala" ->
        s"""//> using resource "./resources"
           |
           |object A {}
           |""".stripMargin
    )

    withBsp(inputs, Seq(".")) { (_, localClient, remoteServer) =>
      async {
        await(remoteServer.workspaceBuildTargets().asScala)

        val diagnosticsParams = localClient.latestDiagnostics().getOrElse {
          fail("No diagnostics found")
        }

        checkDiagnostic(
          diagnostic = diagnosticsParams.getDiagnostics.asScala.toSeq.head,
          expectedMessage = "Unrecognized directive: resource with values: ./resources",
          expectedSeverity = b.DiagnosticSeverity.ERROR,
          expectedStartLine = 0,
          expectedStartCharacter = 20,
          expectedEndLine = 0,
          expectedEndCharacter = 31,
          strictlyCheckMessage = false
        )
      }
    }
  }

  test("directive diagnostics") {
    val inputs = TestInputs(
      os.rel / "Test.scala" ->
        s"""//> using lib "com.lihaoyi::pprint:0.0.0.0.0.1"
           |
           |object Test {
           |  val msg = "Hello"
           |  println(msg)
           |}
           |""".stripMargin
    )

    withBsp(inputs, Seq(".")) { (root, localClient, remoteServer) =>
      async {
        await(remoteServer.workspaceBuildTargets().asScala)
        val diagnosticsParams = extractDiagnosticsParams(root / "Test.scala", localClient)

        val diagnostics = diagnosticsParams.getDiagnostics.asScala.toSeq
        expect(diagnostics.length == 1)

        val sbv =
          if (actualScalaVersion.startsWith("2.12.")) "2.12"
          else if (actualScalaVersion.startsWith("2.13.")) "2.13"
          else if (actualScalaVersion.startsWith("3.")) "3"
          else ???
        val expectedMessage = s"Error downloading com.lihaoyi:pprint_$sbv:0.0.0.0.0.1"
        checkDiagnostic(
          diagnostic = diagnostics.head,
          expectedMessage = expectedMessage,
          expectedSeverity = b.DiagnosticSeverity.ERROR,
          expectedStartLine = 0,
          expectedStartCharacter = 15,
          expectedEndLine = 0,
          expectedEndCharacter = 46,
          strictlyCheckMessage = false
        )
      }
    }
  }

  test("workspace update") {
    val inputs = TestInputs(
      os.rel / "simple.sc" ->
        s"""val msg = "Hello"
           |println(msg)
           |""".stripMargin
    )
    val extraArgs =
      if (Properties.isWin) Seq("-v", "-v", "-v")
      else Nil

    withBsp(inputs, Seq(".") ++ extraArgs) { (root, localClient, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
          expect(targets.length == 2)
          extractMainTargets(targets)
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        {
          val resp = await {
            remoteServer
              .buildTargetDependencySources(new b.DependencySourcesParams(targets))
              .asScala
          }
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundDepSources = resp.getItems.asScala
            .flatMap(_.getSources.asScala)
            .toSeq
            .map { uri =>
              val idx = uri.lastIndexOf('/')
              uri.drop(idx + 1)
            }
          if (actualScalaVersion.startsWith("2.")) {
            expect(foundDepSources.length == 1)
            expect(foundDepSources.forall(_.startsWith("scala-library-")))
          }
          else {
            expect(foundDepSources.length == 2)
            expect(foundDepSources.exists(_.startsWith("scala-library-")))
            expect(foundDepSources.exists(_.startsWith("scala3-library_3-3")))
          }
          expect(foundDepSources.forall(_.endsWith("-sources.jar")))
        }

        val didChangeParamsFuture = localClient.buildTargetDidChange()
        val updatedContent =
          """//> using lib "com.lihaoyi::pprint:0.6.6"
            |val msg = "Hello"
            |pprint.log(msg)
            |""".stripMargin
        os.write.over(root / "simple.sc", updatedContent)

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        val didChangeParamsOptFuture = Future.firstCompletedOf(Seq(
          didChangeParamsFuture.map(Some(_)),
          completeIn(5.seconds).map(_ => None)
        ))
        val didChangeParams = await(didChangeParamsOptFuture).getOrElse {
          sys.error("No buildTargetDidChange notification received")
        }

        val changes = didChangeParams.getChanges.asScala.toSeq
        expect(changes.length == 2)

        val change = changes.head
        expect(change.getTarget.getUri == targetUri)
        expect(change.getKind == b.BuildTargetEventKind.CHANGED)

        {
          val resp = await {
            remoteServer
              .buildTargetDependencySources(new b.DependencySourcesParams(targets))
              .asScala
          }
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundDepSources = resp.getItems.asScala
            .flatMap(_.getSources.asScala)
            .toSeq
            .map { uri =>
              val idx = uri.lastIndexOf('/')
              uri.drop(idx + 1)
            }
          expect(foundDepSources.length > 1)
          expect(foundDepSources.forall(_.endsWith("-sources.jar")))
          expect(foundDepSources.exists(_.startsWith("scala-library-")))
          expect(foundDepSources.exists(_.startsWith("pprint_")))
          resp
        }
      }
    }
  }

  test("workspace update - new file") {
    val inputs = TestInputs(
      os.rel / "Test.scala" ->
        s"""object Test {
           |  val msg = "Hello"
           |  println(msg)
           |}
           |""".stripMargin
    )

    withBsp(inputs, Seq(".")) { (root, localClient, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
          expect(targets.length == 2)
          extractMainTargets(targets)
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        {
          val resp = await {
            remoteServer
              .buildTargetDependencySources(new b.DependencySourcesParams(targets))
              .asScala
          }
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundDepSources = resp.getItems.asScala
            .flatMap(_.getSources.asScala)
            .toSeq
            .map { uri =>
              val idx = uri.lastIndexOf('/')
              uri.drop(idx + 1)
            }
          if (actualScalaVersion.startsWith("2.")) {
            expect(foundDepSources.length == 1)
            expect(foundDepSources.forall(_.startsWith("scala-library-")))
          }
          else {
            expect(foundDepSources.length == 2)
            expect(foundDepSources.exists(_.startsWith("scala-library-")))
            expect(foundDepSources.exists(_.startsWith("scala3-library_3-3")))
          }
          expect(foundDepSources.forall(_.endsWith("-sources.jar")))
        }

        val didChangeParamsFuture = localClient.buildTargetDidChange()

        val newFileContent =
          """object Messages {
            |  def msg = "Hello"
            |}
            |""".stripMargin
        os.write(root / "Messages.scala", newFileContent)
        val updatedContent =
          """object Test {
            |  println(Messages.msg)
            |}
            |""".stripMargin
        os.write.over(root / "Test.scala", updatedContent)

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        val didChangeParamsOptFuture = Future.firstCompletedOf(Seq(
          didChangeParamsFuture.map(Some(_)),
          completeIn(5.seconds).map(_ => None)
        ))
        val didChangeParams = await(didChangeParamsOptFuture).getOrElse {
          sys.error("No buildTargetDidChange notification received")
        }

        val changes = didChangeParams.getChanges.asScala.toSeq
        expect(changes.length == 2)

        val change = changes.head
        expect(change.getTarget.getUri == targetUri)
        expect(change.getKind == b.BuildTargetEventKind.CHANGED)
      }
    }
  }

  test("test workspace update after adding file to main scope") {
    val inputs = TestInputs(
      os.rel / "Messages.scala" ->
        """//> using lib "com.lihaoyi::os-lib:0.7.8"
          |object Messages {
          |  def msg = "Hello"
          |}
          |""".stripMargin,
      os.rel / "MyTests.test.scala" ->
        """//> using lib "com.lihaoyi::utest::0.7.10"
          |import utest._
          |
          |object MyTests extends TestSuite {
          |  val tests = Tests {
          |    test("foo") {
          |      assert(Messages.msg == "Hello")
          |    }
          |  }
          |}
          |""".stripMargin
    )

    withBsp(inputs, Seq(".")) { (root, localClient, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
          expect(targets.length == 2)
          extractTestTargets(targets)
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        {
          val resp = await {
            remoteServer
              .buildTargetDependencySources(new b.DependencySourcesParams(targets))
              .asScala
          }
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundDepSources = resp.getItems.asScala
            .flatMap(_.getSources.asScala)
            .toSeq
            .map { uri =>
              val idx = uri.lastIndexOf('/')
              uri.drop(idx + 1)
            }

          if (actualScalaVersion.startsWith("2.13")) {
            expect(foundDepSources.exists(_.startsWith("utest_2.13-0.7.10")))
            expect(foundDepSources.exists(_.startsWith("os-lib_2.13-0.7.8")))
          }
          else if (actualScalaVersion.startsWith("2.12")) {
            expect(foundDepSources.exists(_.startsWith("utest_2.12-0.7.10")))
            expect(foundDepSources.exists(_.startsWith("os-lib_2.12-0.7.8")))
          }
          else {
            expect(foundDepSources.exists(_.startsWith("utest_3-0.7.10")))
            expect(foundDepSources.exists(_.startsWith("os-lib_3-0.7.8")))
          }

          expect(foundDepSources.exists(_.startsWith("test-interface-1.0")))
          expect(foundDepSources.forall(_.endsWith("-sources.jar")))
        }

        localClient.buildTargetDidChange()

        val newFileContent =
          """object Messages {
            |  def msg = "Hello2"
            |}
            |""".stripMargin
        os.write.over(root / "Messages.scala", newFileContent)

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }
      }
    }
  }

  test("using directive") {
    val inputs = TestInputs(
      os.rel / "test.sc" ->
        s"""// using scala "3.0"
           |println(123)""".stripMargin
    )
    withBsp(inputs, Seq(".")) { (_, localClient, remoteServer) =>
      async {
        // prepare build
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        // build code
        val targets = buildTargetsResp.getTargets.asScala.map(_.getId()).asJava
        await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)

        val visibleDiagnostics =
          localClient.diagnostics().takeWhile(!_.getReset).flatMap(_.getDiagnostics.asScala)

        expect(visibleDiagnostics.nonEmpty)
        visibleDiagnostics.foreach { d =>
          expect(
            d.getSeverity == b.DiagnosticSeverity.WARNING,
            d.getMessage.contains("deprecated"),
            d.getMessage.contains("directive")
          )
        }
      }
    }
  }

  test("workspace/reload --dependency option") {
    val inputs = TestInputs(
      os.rel / "ReloadTest.scala" ->
        s"""import os.pwd
           |object ReloadTest {
           |  println(pwd)
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "setup-ide", ".", extraOptions)
        .call(
          cwd = root,
          stdout = os.Inherit
        )
      val ideOptionsPath = root / Constants.workspaceDirName / "ide-options-v2.json"
      val jsonOptions    = List("--json-options", ideOptionsPath.toString)
      withBsp(inputs, Seq("."), bspOptions = jsonOptions, reuseRoot = Some(root)) {
        (_, _, remoteServer) =>
          async {
            val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
            val targets          = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq

            val resp =
              await(remoteServer.buildTargetCompile(new b.CompileParams(targets.asJava)).asScala)
            expect(resp.getStatusCode == b.StatusCode.ERROR)

            val dependencyOptions = List("--dependency", "com.lihaoyi::os-lib::0.8.0")
            os.proc(TestUtil.cli, "setup-ide", ".", dependencyOptions ++ extraOptions)
              .call(
                cwd = root,
                stdout = os.Inherit
              )

            val reloadResponse =
              extractWorkspaceReloadResponse(await(remoteServer.workspaceReload().asScala))
            expect(reloadResponse.isEmpty)

            val buildTargetsResp0 = await(remoteServer.workspaceBuildTargets().asScala)
            val targets0          = buildTargetsResp0.getTargets.asScala.map(_.getId).toSeq

            val resp0 =
              await(remoteServer.buildTargetCompile(new b.CompileParams(targets0.asJava)).asScala)
            expect(resp0.getStatusCode == b.StatusCode.OK)
          }
      }

    }
  }

  test("workspace/reload of an extra sources directory") {
    val dir1 = "dir1"
    val dir2 = "dir2"
    val inputs = TestInputs(
      os.rel / dir1 / "ReloadTest.scala" ->
        s"""object ReloadTest {
           |  val container = MissingCaseClass(value = "Hello")
           |  println(container.value)
           |}
           |""".stripMargin
    )
    val extraInputs = inputs.add(
      os.rel / dir2 / "MissingCaseClass.scala" -> "case class MissingCaseClass(value: String)"
    )
    extraInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "setup-ide", dir1, extraOptions)
        .call(
          cwd = root,
          stdout = os.Inherit
        )
      withBsp(inputs, Seq(dir1), reuseRoot = Some(root)) {
        (_, _, remoteServer) =>
          async {
            val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
            val targets          = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq

            val resp =
              await(remoteServer.buildTargetCompile(new b.CompileParams(targets.asJava)).asScala)
            expect(resp.getStatusCode == b.StatusCode.ERROR)

            os.proc(TestUtil.cli, "setup-ide", dir1, dir2, extraOptions)
              .call(
                cwd = root,
                stdout = os.Inherit
              )

            val reloadResponse =
              extractWorkspaceReloadResponse(await(remoteServer.workspaceReload().asScala))
            expect(reloadResponse.isEmpty)

            val buildTargetsResp0 = await(remoteServer.workspaceBuildTargets().asScala)
            val targets0          = buildTargetsResp0.getTargets.asScala.map(_.getId).toSeq

            val resp0 =
              await(remoteServer.buildTargetCompile(new b.CompileParams(targets0.asJava)).asScala)
            expect(resp0.getStatusCode == b.StatusCode.OK)
          }
      }

    }
  }

  test("workspace/reload error response when no inputs json present") {
    val inputs = TestInputs(
      os.rel / "ReloadTest.scala" ->
        s"""object ReloadTest {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    withBsp(inputs, Seq(".")) {
      (_, _, remoteServer) =>
        async {
          val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
          val targets          = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq

          val resp =
            await(remoteServer.buildTargetCompile(new b.CompileParams(targets.asJava)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)

          val Some(responseError) =
            extractWorkspaceReloadResponse(await(remoteServer.workspaceReload().asScala))
          expect(responseError.getCode == -32603)
          expect(responseError.getMessage.nonEmpty)
        }
    }

  }

  test("workspace/reload when updated source element in using directive") {
    val utilsFileName = "Utils.scala"
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""|//> using file "Utils.scala"
            |
            |object Hello extends App {
            |   println("Hello World")
            |}""".stripMargin,
      os.rel / utilsFileName ->
        s"""|object Utils {
            |  val hello = "Hello World"
            |}""".stripMargin
    )
    withBsp(inputs, Seq("Hello.scala")) { (root, _, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
          expect(targets.length == 2)
          extractMainTargets(targets)
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        val compileResp = await {
          remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala
        }
        expect(compileResp.getStatusCode == b.StatusCode.OK)

        // after reload compilation should fails, Utils.scala file contains invalid scala code
        val updatedUtilsFile =
          s"""|object Utils {
              |  val hello = "Hello World
              |}""".stripMargin
        os.write.over(root / utilsFileName, updatedUtilsFile)

        val buildTargetsResp0 = await(remoteServer.workspaceBuildTargets().asScala)
        val targets0          = buildTargetsResp0.getTargets.asScala.map(_.getId).toSeq

        val resp0 =
          await(remoteServer.buildTargetCompile(new b.CompileParams(targets0.asJava)).asScala)
        expect(resp0.getStatusCode == b.StatusCode.ERROR)
      }
    }
  }

  test("bloop projects are initialised properly for an invalid directive") {
    val inputs = TestInputs(
      os.rel / "InvalidUsingDirective.scala" ->
        s"""//> using scala 3.1.2
           |
           |object InvalidUsingDirective extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    withBsp(inputs, Seq(".")) {
      (root, localClient, remoteServer) =>
        async {
          checkIfBloopProjectIsInitialised(
            root,
            await(remoteServer.workspaceBuildTargets().asScala)
          )
          val diagnosticsParams =
            extractDiagnosticsParams(root / "InvalidUsingDirective.scala", localClient)
          val diagnostics = diagnosticsParams.getDiagnostics.asScala.toSeq
          expect(diagnostics.length == 1)
          checkDiagnostic(
            diagnostic = diagnostics.head,
            expectedMessage =
              "Expected new line after the using directive, in the line; but found number literal: .2",
            expectedSeverity = b.DiagnosticSeverity.ERROR,
            expectedStartLine = 0,
            expectedStartCharacter = 19,
            expectedEndLine = 0,
            expectedEndCharacter = 19
          )
        }
    }
  }

  test("bloop projects are initialised properly for a directive for an unfetchable dependency") {
    val inputs = TestInputs(
      os.rel / "InvalidUsingDirective.scala" ->
        s"""//> using lib "no::lib:123"
           |
           |object InvalidUsingDirective extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    withBsp(inputs, Seq(".")) {
      (root, localClient, remoteServer) =>
        async {
          checkIfBloopProjectIsInitialised(
            root,
            await(remoteServer.workspaceBuildTargets().asScala)
          )
          val diagnosticsParams =
            extractDiagnosticsParams(root / "InvalidUsingDirective.scala", localClient)
          val diagnostics = diagnosticsParams.getDiagnostics.asScala.toSeq
          expect(diagnostics.length == 1)
          checkDiagnostic(
            diagnostic = diagnostics.head,
            expectedMessage = "Error downloading no:lib",
            expectedSeverity = b.DiagnosticSeverity.ERROR,
            expectedStartLine = 0,
            expectedStartCharacter = 15,
            expectedEndLine = 0,
            expectedEndCharacter = 26,
            strictlyCheckMessage = false
          )
        }
    }
  }
  test("bsp should report actionable diagnostic when enabled") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          s"""//> using lib "com.lihaoyi::os-lib:0.7.8"
             |
             |object Hello extends App {
             |  println("Hello")
             |}
             |""".stripMargin
      )
    )
    withBsp(inputs, Seq(".", "--actions")) {
      (_, localClient, remoteServer) =>
        async {
          // prepare build
          val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
          // build code
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId()).asJava
          await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)

          val visibleDiagnostics =
            localClient.diagnostics().takeWhile(!_.getReset).flatMap(_.getDiagnostics.asScala)

          expect(visibleDiagnostics.nonEmpty)
          expect(visibleDiagnostics.length == 1)

          val updateActionableDiagnostic = visibleDiagnostics.head
          
          checkDiagnostic(
            diagnostic = updateActionableDiagnostic,
            expectedMessage = "com.lihaoyi::os-lib:0.7.8 is outdated",
            expectedSeverity = b.DiagnosticSeverity.HINT,
            expectedStartLine = 0,
            expectedStartCharacter = 15,
            expectedEndLine = 0,
            expectedEndCharacter = 40,
            strictlyCheckMessage = false
          )
        }
    }
  }
  private def checkIfBloopProjectIsInitialised(
    root: os.Path,
    buildTargetsResp: b.WorkspaceBuildTargetsResult
  ): Unit = {
    val targets = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
    expect(targets.length == 2)

    val bloopProjectNames = targets.map { target =>
      val targetUri = TestUtil.normalizeUri(target.getUri)
      checkTargetUri(root, targetUri)
      new URI(targetUri).getQuery.stripPrefix("id=")
    }

    val bloopDir = root / Constants.workspaceDirName / ".bloop"
    expect(os.isDir(bloopDir))

    bloopProjectNames.foreach { bloopProjectName =>
      val bloopProjectJsonPath = bloopDir / s"$bloopProjectName.json"
      expect(os.isFile(bloopProjectJsonPath))
    }
  }

  private def extractDiagnosticsParams(
    relevantFilePath: os.Path,
    localClient: TestBspClient
  ): b.PublishDiagnosticsParams = {
    val params = localClient.latestDiagnostics().getOrElse {
      sys.error("No diagnostics found")
    }
    expect {
      TestUtil.normalizeUri(params.getTextDocument.getUri) == TestUtil.normalizeUri(
        relevantFilePath.toNIO.toUri.toASCIIString
      )
    }
    params
  }

  private def checkDiagnostic(
    diagnostic: b.Diagnostic,
    expectedMessage: String,
    expectedSeverity: b.DiagnosticSeverity,
    expectedStartLine: Int,
    expectedStartCharacter: Int,
    expectedEndLine: Int,
    expectedEndCharacter: Int,
    strictlyCheckMessage: Boolean = true
  ): Unit = {
    expect(diagnostic.getSeverity == expectedSeverity)
    expect(diagnostic.getRange.getStart.getLine == expectedStartLine)
    expect(diagnostic.getRange.getStart.getCharacter == expectedStartCharacter)
    expect(diagnostic.getRange.getEnd.getLine == expectedEndLine)
    expect(diagnostic.getRange.getEnd.getCharacter == expectedEndCharacter)
    if (strictlyCheckMessage)
      expect(diagnostic.getMessage == expectedMessage)
    else
      expect(diagnostic.getMessage.contains(expectedMessage))
  }

  private def extractWorkspaceReloadResponse(workspaceReloadResult: AnyRef): Option[ResponseError] =
    workspaceReloadResult match {
      case gsonMap: LinkedTreeMap[?, ?] if !gsonMap.isEmpty =>
        val gson = new Gson()
        Some(gson.fromJson(gson.toJson(gsonMap), classOf[ResponseError]))
      case _ => None
    }
}

object BspTestDefinitions {

  private final case class Details(
    name: String,
    version: String,
    bspVersion: String,
    argv: List[String],
    languages: List[String]
  )
  private val detailsCodec: JsonValueCodec[Details] = JsonCodecMaker.make

}
