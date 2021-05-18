package scala.cli

import java.io.{ByteArrayInputStream, File, InputStream, IOException, PrintStream}
import java.net.{ConnectException, Socket, URI}
import java.nio.file.{Path, Paths}
import java.util.concurrent.Executors

import ch.epfl.scala.bsp4j
import coursier.cache.internal.ThreadUtil
import org.eclipse.lsp4j.jsonrpc

import scala.annotation.tailrec
import scala.cli.bloop.bloopgun
import scala.cli.internal.Constants
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
        if (path.startsWith(os.pwd)) "." + File.separator + path.relativeTo(os.pwd).toString
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
    projectName: String,
    logger: Logger,
    bloopVersion: String = Constants.bloopVersion,
    stdout: PrintStream = System.out,
    stderr: PrintStream = System.err
  ): os.Path = {

    val config = bloopgun.BloopgunConfig.default
    val bloopgunLogger: bloopgun.BloopgunLogger =
      new bloopgun.BloopgunLogger {
        def debug(msg: => String): Unit =
          logger.debug(msg)
        def error(msg: => String, ex: Throwable): Unit =
          logger.log(s"Error: $msg ($ex)")
      }
    if (!bloopgun.Bloopgun.check(config, bloopgunLogger)) {
      val serverStartedFuture = bloopgun.Bloopgun.startServer(
        config,
        Executors.newSingleThreadScheduledExecutor(ThreadUtil.daemonThreadFactory()),
        100.millis,
        1.minute,
        bloopgunLogger
      )

      Await.result(serverStartedFuture, Duration.Inf)
    }

    val client: bsp4j.BuildClient =
      new bsp4j.BuildClient {

        var printedStart = false

        val gray = "\u001b[90m"
        val reset = Console.RESET

        def onBuildPublishDiagnostics(params: bsp4j.PublishDiagnosticsParams): Unit =
          // if (params.getBuildTarget == ???)
          for (diag <- params.getDiagnostics.asScala)
            printDiagnostic(os.Path(Paths.get(new URI(params.getTextDocument.getUri)).toAbsolutePath), diag)

        def onBuildLogMessage(params: bsp4j.LogMessageParams): Unit = {
          val prefix = params.getType match {
            case bsp4j.MessageType.ERROR       => "Error: "
            case bsp4j.MessageType.WARNING     => "Warning: "
            case bsp4j.MessageType.INFORMATION => ""
            case bsp4j.MessageType.LOG         => "" // discard those by default?
          }
          System.err.println(prefix + params.getMessage)
        }
        def onBuildShowMessage(params: bsp4j.ShowMessageParams): Unit = {
          // println("onBuildShowMessage")
          // pprint.log(params)
        }

        def onBuildTargetDidChange(params: bsp4j.DidChangeBuildTarget): Unit = {}

        def onBuildTaskStart(params: bsp4j.TaskStartParams): Unit =
          for (msg <- Option(params.getMessage) if !msg.contains(" no-op compilation")) {
            printedStart = true
            System.err.println(gray + msg + reset)
          }
        def onBuildTaskProgress(params: bsp4j.TaskProgressParams): Unit = {
          // println("onBuildTaskProgress")
          // pprint.log(params)
        }
        def onBuildTaskFinish(params: bsp4j.TaskFinishParams): Unit =
          if (printedStart)
            for (msg <- Option(params.getMessage))
              System.err.println(gray + msg + reset)

        override def onConnectWithServer(server: bsp4j.BuildServer): Unit = {}
      }

    val conn = bloopgun.Bloopgun.bsp(
      config,
      (workspace / ".scala").toNIO,
      bloopgunLogger
    )

    val s = {
      @tailrec
      def create(stopAt: Long): Socket = {
        val maybeSocket =
          try Some(conn.openSocket())
          catch {
            case _: ConnectException => None
          }
        maybeSocket match {
          case Some(socket) => socket
          case None =>
            if (System.currentTimeMillis() >= stopAt)
              throw new IOException(s"Can't connect to ${conn.address}")
            else {
              Thread.sleep(100L)
              create(stopAt)
            }
        }
      }

      create(System.currentTimeMillis() + 5000L)
    }

    val ec = Executors.newFixedThreadPool(4, ThreadUtil.daemonThreadFactory())
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
    server.buildInitialize(initParams).get()

    server.onBuildInitialized()

    val results = server.workspaceBuildTargets().get()
    val buildTargetOpt = results.getTargets.asScala.find(_.getDisplayName == projectName)

    val buildTarget = buildTargetOpt.getOrElse(???)

    val compileRes = server.buildTargetCompile(
      new bsp4j.CompileParams(List(buildTarget.getId).asJava)
    ).get()

    val scalacOptionsResp = server.buildTargetScalacOptions(
      new bsp4j.ScalacOptionsParams(List(buildTarget.getId).asJava)
    ).get()

    val scalacOptions = scalacOptionsResp.getItems.asScala
      .find(_.getTarget == buildTarget.getId)
      .getOrElse(???)

    val success = compileRes.getStatusCode == bsp4j.StatusCode.OK
    if (!success) {
      sys.error("Compilation failed")
    }

    os.Path(Paths.get(new URI(scalacOptions.getClassDirectory)).toAbsolutePath)
  }

}
