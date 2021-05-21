package scala.cli

import java.io.{ByteArrayInputStream, File, InputStream, IOException, PrintStream}
import java.net.{ConnectException, Socket, URI}
import java.nio.file.{Path, Paths}
import java.util.concurrent.Executors

import ch.epfl.scala.bsp4j
import org.eclipse.lsp4j.jsonrpc

import scala.annotation.tailrec
import scala.cli.bloop.bloop4j.BloopExtraBuildParams
import scala.cli.bloop.bloopgun
import scala.cli.internal.{Constants, Util}
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

trait FullBuildServer extends bsp4j.BuildServer with bsp4j.ScalaBuildServer

object Bloop {

  private def emptyInputStream: InputStream =
    new ByteArrayInputStream(Array.emptyByteArray)

  private def printDiagnostic(path: os.Path, diag: bsp4j.Diagnostic): Unit =
    if (diag.getSeverity == bsp4j.DiagnosticSeverity.ERROR || diag.getSeverity == bsp4j.DiagnosticSeverity.WARNING) {
      val red = Console.RED
      val yellow = Console.YELLOW
      val reset = Console.RESET
      val prefix = if (diag.getSeverity == bsp4j.DiagnosticSeverity.ERROR) s"[${red}error$reset] " else s"[${yellow}warn$reset] "

      val line = (diag.getRange.getStart.getLine + 1).toString + ":"
      val col = (diag.getRange.getStart.getCharacter + 1).toString + ":"
      val msgIt = diag.getMessage.linesIterator

      val path0 =
        if (path.startsWith(Os.pwd)) "." + File.separator + path.relativeTo(Os.pwd).toString
        else path.toString
      println(s"$prefix$path0:$line$col" + (if (msgIt.hasNext) " " + msgIt.next() else ""))
      for (line <- msgIt)
        println(prefix + line)
      for (code <- Option(diag.getCode))
        code.linesIterator.map(prefix + _).foreach(println(_))
      if (diag.getRange.getStart.getLine == diag.getRange.getEnd.getLine && diag.getRange.getStart.getCharacter != null && diag.getRange.getEnd.getCharacter != null)
        println(prefix + " " * diag.getRange.getStart.getCharacter + "^" * (diag.getRange.getEnd.getCharacter - diag.getRange.getStart.getCharacter + 1))
    }

  def compile(
    workspace: os.Path,
    classesDir: os.Path,
    projectName: String,
    logger: Logger,
    bloopVersion: String = Constants.bloopVersion
  ): Boolean = {

    val config = bloopgun.BloopgunConfig.default.copy(
      bspStdout = logger.bloopBspStdout,
      bspStderr = logger.bloopBspStderr
    )
    val bloopgunLogger = logger.bloopgunLogger

    val isBloopRunning = bloopgun.Bloopgun.check(config, bloopgunLogger)

    logger.debug(
      if (isBloopRunning) s"Bloop is running on ${config.host}:${config.port}"
      else s"No bloop daemon found on ${config.host}:${config.port}"
    )

    if (!isBloopRunning) {
      logger.debug(s"Starting bloop server version ${config.version}")
      val serverStartedFuture = bloopgun.Bloopgun.startServer(
        config,
        Executors.newSingleThreadScheduledExecutor(Util.daemonThreadFactory("scala-cli-bloopgun")),
        100.millis,
        1.minute,
        bloopgunLogger
      )

      Await.result(serverStartedFuture, Duration.Inf)
      logger.debug("Bloop server started")
    }

    val client: bsp4j.BuildClient =
      new bsp4j.BuildClient {

        var printedStart = false

        val gray = "\u001b[90m"
        val reset = Console.RESET

        def onBuildPublishDiagnostics(params: bsp4j.PublishDiagnosticsParams): Unit = {
          logger.debug("Received onBuildPublishDiagnostics from bloop: " + pprint.tokenize(params).map(_.render).mkString)
          // if (params.getBuildTarget == ???)
          for (diag <- params.getDiagnostics.asScala)
            printDiagnostic(os.Path(Paths.get(new URI(params.getTextDocument.getUri)).toAbsolutePath), diag)
        }

        def onBuildLogMessage(params: bsp4j.LogMessageParams): Unit = {
          logger.debug("Received onBuildLogMessage from bloop: " + pprint.tokenize(params).map(_.render).mkString)
          val prefix = params.getType match {
            case bsp4j.MessageType.ERROR       => "Error: "
            case bsp4j.MessageType.WARNING     => "Warning: "
            case bsp4j.MessageType.INFORMATION => ""
            case bsp4j.MessageType.LOG         => "" // discard those by default?
          }
          System.err.println(prefix + params.getMessage)
        }
        def onBuildShowMessage(params: bsp4j.ShowMessageParams): Unit =
          logger.debug("Received onBuildShowMessage from bloop: " + pprint.tokenize(params).map(_.render).mkString)

        def onBuildTargetDidChange(params: bsp4j.DidChangeBuildTarget): Unit =
          logger.debug("Received onBuildTargetDidChange from bloop: " + pprint.tokenize(params).map(_.render).mkString)

        def onBuildTaskStart(params: bsp4j.TaskStartParams): Unit = {
          logger.debug("Received onBuildTaskStart from bloop: " + pprint.tokenize(params).map(_.render).mkString)
          for (msg <- Option(params.getMessage) if !msg.contains(" no-op compilation")) {
            printedStart = true
            System.err.println(gray + msg + reset)
          }
        }
        def onBuildTaskProgress(params: bsp4j.TaskProgressParams): Unit =
          logger.debug("Received onBuildTaskProgress from bloop: " + pprint.tokenize(params).map(_.render).mkString)
        def onBuildTaskFinish(params: bsp4j.TaskFinishParams): Unit = {
          logger.debug("Received onBuildTaskFinish from bloop: " + pprint.tokenize(params).map(_.render).mkString)
          if (printedStart)
            for (msg <- Option(params.getMessage))
              System.err.println(gray + msg + reset)
        }

        override def onConnectWithServer(server: bsp4j.BuildServer): Unit = {}
      }

    logger.debug("Opening BSP connection with bloop")
    val conn = bloopgun.Bloopgun.bsp(
      config,
      (workspace / ".scala").toNIO,
      bloopgunLogger
    )
    logger.debug(s"Bloop BSP connection waiting at ${conn.address}")

    val s = {
      @tailrec
      def create(stopAt: Long): Socket = {
        val maybeSocket =
          try Right(conn.openSocket())
          catch {
            case e: ConnectException => Left(e)
          }
        maybeSocket match {
          case Right(socket) => socket
          case Left(e) =>
            if (System.currentTimeMillis() >= stopAt)
              throw new IOException(s"Can't connect to ${conn.address}", e)
            else {
              Thread.sleep(100L)
              create(stopAt)
            }
        }
      }

      create(System.currentTimeMillis() + 5000L)
    }

    logger.debug(s"Connected to Bloop via BSP at ${conn.address}")

    val ec = Executors.newFixedThreadPool(4, Util.daemonThreadFactory("scala-cli-bsp-jsonrpc"))
    val launcher = new jsonrpc.Launcher.Builder[FullBuildServer]()
      .setExecutorService(ec)
      .setInput(s.getInputStream)
      .setOutput(s.getOutputStream)
      .setRemoteInterface(classOf[FullBuildServer])
      .setLocalService(client)
      .create()
    val server = launcher.getRemoteProxy
    client.onConnectWithServer(server)

    val f = launcher.startListening()

    val initParams = new bsp4j.InitializeBuildParams(
      "scala-cli",
      Constants.version,
      Constants.bspVersion,
      (workspace / ".scala").toNIO.toUri.toASCIIString,
      new bsp4j.BuildClientCapabilities(List("scala", "java").asJava)
    )
    val bloopExtraParams = new BloopExtraBuildParams()
    bloopExtraParams.setClientClassesRootDir(classesDir.toNIO.toUri.toASCIIString)
    bloopExtraParams.setOwnsBuildFiles(true)
    initParams.setData(bloopExtraParams)
    logger.debug("Sending buildInitialize BSP command to Bloop")
    server.buildInitialize(initParams).get()

    server.onBuildInitialized()

    logger.debug("Listing BSP build targets")
    val results = server.workspaceBuildTargets().get()
    val buildTargetOpt = results.getTargets.asScala.find(_.getDisplayName == projectName)

    val buildTarget = buildTargetOpt.getOrElse {
      throw new Exception(
        s"Expected to find project '$projectName' in build targets (only got ${results.getTargets.asScala.map("'" + _.getDisplayName + "'").mkString(", ")})"
      )
    }

    logger.debug(s"Compiling $projectName with Bloop")
    val compileRes = server.buildTargetCompile(
      new bsp4j.CompileParams(List(buildTarget.getId).asJava)
    ).get()

    logger.debug(s"Getting scalac options of $projectName with Bloop")
    val scalacOptionsResp = server.buildTargetScalacOptions(
      new bsp4j.ScalacOptionsParams(List(buildTarget.getId).asJava)
    ).get()

    val scalacOptions = scalacOptionsResp.getItems.asScala
      .find(_.getTarget == buildTarget.getId)
      .getOrElse {
        throw new Exception(
          s"Expected to find build target '${buildTarget.getId}' in scalac options (only got ${scalacOptionsResp.getItems.asScala.map("'" + _.getTarget + "'").mkString(", ")})"
        )
      }

    val success = compileRes.getStatusCode == bsp4j.StatusCode.OK
    logger.debug(if (success) "Compilation succeeded" else "Compilation failed")
    success
  }

}
