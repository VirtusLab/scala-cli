package scala.build.compiler

import bloop.rifle.{BloopRifleConfig, BloopServer, BloopThreads}
import ch.epfl.scala.bsp4j.BuildClient

import scala.build.errors.{BuildException, FetchingDependenciesError, Severity}
import scala.build.internal.Constants
import scala.build.internal.util.WarningMessages
import scala.build.options.BuildOptions
import scala.build.{Logger, retry}
import scala.concurrent.duration.DurationInt
import scala.util.Try

final class BloopCompilerMaker(
  getConfig: BuildOptions => Either[BuildException, BloopRifleConfig],
  threads: BloopThreads,
  strictBloopJsonCheck: Boolean,
  offline: Boolean
) extends ScalaCompilerMaker {
  def create(
    workspace: os.Path,
    classesDir: os.Path,
    buildClient: BuildClient,
    logger: Logger,
    buildOptions: BuildOptions
  ): Either[BuildException, ScalaCompiler] =
    getConfig(buildOptions) match
      case Left(_) if offline =>
        logger.diagnostic(WarningMessages.offlineModeBloopJvmNotFound, Severity.Warning)
        SimpleScalaCompilerMaker("java", Nil).create(
          workspace,
          classesDir,
          buildClient,
          logger,
          buildOptions
        )
      case Right(config) =>
        val createBuildServer =
          () =>
            // retrying here in case a number of Scala CLI processes are started at the same time
            // and they all try to connect to the server / spawn a new server
            // otherwise, users may run into one of:
            //   - libdaemonjvm.client.ConnectError$ZombieFound
            //   - Caught java.lang.RuntimeException: Fatal error, could not spawn Bloop: not running
            //   - java.lang.RuntimeException: Bloop BSP connection in (...) was unexpectedly closed or bloop didn't start.
            // if a sufficiently large number of processes was started, this may happen anyway, of course
            retry(if offline then 1 else 3)(logger) {
              BloopServer.buildServer(
                config,
                "scala-cli",
                Constants.version,
                workspace.toNIO,
                classesDir.toNIO,
                buildClient,
                threads,
                logger.bloopRifleLogger
              )
            }

        val res = Try(new BloopCompiler(createBuildServer, 20.seconds, strictBloopJsonCheck))
          .toEither
          .left.flatMap {
            case e if offline =>
              e.getCause match
                case _: FetchingDependenciesError =>
                  logger.diagnostic(
                    WarningMessages.offlineModeBloopNotFound,
                    Severity.Warning
                  )
                  SimpleScalaCompilerMaker("java", Nil)
                    .create(workspace, classesDir, buildClient, logger, buildOptions)
                case _ => Left(e)
            case e => Left(e)
          }.fold(t => throw t, identity)
        Right(res)
      case Left(ex) => Left(ex)
}
