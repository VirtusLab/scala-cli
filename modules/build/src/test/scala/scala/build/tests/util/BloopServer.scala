package scala.build.tests.util

import bloop.rifle.{BloopRifle, BloopRifleConfig}
import coursier.cache.FileCache

import scala.build.internals.EnvVar
import scala.build.{Bloop, Logger}
import scala.util.Properties
import scala.util.control.NonFatal

object BloopServer {

  private val baseDir = os.temp.dir(prefix = "scli-bloop-")

  private val bloopAddress =
    BloopRifleConfig.Address.DomainSocket((baseDir / "daemon").toNIO)

  val bloopConfig = {
    val base = BloopRifleConfig.default(
      bloopAddress,
      v => Bloop.bloopClassPath(Logger.nop, FileCache(), v).map(_.map(_.toPath)),
      (baseDir / "bloop").toNIO
    )
    base.copy(
      javaPath =
        if (Properties.isWin) base.javaPath
        else
          // On Linux / macOS, we start the Bloop server via /bin/sh,
          // which can have issues with the directory of "java" in the PATH,
          // if it contains '+' or '%' IIRC.
          // So we hardcode the path to "java" here.
          EnvVar.Java.javaHome.valueOpt
            .map(os.Path(_, os.pwd))
            .map(_ / "bin" / "java")
            .map(_.toString)
            .getOrElse(base.javaPath)
    )
  }

  Runtime.getRuntime.addShutdownHook(
    new Thread("scli-bloop-test-shutdown") {
      setDaemon(true)
      override def run(): Unit =
        try
          if BloopRifle.check(bloopConfig, Logger.nop.bloopRifleLogger) then
            BloopRifle.exit(bloopConfig, os.pwd.toNIO, Logger.nop.bloopRifleLogger)
        catch
          case NonFatal(_) => ()
        try os.remove.all(baseDir)
        catch
          case NonFatal(_) => ()
    }
  )

}
