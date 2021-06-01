package scala.cli

import java.io.{ByteArrayInputStream, File, InputStream, IOException, PrintStream}
import java.net.{ConnectException, Socket, URI}
import java.nio.file.{Path, Paths}
import java.util.concurrent.{ExecutorService, ScheduledExecutorService}

import ch.epfl.scala.bsp4j
import org.eclipse.lsp4j.jsonrpc

import scala.annotation.tailrec
import scala.cli.bloop.bloop4j.BloopExtraBuildParams
import scala.cli.bloop.bloopgun
import scala.cli.internal.{Constants, Util}
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

trait FullBuildServer extends bsp4j.BuildServer with bsp4j.ScalaBuildServer

object Bloop {

  private def emptyInputStream: InputStream =
    new ByteArrayInputStream(Array.emptyByteArray)

  private def ensureBloopRunning(
    config: bloopgun.BloopgunConfig,
    startServerChecksPool: ScheduledExecutorService,
    logger: Logger
  ): Unit = {

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
        startServerChecksPool,
        100.millis,
        1.minute,
        bloopgunLogger
      )

      Await.result(serverStartedFuture, Duration.Inf)
      logger.debug("Bloop server started")
    }

  }

  private def connect(
    conn: bloopgun.BspConnection,
    period: FiniteDuration = 100.milliseconds,
    timeout: FiniteDuration = 5.seconds
  ): Socket = {

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
            Thread.sleep(period.toMillis)
            create(stopAt)
          }
      }
    }

    create(System.currentTimeMillis() + timeout.toMillis)
  }

  def compile(
    workspace: os.Path,
    classesDir: os.Path,
    projectName: String,
    buildClient: bsp4j.BuildClient,
    threads: BloopThreads,
    logger: Logger,
    bloopVersion: String = Constants.bloopVersion
  ): Boolean = {

    val config = bloopgun.BloopgunConfig.default.copy(
      bspStdout = logger.bloopBspStdout,
      bspStderr = logger.bloopBspStderr
    )

    ensureBloopRunning(config, threads.startServerChecks, logger)

    val bloopgunLogger = logger.bloopgunLogger

    logger.debug("Opening BSP connection with bloop")
    val conn = bloopgun.Bloopgun.bsp(
      config,
      (workspace / ".scala").toNIO,
      bloopgunLogger
    )
    logger.debug(s"Bloop BSP connection waiting at ${conn.address}")

    val socket = connect(conn)

    logger.debug(s"Connected to Bloop via BSP at ${conn.address}")

    val launcher = new jsonrpc.Launcher.Builder[FullBuildServer]()
      .setExecutorService(threads.jsonrpc)
      .setInput(socket.getInputStream)
      .setOutput(socket.getOutputStream)
      .setRemoteInterface(classOf[FullBuildServer])
      .setLocalService(buildClient)
      .create()
    val server = launcher.getRemoteProxy
    buildClient.onConnectWithServer(server)

    val f = launcher.startListening()

    val initParams = new bsp4j.InitializeBuildParams(
      "scala-cli",
      Constants.version,
      Constants.bspVersion,
      (workspace / ".scala").toNIO.toUri.toASCIIString,
      new bsp4j.BuildClientCapabilities(List("scala", "java").asJava)
    )
    val bloopExtraParams = new BloopExtraBuildParams
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

    // Close the jsonrpc thread listening to input messages
    // First line makes jsonrpc discard the closed connection exception.
    f.cancel(true)
    socket.close()

    val success = compileRes.getStatusCode == bsp4j.StatusCode.OK
    logger.debug(if (success) "Compilation succeeded" else "Compilation failed")
    success
  }

}
