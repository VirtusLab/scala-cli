package scala.build.tests.util

import bloop.rifle.{BloopRifle, BloopRifleConfig}
import coursier.cache.FileCache

import scala.build.internals.EnvVar
import scala.build.tests.TestLogger
import scala.build.{Bloop, BuildThreads, Logger, retry}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
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
      v => Bloop.bloopClassPath(Logger.nop, FileCache(), v).map(_.map(_.toPath)),
      directories.bloopWorkingDir.toNIO
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

  /** Makes sure the shared Bloop server is running, spawning it if necessary (once per JVM).
    *
    * Spawning Bloop can get stuck on a busy CI runner (i.e. when several test JVMs try to spawn it
    * at the same time). bloop-rifle only gives up on a stuck spawn after `startCheckTimeout + 30s`,
    * which is longer than munit's per-test timeout, so a stuck spawn in the first test of a suite
    * fails that test before the build gets the chance to retry. Doing it here instead (from a suite
    * fixture, to which munit doesn't apply its per-test timeout) allows to retry it safely.
    */
  private lazy val ensureRunning: Unit = {
    val logger  = TestLogger()
    val threads = BuildThreads.createBloopThreads()
    try
      retry()(logger) {
        if !BloopRifle.check(bloopConfig, logger.bloopRifleLogger) then
          logger.message("Bloop server not running, spawning it before running the tests…")
          val spawned = BloopRifle.startServer(
            bloopConfig,
            threads.startServerChecks,
            logger.bloopRifleLogger,
            bloopConfig.retainedBloopVersion.version.raw,
            bloopConfig.javaPath
          )
          Await.result(spawned, bloopConfig.startCheckTimeout + 30.seconds)
      }
    finally threads.shutdown()
  }

  /** Suite fixture spawning the shared Bloop server before the suite's tests run, outside of
    * munit's per-test timeout.
    */
  val fixture: munit.Fixture[Unit] = new munit.Fixture[Unit]("bloop-server") {
    def apply(): Unit              = ()
    override def beforeAll(): Unit = ensureRunning
  }

  /** Mix into suites compiling with the shared Bloop server. */
  trait Suite extends munit.FunSuite {
    override def munitFixtures: Seq[munit.AnyFixture[?]] = super.munitFixtures :+ fixture
  }
}
