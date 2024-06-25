package scala.build.compiler

import ch.epfl.scala.bsp4j.BuildClient

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.BuildOptions

trait ScalaCompilerMaker {
  def create(
    workspace: os.Path,
    classesDir: os.Path,
    buildClient: BuildClient,
    logger: Logger,
    buildOptions: BuildOptions
  ): Either[BuildException, ScalaCompiler]

  final def withCompiler[T](
    workspace: os.Path,
    classesDir: os.Path,
    buildClient: BuildClient,
    logger: Logger,
    buildOptions: BuildOptions
  )(
    f: ScalaCompiler => Either[BuildException, T]
  ): Either[BuildException, T] = {
    var server: ScalaCompiler = null
    try {
      val createdServer = create(
        workspace,
        classesDir,
        buildClient,
        logger,
        buildOptions
      )
      server = createdServer.toOption.getOrElse(null)
      createdServer.flatMap(f)
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
      logger: Logger,
      buildOptions: BuildOptions
    ): Either[BuildException, ScalaCompiler] =
      compilerMaker.create(workspace, classesDir, buildClient, logger, buildOptions).map(
        ScalaCompiler.IgnoreScala2(_)
      )
  }
}
