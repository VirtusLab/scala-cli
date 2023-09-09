package scala.build.compiler

import bloop.rifle.{BloopRifleConfig, BloopServer, BloopThreads}
import ch.epfl.scala.bsp4j.BuildClient

import scala.build.Logger
import scala.build.errors.{FetchingDependenciesError, Severity}
import scala.build.internal.Constants
import scala.build.internal.util.WarningMessages
import scala.concurrent.duration.DurationInt
import scala.util.Try

final class BloopCompilerMaker(
  config: BloopRifleConfig,
  threads: BloopThreads,
  strictBloopJsonCheck: Boolean,
  offline: Boolean
) extends ScalaCompilerMaker {
  def create(
    workspace: os.Path,
    classesDir: os.Path,
    buildClient: BuildClient,
    logger: Logger
  ): ScalaCompiler = {
    val createBuildServer =
      () =>
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

    Try(new BloopCompiler(createBuildServer, 20.seconds, strictBloopJsonCheck))
      .toEither
      .left.flatMap {
        case e if offline =>
          e.getCause match
            case _: FetchingDependenciesError =>
              logger.diagnostic(
                WarningMessages.offlineModeBloopNotFound,
                Severity.Warning
              )
              Right(
                SimpleScalaCompilerMaker("java", Nil)
                  .create(workspace, classesDir, buildClient, logger)
              )
            case _ => Left(e)
        case e => Left(e)
      }.fold(t => throw t, identity)
  }
}
