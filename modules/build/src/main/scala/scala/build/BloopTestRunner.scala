package scala.build

import bloop.rifle.{BloopRifleConfig, BloopServer}

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.concurrent.duration.DurationInt
import scala.util.Try

object BloopTestRunner {
  private final class BloopTestFailedError(message: String, cause: Throwable = null)
      extends BuildException(message, cause = cause)

  def run(
    build: Build.Successful,
    bloopConfig: BloopRifleConfig,
    threads: BuildThreads,
    logger: Logger,
    requireTests: Boolean,
    args: Seq[String]
  ): Either[BuildException, Int] = either {
    val buildClient = BloopTestBuildClient.create(logger)
    val workspace   = build.inputs.workspace / Constants.workspaceDirName
    val classesDir  = Build.classesRootDir(build.inputs.workspace, build.inputs.projectName)

    val server = value {
      Try {
        retry()(logger) {
          BloopServer.buildServer(
            bloopConfig,
            "scala-cli",
            Constants.version,
            workspace.toNIO,
            classesDir.toNIO,
            buildClient,
            threads.bloop,
            logger.bloopRifleLogger
          )
        }
      }.toEither.left.map(ex =>
        new BloopTestFailedError("Failed to connect to Bloop for running tests", ex)
      )
    }

    try {
      val testOptions = value(prepareTestOptions(build, args, logger))
      val exitCode    = value {
        Bloop
          .test(
            build.project.projectName,
            server.server,
            logger,
            20.seconds,
            testOptions
          )
          .left
          .map(ex => new BloopTestFailedError("Bloop test execution failed", ex))
      }
      if requireTests && buildClient.testsRan == 0 && testOptions.selectedTestClasses.isEmpty
      then {
        logger.error("Error: no tests were run.")
        1
      }
      else exitCode
    }
    finally server.shutdown()
  }

  private def prepareTestOptions(
    build: Build.Successful,
    args: Seq[String],
    logger: Logger
  ): Either[BuildException, Bloop.BloopTestOptions] = {
    val testOnly            = build.options.testOptions.testOnly
    val selectedTestClasses = testOnly match {
      case None       => Nil
      case Some(glob) =>
        BloopTestClassDiscovery.matchingTestClasses(
          build.fullClassPath.map(_.toNIO),
          glob,
          logger
        )
    }
    Right(
      Bloop.BloopTestOptions(
        testOnly = testOnly,
        selectedTestClasses = selectedTestClasses,
        extraArgs = args
      )
    )
  }
}
