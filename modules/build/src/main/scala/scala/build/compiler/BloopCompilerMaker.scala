package scala.build.compiler

import ch.epfl.scala.bsp4j.BuildClient

import scala.build.Logger
import scala.build.bloop.{BloopServer, BloopThreads}
import scala.build.blooprifle.BloopRifleConfig
import scala.build.internal.Constants
import scala.concurrent.duration.DurationInt

final class BloopCompilerMaker(
  config: BloopRifleConfig,
  threads: BloopThreads,
  strictBloopJsonCheck: Boolean
) extends ScalaCompilerMaker {
  def create(
    workspace: os.Path,
    classesDir: os.Path,
    buildClient: BuildClient,
    logger: Logger
  ): BloopCompiler = {
    val buildServer = BloopServer.buildServer(
      config,
      "scala-cli",
      Constants.version,
      workspace.toNIO,
      classesDir.toNIO,
      buildClient,
      threads,
      logger.bloopRifleLogger
    )
    new BloopCompiler(buildServer, 20.seconds, strictBloopJsonCheck)
  }
}
