package scala.cli.integration

import ch.epfl.scala.{bsp4j => b}
import com.eed3si9n.expecty.Expecty.expect
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError

import java.net.URI
import java.util.concurrent.{ExecutorService, ScheduledExecutorService}

import scala.annotation.tailrec
import scala.cli.integration.BspSuite.{Details, detailsCodec}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait BspSuite { _: ScalaCliSuite =>
  protected def extraOptions: Seq[String]
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

  protected def extractMainTargets(targets: Seq[b.BuildTargetIdentifier]): b.BuildTargetIdentifier =
    targets.collectFirst {
      case t if !t.getUri.contains("-test") => t
    }.get

  protected def extractTestTargets(targets: Seq[b.BuildTargetIdentifier]): b.BuildTargetIdentifier =
    targets.collectFirst {
      case t if t.getUri.contains("-test") => t
    }.get

  def withBsp[T](
    inputs: TestInputs,
    args: Seq[String],
    attempts: Int = if (TestUtil.isCI) 3 else 1,
    pauseDuration: FiniteDuration = 5.seconds,
    bspOptions: List[String] = List.empty,
    bspEnvs: Map[String, String] = Map.empty,
    reuseRoot: Option[os.Path] = None,
    stdErrOpt: Option[os.RelPath] = None,
    extraOptionsOverride: Seq[String] = extraOptions
  )(
    f: (
      os.Path,
      TestBspClient,
      b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer & b.JvmBuildServer
    ) => Future[T]
  ): T = withBspInitResults(
    inputs,
    args,
    attempts,
    pauseDuration,
    bspOptions,
    bspEnvs,
    reuseRoot,
    stdErrOpt,
    extraOptionsOverride
  )((root, client, server, _: b.InitializeBuildResult) => f(root, client, server))

  def withBspInitResults[T](
    inputs: TestInputs,
    args: Seq[String],
    attempts: Int = if (TestUtil.isCI) 3 else 1,
    pauseDuration: FiniteDuration = 5.seconds,
    bspOptions: List[String] = List.empty,
    bspEnvs: Map[String, String] = Map.empty,
    reuseRoot: Option[os.Path] = None,
    stdErrOpt: Option[os.RelPath] = None,
    extraOptionsOverride: Seq[String] = extraOptions
  )(
    f: (
      os.Path,
      TestBspClient,
      b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer & b.JvmBuildServer,
      b.InitializeBuildResult
    ) => Future[T]
  ): T = {

    def attempt(): Try[T] = Try {
      val inputsRoot                              = inputs.root()
      val root                                    = reuseRoot.getOrElse(inputsRoot)
      val stdErrPathOpt: Option[os.ProcessOutput] = stdErrOpt.map(path => root / path)
      val stderr: os.ProcessOutput                = stdErrPathOpt.getOrElse(os.Inherit)

      val proc = os.proc(TestUtil.cli, "bsp", bspOptions ++ extraOptionsOverride, args)
        .spawn(cwd = root, stderr = stderr, env = bspEnvs)
      var remoteServer: b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer & b.JvmBuildServer =
        null

      val bspServerExited = Promise[Unit]()
      val t               = new Thread("bsp-server-watcher") {
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
        val initRes: b.InitializeBuildResult = Await.result(
          whileBspServerIsRunning(remoteServer.buildInitialize(initParams(root)).asScala),
          Duration.Inf
        )
        Await.result(
          whileBspServerIsRunning(f(root, localClient, remoteServer, initRes)),
          Duration.Inf
        )
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
        case Success(t)  => t
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

  protected def readBspConfig(root: os.Path): Details = {
    val bspFile = root / ".bsp" / "scala-cli.json"
    expect(os.isFile(bspFile))
    val content = os.read.bytes(bspFile)
    // check that we can decode the connection details
    readFromArray(content)(detailsCodec)
  }

  protected def checkIfBloopProjectIsInitialised(
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

  protected def extractDiagnosticsParams(
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

  protected def checkDiagnostic(
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
    val message = TestUtil.removeAnsiColors(diagnostic.getMessage)
    if (strictlyCheckMessage)
      assertNoDiff(message, expectedMessage)
    else
      expect(message.contains(expectedMessage))
    for (es <- expectedSource)
      expect(diagnostic.getSource == es)
  }

  protected def checkScalaAction(
    diagnostic: b.Diagnostic,
    expectedActionsSize: Int,
    expectedTitle: String,
    expectedChanges: Int,
    expectedStartLine: Int,
    expectedStartCharacter: Int,
    expectedEndLine: Int,
    expectedEndCharacter: Int,
    expectedNewText: String
  ): Unit = {
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

  protected def extractWorkspaceReloadResponse(workspaceReloadResult: AnyRef)
    : Option[ResponseError] =
    workspaceReloadResult match {
      case gsonMap: LinkedTreeMap[?, ?] if !gsonMap.isEmpty =>
        val gson = new Gson()
        Some(gson.fromJson(gson.toJson(gsonMap), classOf[ResponseError]))
      case _ => None
    }
}

object BspSuite {
  final protected case class Details(
    name: String,
    version: String,
    bspVersion: String,
    argv: List[String],
    languages: List[String]
  )
  protected val detailsCodec: JsonValueCodec[Details] = JsonCodecMaker.make
}
