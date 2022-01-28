package scala.build.tests.util

import scala.build.{Bloop, Logger}
import scala.build.blooprifle.BloopRifleConfig
import scala.util.Properties

object BloopServer {

  private def directories = scala.build.Directories.default()

  // FIXME We could use a test-specific Bloop instance here.
  // Not sure how to properly shut it down or have it exit after a period
  // of inactivity, so we keep using our default global Bloop for now.
  private def bloopAddress =
    if (Properties.isWin)
      BloopRifleConfig.Address.Tcp(BloopRifleConfig.defaultHost, BloopRifleConfig.defaultPort)
    else
      BloopRifleConfig.Address.DomainSocket(directories.bloopDaemonDir.toNIO)

  val bloopConfig = BloopRifleConfig.default(
    bloopAddress,
    v => Bloop.bloopClassPath(Logger.nop, v),
    directories.bloopWorkingDir.toIO
  )

}
