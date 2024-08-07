package scala.build.tests.util

import bloop.rifle.BloopRifleConfig
import coursier.cache.FileCache

import scala.build.internals.EnvVar
import scala.build.{Bloop, Logger}
import scala.util.Properties

object BloopServer {

  private def directories = scala.build.Directories.default()

  // FIXME We could use a test-specific Bloop instance here.
  // Not sure how to properly shut it down or have it exit after a period
  // of inactivity, so we keep using our default global Bloop for now.
  private def bloopAddress =
    BloopRifleConfig.Address.DomainSocket(directories.bloopDaemonDir.toNIO)

  val bloopConfig = {
    val base = BloopRifleConfig.default(
      bloopAddress,
      v => Bloop.bloopClassPath(Logger.nop, FileCache(), v),
      directories.bloopWorkingDir.toIO
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

}
