package scala.build.compiler

import ch.epfl.scala.bsp4j.BuildClient

import scala.build.Logger

trait ScalaCompilerMaker {
  def create(
    workspace: os.Path,
    classesDir: os.Path,
    buildClient: BuildClient,
    logger: Logger
  ): ScalaCompiler

  final def withCompiler[T](
    workspace: os.Path,
    classesDir: os.Path,
    buildClient: BuildClient,
    logger: Logger
  )(
    f: ScalaCompiler => T
  ): T = {
    var server: ScalaCompiler = null
    try {
      server = create(
        workspace,
        classesDir,
        buildClient,
        logger
      )
      f(server)
    }
    // format: off
    finally {
      if (server != null)
        server.shutdown()
    }
    // format: on
  }
}

object ScalaCompilerMaker {
  final case class IgnoreScala2(compilerMaker: ScalaCompilerMaker) extends ScalaCompilerMaker {
    def create(
      workspace: os.Path,
      classesDir: os.Path,
      buildClient: BuildClient,
      logger: Logger
    ): ScalaCompiler =
      ScalaCompiler.IgnoreScala2(
        compilerMaker.create(workspace, classesDir, buildClient, logger)
      )
  }
}
