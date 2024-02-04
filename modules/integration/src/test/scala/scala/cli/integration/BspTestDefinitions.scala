package scala.cli.integration

import ch.epfl.scala.bsp4j.{BuildTargetIdentifier, JvmTestEnvironmentParams}
import ch.epfl.scala.bsp4j as b
import com.eed3si9n.expecty.Expecty.expect
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.{Gson, JsonElement}
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
    reuseRoot: Option[os.Path] = None,
    stdErrOpt: Option[os.RelPath] = None
  )(
    f: (
      os.Path,
      TestBspClient,
      b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer & b.JvmBuildServer
    ) => Future[T]
  ): T = {

    def attempt(): Try[T] = Try {
      val inputsRoot                              = inputs.root()
      val root                                    = reuseRoot.getOrElse(inputsRoot)
      val stdErrPathOpt: Option[os.ProcessOutput] = stdErrOpt.map(path => inputsRoot / path)
      val stderr: os.ProcessOutput                = stdErrPathOpt.getOrElse(os.Inherit)

      val proc = os.proc(TestUtil.cli, "bsp", bspOptions ++ extraOptions, args)
        .spawn(cwd = root, stderr = stderr)
      var remoteServer: b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer & b.JvmBuildServer =
        null

      val bspServerExited = Promise[Unit]()
      val t = new Thread("bsp-server-watcher") {
        setDaemon(true)
        override def run() = {
          proc.join()
          bspServerExited.success(())
        }
      }
      t.start()

      def whileBspServerIsRunning[T](f: Future[T]): Future[T] = {
        val ex = new Exception
        Future.firstCompletedOf(Seq(f.map(Right(_)), bspServerExited.future.map(Left(_))))
          .transform {
            case Success(Right(t)) => Success(t)
            case Success(Left(())) => Failure(new Exception("BSP server exited too early", ex))
            case Failure(ex)       => Failure(ex)
          }
      }

      try {
        val (localClient, remoteServer0, _) =
          TestBspClient.connect(proc.stdout, proc.stdin, pool)
        remoteServer = remoteServer0
        Await.result(
          whileBspServerIsRunning(remoteServer.buildInitialize(initParams(root)).asScala),
          Duration.Inf
        )
        Await.result(whileBspServerIsRunning(f(root, localClient, remoteServer)), Duration.Inf)
      }
      finally {
        if (remoteServer != null)
          try
            Await.result(whileBspServerIsRunning(remoteServer.buildShutdown().asScala), 20.seconds)
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
    os.rel / "simple.sc" -> s"//> using dep \"com.lihaoyi::pprint:${Constants.pprintVersion}\""
  )

  test("setup-ide should have only absolute paths even if relative ones were specified") {
    val path = os.rel / "directory" / "simple.sc"
    val inputs =
      TestInputs(path -> s"//> using dep \"com.lihaoyi::pprint:${Constants.pprintVersion}\"")
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

        if (actualScalaVersion.startsWith("3."))
          expect(compileProducts.contains(os.rel / "simple$_.class"))
        else
          expect(compileProducts.contains(os.rel / "simple$.class"))

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
        """val msg: NonExistent = "Hello""""
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

        val expectedMessage =
          if (actualScalaVersion.startsWith("2."))
            "not found: type NonExistent"
          else
            "Not found: type NonExistent"

        checkDiagnostic(
          diagnostic = diagnostics.head,
          expectedMessage = expectedMessage,
          expectedSeverity = b.DiagnosticSeverity.ERROR,
          expectedStartLine = 0,
          expectedStartCharacter = 9,
          expectedEndLine = 0,
          expectedEndCharacter = 20
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
        s"""//> using dep "com.lihaoyi::pprint:0.0.0.0.0.1"
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

  test("directives in multiple files diagnostics") {
    val inputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using scala "3.3.0"
           |
           |object Foo extends App {
           |  println("Foo")
           |}
           |""".stripMargin,
      os.rel / "Bar.scala"  -> "",
      os.rel / "Hello.java" -> "//> using jvm \"11\""
    )

    withBsp(inputs, Seq(".")) { (root, localClient, remoteServer) =>
      async {
        await(remoteServer.workspaceBuildTargets().asScala)
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
        expect(compileResp.getStatusCode == b.StatusCode.OK)

        def checkDirectivesInMultipleFilesWarnings(
          fileName: String,
          expectedStartLine: Int,
          expectedStartCharacter: Int,
          expectedEndLine: Int,
          expectedEndCharacter: Int
        ): Unit = {
          val diagnosticsParams = localClient.diagnostics().collectFirst {
            case diag
                if !diag.getDiagnostics.isEmpty &&
                TestUtil.normalizeUri(diag.getTextDocument.getUri) ==
                  TestUtil.normalizeUri((root / fileName).toNIO.toUri.toASCIIString) => diag
          }
          expect(diagnosticsParams.isDefined)
          val diagnostics = diagnosticsParams.get.getDiagnostics.asScala.toSeq

          val expectedMessage =
            "Using directives detected in multiple files. It is recommended to keep them centralized in the"
          checkDiagnostic(
            diagnostic = diagnostics.head,
            expectedMessage = expectedMessage,
            expectedSeverity = b.DiagnosticSeverity.WARNING,
            expectedStartLine = expectedStartLine,
            expectedStartCharacter = expectedStartCharacter,
            expectedEndLine = expectedEndLine,
            expectedEndCharacter = expectedEndCharacter,
            strictlyCheckMessage = false
          )
        }

        checkDirectivesInMultipleFilesWarnings("Foo.scala", 0, 0, 0, 23)
        checkDirectivesInMultipleFilesWarnings("Hello.java", 0, 0, 0, 18)
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
          """//> using dep "com.lihaoyi::pprint:0.6.6"
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
        """//> using dep "com.lihaoyi::os-lib:0.7.8"
          |object Messages {
          |  def msg = "Hello"
          |}
          |""".stripMargin,
      os.rel / "MyTests.test.scala" ->
        """//> using dep "com.lihaoyi::utest::0.7.10"
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

  test("return .scala-build directory as a output paths") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        """object Hello extends App {
          |  println("Hello World")
          |}
          |""".stripMargin
    )
    withBsp(inputs, Seq(".")) { (root, _, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId).toSeq
          extractTestTargets(targets)
        }

        val resp = await(
          remoteServer.buildTargetOutputPaths(new b.OutputPathsParams(List(target).asJava)).asScala
        )
        val outputPathsItems = resp.getItems.asScala
        assert(outputPathsItems.nonEmpty)

        val outputPathItem        = outputPathsItems.head
        val expectedOutputPathUri = (root / Constants.workspaceDirName).toIO.toURI.toASCIIString
        val expectedOutputPathItem =
          new b.OutputPathsItem(
            target,
            List(new b.OutputPathItem(expectedOutputPathUri, b.OutputPathItemKind.DIRECTORY)).asJava
          )
        expect(outputPathItem == expectedOutputPathItem)

      }
    }
  }

  test("workspace/reload --dependency option") {
    val inputs = TestInputs(
      os.rel / "ReloadTest.scala" ->
        s"""object ReloadTest {
           |  println(os.pwd)
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

  test("workspace/reload extra dependency directive") {
    val sourceFilePath = os.rel / "ReloadTest.scala"
    val inputs = TestInputs(
      sourceFilePath ->
        s"""object ReloadTest {
           |  println(os.pwd)
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

            val depName    = "os-lib"
            val depVersion = "0.8.1"
            val updatedSourceFile =
              s"""//> using dep "com.lihaoyi::$depName:$depVersion"
                 |
                 |object ReloadTest {
                 |  println(os.pwd)
                 |}
                 |""".stripMargin
            os.write.over(root / sourceFilePath, updatedSourceFile)

            val reloadResponse =
              extractWorkspaceReloadResponse(await(remoteServer.workspaceReload().asScala))
            expect(reloadResponse.isEmpty)

            val depSourcesParams = new b.DependencySourcesParams(targets.asJava)
            val depSourcesResponse =
              await(remoteServer.buildTargetDependencySources(depSourcesParams).asScala)
            val depSources = depSourcesResponse.getItems.asScala.flatMap(_.getSources.asScala)
            expect(depSources.exists(s => s.contains(depName) && s.contains(depVersion)))
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

          val reloadResp = await(remoteServer.workspaceReload().asScala)
          val responseError = extractWorkspaceReloadResponse(reloadResp).getOrElse {
            sys.error(s"Unexpected workspace reload response shape $reloadResp")
          }
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

  test("workspace/reload should restart bloop with correct JVM version from options") {
    val sourceFilePath = os.rel / "ReloadTest.java"
    val inputs = TestInputs(
      sourceFilePath ->
        s"""public class ReloadTest {
           |  public static void main(String[] args) {
           |    String a = "Hello World";
           |
           |    switch (a) {
           |      case String s when s.length() > 6 -> System.out.println(s.toUpperCase());
           |      case String s -> System.out.println(s.toLowerCase());
           |    }
           |  }
           |}""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "bloop", "exit")
        .call(
          cwd = root,
          stdout = os.Inherit
        )
      os.proc(TestUtil.cli, "setup-ide", ".", "--jvm", "11", extraOptions)
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

            val errorResponse =
              await(remoteServer.buildTargetCompile(new b.CompileParams(targets.asJava)).asScala)
            expect(errorResponse.getStatusCode == b.StatusCode.ERROR)

            val javacOptions = Seq("--javac-opt", "--enable-preview")

            os.proc(TestUtil.cli, "setup-ide", ".", "--jvm", "19", javacOptions, extraOptions)
              .call(
                cwd = root,
                stdout = os.Inherit
              )

            val reloadResponse =
              extractWorkspaceReloadResponse(await(remoteServer.workspaceReload().asScala))
            expect(reloadResponse.isEmpty)

            val buildTargetsResp0 = await(remoteServer.workspaceBuildTargets().asScala)
            val reloadedTargets   = buildTargetsResp0.getTargets.asScala.map(_.getId).toSeq

            val okResponse =
              await(
                remoteServer.buildTargetCompile(new b.CompileParams(reloadedTargets.asJava)).asScala
              )
            expect(okResponse.getStatusCode == b.StatusCode.OK)
          }
      }

    }
  }

  test("workspace/reload should restart bloop with correct JVM version from directives") {
    val sourceFilePath = os.rel / "ReloadTest.java"
    val inputs = TestInputs(
      sourceFilePath ->
        s"""//> using jvm 11
           |
           |public class ReloadTest {
           |  public static void main(String[] args) {
           |    System.out.println("Hello World");
           |  }
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "bloop", "exit")
        .call(
          cwd = root,
          stdout = os.Inherit
        )
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
            expect(resp.getStatusCode == b.StatusCode.OK)

            val updatedSourceFile =
              s"""//> using jvm 19
                 |//> using javacOpt --enable-preview
                 |
                 |public class ReloadTest {
                 |  public static void main(String[] args) {
                 |    String a = "Hello World";
                 |
                 |    switch (a) {
                 |      case String s when s.length() > 6 -> System.out.println(s.toUpperCase());
                 |      case String s -> System.out.println(s.toLowerCase());
                 |    }
                 |  }
                 |}
                 |""".stripMargin
            os.write.over(root / sourceFilePath, updatedSourceFile)

            val errorResponse =
              await(remoteServer.buildTargetCompile(new b.CompileParams(targets.asJava)).asScala)
            expect(errorResponse.getStatusCode == b.StatusCode.ERROR)

            val reloadResponse =
              extractWorkspaceReloadResponse(await(remoteServer.workspaceReload().asScala))
            expect(reloadResponse.isEmpty)

            val buildTargetsResp0 = await(remoteServer.workspaceBuildTargets().asScala)
            val reloadedTargets   = buildTargetsResp0.getTargets.asScala.map(_.getId).toSeq

            val okResponse =
              await(
                remoteServer.buildTargetCompile(new b.CompileParams(reloadedTargets.asJava)).asScala
              )
            expect(okResponse.getStatusCode == b.StatusCode.OK)
          }
      }

    }
  }

  test("bsp should start bloop with correct JVM version from directives") {
    val sourceFilePath = os.rel / "ReloadTest.java"
    val inputs = TestInputs(
      sourceFilePath ->
        s"""//> using jvm 19
           |//> using javacOpt --enable-preview
           |
           |public class ReloadTest {
           |  public static void main(String[] args) {
           |    String a = "Hello World";
           |
           |    switch (a) {
           |      case String s when s.length() > 6 -> System.out.println(s.toUpperCase());
           |      case String s -> System.out.println(s.toLowerCase());
           |    }
           |  }
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "bloop", "exit")
        .call(
          cwd = root,
          stdout = os.Inherit
        )
      os.proc(TestUtil.cli, "--power", "bloop", "start", "--jvm", "17")
        .call(
          cwd = root,
          stdout = os.Inherit
        )
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
            expect(resp.getStatusCode == b.StatusCode.OK)
          }
      }
    }
  }

  test("bloop projects are initialised properly for an invalid directive value") {
    val inputs = TestInputs(
      os.rel / "InvalidUsingDirective.scala" ->
        s"""//> using scala true
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
              """Encountered an error for the scala using directive.
                |Expected a string value, got 'true'""".stripMargin,
            expectedSeverity = b.DiagnosticSeverity.ERROR,
            expectedStartLine = 0,
            expectedStartCharacter = 16,
            expectedEndLine = 0,
            expectedEndCharacter = 20
          )
        }
    }
  }

  test("bloop projects are initialised properly for an unrecognised directive") {
    val sourceFileName = "UnrecognisedUsingDirective.scala"
    val directiveKey   = "unrecognised.directive"
    val directiveValue = "value"
    val inputs = TestInputs(
      os.rel / sourceFileName ->
        s"""//> using $directiveKey "$directiveValue"
           |
           |object UnrecognisedUsingDirective extends App {
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
            extractDiagnosticsParams(root / sourceFileName, localClient)
          val diagnostics = diagnosticsParams.getDiagnostics.asScala
          expect(diagnostics.length == 1)
          checkDiagnostic(
            diagnostic = diagnostics.head,
            expectedMessage =
              s"Unrecognized directive: $directiveKey with values: $directiveValue",
            expectedSeverity = b.DiagnosticSeverity.ERROR,
            expectedStartLine = 0,
            expectedStartCharacter = 34,
            expectedEndLine = 0,
            expectedEndCharacter = 39
          )
        }
    }
  }

  test("bloop projects are initialised properly for a directive for an unfetchable dependency") {
    val inputs = TestInputs(
      os.rel / "InvalidUsingDirective.scala" ->
        s"""//> using dep "no::lib:123"
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
  test("bsp should support parsing cancel params") { // TODO This test only checks if the native launcher of Scala CLI is able to parse cancel params,
    // this test does not check if Bloop supports $/cancelRequest. The status of that is tracked under the https://github.com/scalacenter/bloop/issues/2030.
    val fileName = "Hello.scala"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""object Hello extends App {
           |  while(true) {
           |    println("Hello World")
           |  }
           |}
           |""".stripMargin
    )
    withBsp(inputs, Seq("."), stdErrOpt = Some(os.rel / "stderr.txt")) {
      (root, _, remoteServer) =>
        async {
          // prepare build
          val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
          // build code
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId())
          val compileResp = await {
            remoteServer
              .buildTargetCompile(new b.CompileParams(targets.asJava))
              .asScala
          }
          expect(compileResp.getStatusCode == b.StatusCode.OK)

          val Some(mainTarget) = targets.find(!_.getUri.contains("-test"))
          val runRespFuture =
            remoteServer
              .buildTargetRun(new b.RunParams(mainTarget))
          runRespFuture.cancel(true)
          expect(runRespFuture.isCancelled || runRespFuture.isCompletedExceptionally)
          expect(!os.read(root / "stderr.txt").contains(
            "Unmatched cancel notification for request id null"
          ))
        }
    }
  }
  test("bsp should report actionable diagnostic when enabled") {
    val fileName = "Hello.scala"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""//> using dep "com.lihaoyi::os-lib:0.7.8"
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
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
            expectedMessage = "os-lib is outdated",
            expectedSeverity = b.DiagnosticSeverity.HINT,
            expectedStartLine = 0,
            expectedStartCharacter = 15,
            expectedEndLine = 0,
            expectedEndCharacter = 40,
            expectedSource = Some("scala-cli"),
            strictlyCheckMessage = false
          )

          val scalaDiagnostic = new Gson().fromJson[b.ScalaDiagnostic](
            updateActionableDiagnostic.getData().asInstanceOf[JsonElement],
            classOf[b.ScalaDiagnostic]
          )

          val actions = scalaDiagnostic.getActions().asScala.toList
          assert(actions.size == 1)
          val changes = actions.head.getEdit().getChanges().asScala.toList
          assert(changes.size == 1)
          val textEdit = changes.head

          expect(textEdit.getNewText().contains("com.lihaoyi::os-lib:"))
          expect(textEdit.getRange().getStart.getLine == 0)
          expect(textEdit.getRange().getStart.getCharacter == 15)
          expect(textEdit.getRange().getEnd.getLine == 0)
          expect(textEdit.getRange().getEnd.getCharacter == 40)
        }
    }
  }
  test("bsp should support jvmRunEnvironment request") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using dep "com.lihaoyi::os-lib:0.7.8"
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    withBsp(inputs, Seq(".")) {
      (_, _, remoteServer) =>
        async {
          // prepare build
          val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
          // build code
          val targets = buildTargetsResp.getTargets.asScala.map(_.getId()).asJava

          val jvmRunEnvironmentResult: b.JvmRunEnvironmentResult = await {
            remoteServer
              .buildTargetJvmRunEnvironment(new b.JvmRunEnvironmentParams(targets))
              .asScala
          }
          expect(jvmRunEnvironmentResult.getItems.asScala.toList.nonEmpty)

          val jvmTestEnvironmentResult: b.JvmTestEnvironmentResult = await {
            remoteServer
              .buildTargetJvmTestEnvironment(new JvmTestEnvironmentParams(targets))
              .asScala
          }
          expect(jvmTestEnvironmentResult.getItems.asScala.toList.nonEmpty)
        }
    }
  }

  if (actualScalaVersion.startsWith("3"))
    test("@main in script") {
      val inputs = TestInputs(
        os.rel / "test.sc" ->
          """@main def main(args: Strings*): Unit = println("Args: " + args.mkString(" "))
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

          checkDiagnostic(
            diagnostic = diagnostics.head,
            expectedMessage =
              "Annotation @main in .sc scripts is not supported, use .scala format instead",
            expectedSeverity = b.DiagnosticSeverity.ERROR,
            expectedStartLine = 0,
            expectedStartCharacter = 0,
            expectedEndLine = 0,
            expectedEndCharacter = 5
          )
        }
      }
    }

  def testSourceJars(
    directives: String = "//> using jar Message.jar",
    getBspOptions: os.RelPath => List[String] = _ => List.empty,
    checkTestTarget: Boolean = false
  ): Unit = {
    val jarSources    = os.rel / "jarStuff"
    val mainSources   = os.rel / "src"
    val jarPath       = mainSources / "Message.jar"
    val sourceJarPath = mainSources / "Message-sources.jar"
    val inputs = TestInputs(
      jarSources / "Message.scala" -> "case class Message(value: String)",
      mainSources / "Main.scala" ->
        s"""$directives
           |object Main extends App {
           |  println(Message("Hello").value)
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      // package the library jar
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        jarSources,
        "--library",
        "-o",
        jarPath,
        extraOptions
      )
        .call(cwd = root)
      // package the sources jar
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        jarSources,
        "--source",
        "-o",
        sourceJarPath,
        extraOptions
      )
        .call(cwd = root)
      withBsp(
        inputs,
        Seq(mainSources.toString),
        reuseRoot = Some(root),
        bspOptions = getBspOptions(sourceJarPath)
      ) {
        (_, _, remoteServer) =>
          async {
            val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
            val targets = buildTargetsResp
              .getTargets
              .asScala
            val Some(mainTarget) = targets.find(!_.getId.getUri.contains("-test"))
            val Some(testTarget) = targets.find(_.getId.getUri.contains("-test"))
            // ensure that the project compiles
            val compileRes = await(remoteServer.buildTargetCompile(
              new b.CompileParams(List(mainTarget.getId).asJava)
            ).asScala)
            expect(compileRes.getStatusCode == b.StatusCode.OK)
            // ensure that the source jar is in the dependency sources
            val dependencySourcesResp = await {
              remoteServer
                .buildTargetDependencySources(
                  new b.DependencySourcesParams(List(mainTarget.getId, testTarget.getId).asJava)
                )
                .asScala
            }
            val dependencySourceItems = dependencySourcesResp.getItems.asScala
            val sources = dependencySourceItems
              .filter(dsi =>
                if (checkTestTarget) dsi.getTarget == testTarget.getId
                else dsi.getTarget == mainTarget.getId
              )
              .flatMap(_.getSources.asScala)
            expect(sources.exists(_.endsWith(sourceJarPath.last)))
          }
      }
    }
  }

  test("source jars handled correctly from the command line") {
    testSourceJars(getBspOptions = sourceJarPath => List("--source-jar", sourceJarPath.toString))
  }

  test(
    "source jars handled correctly from the command line smartly assuming a *-sources.jar is a source jar"
  ) {
    testSourceJars(getBspOptions = sourceJarPath => List("--extra-jar", sourceJarPath.toString))
  }

  test("source jars handled correctly from a test scope using directive") {
    testSourceJars(
      directives = """//> using jar Message.jar
                     |//> using test.sourceJar Message-sources.jar""".stripMargin,
      checkTestTarget = true
    )
  }

  if (actualScalaVersion.startsWith("3."))
    test("actionable diagnostics from compiler") {
      val inputs = TestInputs(
        os.rel / "test.sc" ->
          """//> using scala 3.3.2-RC1-bin-20230723-5afe621-NIGHTLY
            |def foo(): Int = 23
            |
            |def test: Int = foo
            |//              ^^^ error: missing parentheses
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

          val diagnostics = diagnosticsParams.getDiagnostics.asScala
          expect(diagnostics.size == 1)

          val theDiagnostic = diagnostics.head

          checkDiagnostic(
            diagnostic = theDiagnostic,
            expectedMessage =
              "method foo in class test$_ must be called with () argument",
            expectedSeverity = b.DiagnosticSeverity.ERROR,
            expectedStartLine = 3,
            expectedStartCharacter = 16,
            expectedEndLine = 3,
            expectedEndCharacter = 19
          )

          // Shouldn't dataKind be set to "scala"? expect(theDiagnostic.getDataKind == "scala")

          val gson = new com.google.gson.Gson()

          val scalaDiagnostic: b.ScalaDiagnostic = gson.fromJson(
            theDiagnostic.getData.toString,
            classOf[b.ScalaDiagnostic]
          )

          val actions = scalaDiagnostic.getActions.asScala
          expect(actions.size == 1)

          val action = actions.head
          expect(action.getTitle == "Insert ()")

          val edit = action.getEdit
          expect(edit.getChanges.asScala.size == 1)
          val change = edit.getChanges.asScala.head

          val expectedRange = new b.Range(
            new b.Position(9, 19),
            new b.Position(9, 19)
          )
          expect(change.getRange == expectedRange)
          expect(change.getNewText == "()")
        }
      }
    }

  if (!actualScalaVersion.startsWith("2.12"))
    test("actionable diagnostics on deprecated using directives") {
      val inputs = TestInputs(
        os.rel / "test.sc" ->
          """//> using toolkit latest
            |//> using test.toolkit "typelevel:latest"
            |
            |//> using lib org.typelevel::cats-core:2.6.1
            |
            |object Test extends App {
            | println("Hello")
            |}
            |""".stripMargin
      )

      withBsp(inputs, Seq(".", "--actions=false")) { (root, localClient, remoteServer) =>
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
          expect(compileResp.getStatusCode == b.StatusCode.OK)

          val diagnosticsParams = {
            val diagnostics = localClient.diagnostics()
              .filter(_.getReset == false)
            expect(diagnostics.size == 3)
            val params = diagnostics.head
            expect(params.getBuildTarget.getUri == targetUri)
            expect(
              TestUtil.normalizeUri(params.getTextDocument.getUri) ==
                TestUtil.normalizeUri((root / "test.sc").toNIO.toUri.toASCIIString)
            )
            diagnostics
          }

          val diagnostics = diagnosticsParams.flatMap(_.getDiagnostics.asScala)
            .sortBy(_.getRange().getEnd().getCharacter())

          {
            checkDiagnostic(
              diagnostic = diagnostics.apply(0),
              expectedMessage =
                "Using 'latest' for toolkit is deprecated, use 'default' to get more stable behaviour",
              expectedSeverity = b.DiagnosticSeverity.WARNING,
              expectedStartLine = 0,
              expectedStartCharacter = 10,
              expectedEndLine = 0,
              expectedEndCharacter = 24
            )

            checkScalaAction(
              diagnostic = diagnostics.apply(0),
              expectedActionsSize = 1,
              expectedTitle = "Change to: toolkit default",
              expectedChanges = 1,
              expectedStartLine = 0,
              expectedStartCharacter = 10,
              expectedEndLine = 0,
              expectedEndCharacter = 24,
              expectedNewText = "toolkit default"
            )
          }

          {
            checkDiagnostic(
              diagnostic = diagnostics.apply(1),
              expectedMessage =
                "Using 'latest' for toolkit is deprecated, use 'default' to get more stable behaviour",
              expectedSeverity = b.DiagnosticSeverity.WARNING,
              expectedStartLine = 1,
              expectedStartCharacter = 10,
              expectedEndLine = 1,
              expectedEndCharacter = 41
            )

            checkScalaAction(
              diagnostic = diagnostics.apply(1),
              expectedActionsSize = 1,
              expectedTitle = "Change to: test.toolkit typelevel:default",
              expectedChanges = 1,
              expectedStartLine = 1,
              expectedStartCharacter = 10,
              expectedEndLine = 1,
              expectedEndCharacter = 41,
              expectedNewText = "test.toolkit typelevel:default"
            )
          }

          {
            checkDiagnostic(
              diagnostic = diagnostics.apply(2),
              expectedMessage =
                "Using 'lib' is deprecated, use 'dep' instead",
              expectedSeverity = b.DiagnosticSeverity.WARNING,
              expectedStartLine = 3,
              expectedStartCharacter = 10,
              expectedEndLine = 3,
              expectedEndCharacter = 44
            )

            checkScalaAction(
              diagnostic = diagnostics.apply(2),
              expectedActionsSize = 1,
              expectedTitle = "Change to: dep org.typelevel::cats-core:2.6.1",
              expectedChanges = 1,
              expectedStartLine = 3,
              expectedStartCharacter = 10,
              expectedEndLine = 3,
              expectedEndCharacter = 44,
              expectedNewText = "dep org.typelevel::cats-core:2.6.1"
            )
          }
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
    expectedSource: Option[String] = None,
    strictlyCheckMessage: Boolean = true
  ): Unit = {
    expect(diagnostic.getSeverity == expectedSeverity)
    expect(diagnostic.getRange.getStart.getLine == expectedStartLine)
    expect(diagnostic.getRange.getStart.getCharacter == expectedStartCharacter)
    expect(diagnostic.getRange.getEnd.getLine == expectedEndLine)
    expect(diagnostic.getRange.getEnd.getCharacter == expectedEndCharacter)
    if (strictlyCheckMessage)
      assertNoDiff(diagnostic.getMessage, expectedMessage)
    else
      expect(diagnostic.getMessage.contains(expectedMessage))
    for (es <- expectedSource)
      expect(diagnostic.getSource == es)
  }

  private def checkScalaAction(
    diagnostic: b.Diagnostic,
    expectedActionsSize: Int,
    expectedTitle: String,
    expectedChanges: Int,
    expectedStartLine: Int,
    expectedStartCharacter: Int,
    expectedEndLine: Int,
    expectedEndCharacter: Int,
    expectedNewText: String
  ) = {
    expect(diagnostic.getDataKind == "scala")

    val gson = new com.google.gson.Gson()

    val scalaDiagnostic: b.ScalaDiagnostic = gson.fromJson(
      diagnostic.getData.toString,
      classOf[b.ScalaDiagnostic]
    )

    val actions = scalaDiagnostic.getActions.asScala

    expect(actions.size == expectedActionsSize)

    val action = actions.head
    expect(action.getTitle == expectedTitle)

    val edit = action.getEdit
    expect(edit.getChanges.asScala.size == expectedChanges)
    val change = edit.getChanges.asScala.head

    val expectedRange = new b.Range(
      new b.Position(expectedStartLine, expectedStartCharacter),
      new b.Position(expectedEndLine, expectedEndCharacter)
    )
    expect(change.getRange == expectedRange)
    expect(change.getNewText == expectedNewText)
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

  private final case class TextEdit(range: b.Range, newText: String)

}
