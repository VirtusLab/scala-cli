package scala.build.bloop

import java.io.IOException
import java.net.{ConnectException, Socket}
import java.nio.file.{Files, Path}
import java.util.concurrent.{Future => JFuture, ScheduledExecutorService, TimeoutException}

import ch.epfl.scala.bsp4j
import org.eclipse.lsp4j.jsonrpc

import scala.annotation.tailrec
import scala.build.bloop.bloop4j.BloopExtraBuildParams
import scala.build.blooprifle.internal.Constants
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.build.blooprifle.{BloopRifle, BloopRifleConfig, BloopRifleLogger, BspConnection}

trait BloopServer {
  def server: BuildServer

  def shutdown(): Unit
}

object BloopServer {

  private case class BloopServerImpl(
    server: BuildServer,
    listeningFuture: JFuture[Void],
    socket: Socket
  ) extends BloopServer {
    def shutdown(): Unit = {
      // Close the jsonrpc thread listening to input messages
      // First line makes jsonrpc discard the closed connection exception.
      listeningFuture.cancel(true)
      socket.close()
    }
  }

  private def ensureBloopRunning(
    config: BloopRifleConfig,
    startServerChecksPool: ScheduledExecutorService,
    logger: BloopRifleLogger
  ): Unit = {

    val isBloopRunning = BloopRifle.check(config, logger, startServerChecksPool)

    logger.debug(
      if (isBloopRunning) s"Bloop is running on ${config.host}:${config.port}"
      else s"No bloop daemon found on ${config.host}:${config.port}"
    )

    if (!isBloopRunning) {
      logger.debug("Starting bloop server")
      val serverStartedFuture = BloopRifle.startServer(
        config,
        startServerChecksPool,
        logger
      )

      Await.result(serverStartedFuture, Duration.Inf)
      logger.debug("Bloop server started")
    }

  }

  private def connect(
    conn: BspConnection,
    period: FiniteDuration,
    timeout: FiniteDuration
  ): Socket = {

    @tailrec
    def create(stopAt: Long): Socket = {
      val maybeSocket =
        try Right(conn.openSocket(period, timeout))
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

  def bsp(
    config: BloopRifleConfig,
    workspace: Path,
    threads: BloopThreads,
    logger: BloopRifleLogger,
    period: FiniteDuration,
    timeout: FiniteDuration
  ): (BspConnection, Socket) = {

    ensureBloopRunning(config, threads.startServerChecks, logger)

    logger.debug("Opening BSP connection with bloop")
    Files.createDirectories(workspace.resolve(".scala/.bloop"))
    val conn = BloopRifle.bsp(
      config,
      workspace.resolve(".scala"),
      logger
    )
    logger.debug(s"Bloop BSP connection waiting at ${conn.address}")

    val socket = connect(conn, period, timeout)

    logger.debug(s"Connected to Bloop via BSP at ${conn.address}")

    (conn, socket)
  }

  def buildServer(
    config: BloopRifleConfig,
    clientName: String,
    clientVersion: String,
    workspace: Path,
    classesDir: Path,
    buildClient: bsp4j.BuildClient,
    threads: BloopThreads,
    logger: BloopRifleLogger
  ): BloopServer = {

    val (conn, socket) = bsp(config, workspace, threads, logger, config.period, config.timeout)

    logger.debug(s"Connected to Bloop via BSP at ${conn.address}")

    // FIXME As of now, we don't detect when connection gets closed.
    // For TCP connections, this should be do-able with heartbeat messages
    // (to be added to BSP?).
    // For named sockets, the recv system call is supposed to allow to detect
    // that case, unlike the read system call. But the ipcsocket library that we use
    // for named sockets relies on read.

    val launcher = new jsonrpc.Launcher.Builder[BuildServer]()
      .setExecutorService(threads.jsonrpc)
      .setInput(socket.getInputStream)
      .setOutput(socket.getOutputStream)
      .setRemoteInterface(classOf[BuildServer])
      .setLocalService(buildClient)
      .create()
    val server = launcher.getRemoteProxy
    buildClient.onConnectWithServer(server)

    val f = launcher.startListening()

    val initParams = new bsp4j.InitializeBuildParams(
      clientName,
      clientVersion,
      Constants.bspVersion,
      workspace.resolve(".scala").toUri.toASCIIString,
      new bsp4j.BuildClientCapabilities(List("scala", "java").asJava)
    )
    val bloopExtraParams = new BloopExtraBuildParams
    bloopExtraParams.setClientClassesRootDir(classesDir.toUri.toASCIIString)
    bloopExtraParams.setOwnsBuildFiles(true)
    initParams.setData(bloopExtraParams)
    logger.debug("Sending buildInitialize BSP command to Bloop")
    try server.buildInitialize(initParams).get(config.initTimeout.length, config.initTimeout.unit)
    catch {
      case ex: TimeoutException =>
        throw new Exception("Timeout while waiting for buildInitialize response", ex)
    }

    server.onBuildInitialized()
    BloopServerImpl(server, f, socket)
  }

  def withBuildServer[T](
    config: BloopRifleConfig,
    clientName: String,
    clientVersion: String,
    workspace: Path,
    classesDir: Path,
    buildClient: bsp4j.BuildClient,
    threads: BloopThreads,
    logger: BloopRifleLogger
  )(f: BloopServer => T): T = {
    var server: BloopServer = null
    try {
      server = buildServer(
        config,
        clientName,
        clientVersion,
        workspace,
        classesDir,
        buildClient,
        threads,
        logger
      )
      f(server)
    }
    finally {
      if (server != null)
        server.shutdown()
    }
  }

}
