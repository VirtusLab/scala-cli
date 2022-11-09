package scala.cli.commands
package util

import com.github.plokhotnyuk.jsoniter_scala.core._
import coursier.cache.FileCache
import coursier.core.{Version => Ver}
import coursier.util.Task

import java.io.File
import java.nio.file.{AtomicMoveNotSupportedException, FileAlreadyExistsException, Files, Paths}
import java.util.Random

import scala.build.blooprifle.internal.Constants
import scala.build.blooprifle.{BloopRifleConfig, BloopVersion, BspConnectionAddress}
import scala.build.internal.Util
import scala.build.{Bloop, Logger, Os}
import scala.cli.commands.bloop.BloopJson
import scala.cli.internal.Pid
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Properties

object SharedCompilationServerOptionsUtil {

  implicit class SharedCompilationServerOptionsOps(private val v: SharedCompilationServerOptions)
      extends AnyVal {
    import v._

    private def pidOrRandom: Either[Int, Int] = cached((v, "pid")) {
      Option((new Pid).get()).map(_.intValue()).map(Right(_)).getOrElse {
        val r = new Random
        Left(r.nextInt())
      }
    }

    private def socketDirectory(directories: scala.build.Directories): os.Path = {
      val dir = directories.bspSocketDir
      // Ensuring that whenever dir exists, it has the right permissions
      if (!os.isDir(dir)) {
        val tmpDir = dir / os.up / s".${dir.last}.tmp-${pidOrRandom.merge}"
        try {
          os.makeDir.all(tmpDir)
          if (!Properties.isWin)
            os.perms.set(tmpDir, "rwx------")
          try os.move(tmpDir, dir, atomicMove = true)
          catch {
            case _: AtomicMoveNotSupportedException =>
              try os.move(tmpDir, dir)
              catch {
                case _: FileAlreadyExistsException =>
              }
            case _: FileAlreadyExistsException =>
          }
        }
        finally if (os.exists(tmpDir)) os.remove(tmpDir)
      }
      dir
    }

    private def bspSocketFile(directories: => scala.build.Directories): File = {
      val (socket, deleteOnExit) = bloopBspSocket match {
        case Some(path) =>
          (os.Path(path, Os.pwd), false)
        case None =>
          val dir = socketDirectory(directories)
          val fileName = pidOrRandom
            .map("proc-" + _)
            .left.map("conn-" + _)
            .merge
          val path = dir / fileName
          if (os.exists(path)) // isFile is false for domain sockets
            os.remove(path)
          (path, true)
      }
      if (deleteOnExit)
        Runtime.getRuntime.addShutdownHook(
          new Thread("delete-bloop-bsp-named-socket") {
            override def run() =
              Files.deleteIfExists(socket.toNIO)
          }
        )
      socket.toIO.getCanonicalFile
    }

    def defaultBspSocketOrPort(
      directories: => scala.build.Directories
    ): Option[() => BspConnectionAddress] = {
      def namedSocket =
        Some(() => BspConnectionAddress.UnixDomainSocket(bspSocketFile(directories)))

      def default = namedSocket
      bloopBspProtocol.filter(_ != "default") match {
        case None          => default
        case Some("tcp")   => None
        case Some("local") => namedSocket
        case Some(other) =>
          sys.error(
            s"Invalid bloop BSP protocol value: '$other' (expected 'tcp', 'local', or 'default')"
          )
      }
    }

    private def parseDuration(name: String, valueOpt: Option[String]): Option[FiniteDuration] =
      valueOpt.map(_.trim).filter(_.nonEmpty).map(Duration(_)).map {
        case d: FiniteDuration => d
        case d                 => sys.error(s"Expected finite $name duration, got $d")
      }

    def bloopBspTimeoutDuration: Option[FiniteDuration] =
      parseDuration("BSP connection timeout", bloopBspTimeout)
    def bloopBspCheckPeriodDuration: Option[FiniteDuration] =
      parseDuration("BSP connection check period", bloopBspCheckPeriod)
    def bloopStartupTimeoutDuration: Option[FiniteDuration] =
      parseDuration("connection server startup timeout", bloopStartupTimeout)

    def retainedBloopVersion: BloopRifleConfig.BloopVersionConstraint = cached(v) {
      bloopVersion
        .map(_.trim)
        .filter(_.nonEmpty)
        .fold[BloopRifleConfig.BloopVersionConstraint](BloopRifleConfig.AtLeast(
          BloopVersion(Constants.bloopVersion)
        ))(v => BloopRifleConfig.Strict(BloopVersion(v)))
    }

    def bloopDefaultJvmOptions(logger: Logger): Option[List[String]] = {
      val filePathOpt = bloopGlobalOptionsFile.filter(_.trim.nonEmpty).map(os.Path(_, Os.pwd))
      for (filePath <- filePathOpt)
        yield
          if (os.exists(filePath) && os.isFile(filePath))
            try {
              val content   = os.read.bytes(filePath)
              val bloopJson = readFromArray(content)(BloopJson.codec)
              bloopJson.javaOptions
            }
            catch {
              case e: Throwable =>
                logger.message(s"Error parsing global bloop config in '$filePath':")
                Util.printException(e)
                List.empty
            }
          else {
            logger.message(s"Bloop global options file '$filePath' not found.")
            List.empty
          }
    }

    def bloopRifleConfig(
      logger: Logger,
      cache: FileCache[Task],
      verbosity: Int,
      javaPath: String,
      directories: => scala.build.Directories,
      javaV: Option[Int] = None
    ): BloopRifleConfig = {

      val portOpt = bloopPort.filter(_ != 0) match {
        case Some(n) if n < 0 =>
          Some(scala.build.blooprifle.internal.Util.randomPort())
        case other => other
      }
      val address =
        (
          bloopHost.filter(_.nonEmpty),
          portOpt,
          bloopDaemonDir.filter(_.nonEmpty)
        ) match {
          case (_, _, Some(path)) =>
            BloopRifleConfig.Address.DomainSocket(Paths.get(path))
          case (None, None, None) =>
            val isBloopMainLine = Ver(retainedBloopVersion.version.raw) < Ver("1.4.12")
            if (isBloopMainLine)
              BloopRifleConfig.Address.Tcp(
                host = BloopRifleConfig.defaultHost,
                port = BloopRifleConfig.defaultPort
              )
            else
              BloopRifleConfig.Address.DomainSocket(directories.bloopDaemonDir.toNIO)
          case (hostOpt, portOpt0, _) =>
            BloopRifleConfig.Address.Tcp(
              host = hostOpt.getOrElse(BloopRifleConfig.defaultHost),
              port = portOpt0.getOrElse(BloopRifleConfig.defaultPort)
            )
        }

      val workingDir = bloopWorkingDir
        .filter(_.trim.nonEmpty)
        .map(os.Path(_, Os.pwd))
        .getOrElse(directories.bloopWorkingDir)
      val baseConfig = BloopRifleConfig.default(
        address,
        v => Bloop.bloopClassPath(logger, cache, v),
        workingDir.toIO
      )

      baseConfig.copy(
        javaPath = javaPath,
        bspSocketOrPort = defaultBspSocketOrPort(directories),
        bspStdout = if (verbosity >= 3) Some(System.err) else None,
        bspStderr = Some(System.err),
        period = bloopBspCheckPeriodDuration.getOrElse(baseConfig.period),
        timeout = bloopBspTimeoutDuration.getOrElse(baseConfig.timeout),
        initTimeout = bloopStartupTimeoutDuration.getOrElse(baseConfig.initTimeout),
        javaOpts =
          (if (bloopDefaultJavaOpts) baseConfig.javaOpts
           else Nil) ++ bloopJavaOpt ++ bloopDefaultJvmOptions(logger).getOrElse(Nil),
        minimumBloopJvm = javaV.getOrElse(8),
        retainedBloopVersion = retainedBloopVersion
      )
    }
  }
}
