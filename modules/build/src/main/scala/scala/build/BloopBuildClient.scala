package scala.build

import ch.epfl.scala.bsp4j

import java.io.PrintStream

import scala.build.options.Scope

trait BloopBuildClient extends bsp4j.BuildClient {
  def setProjectParams(newParams: Seq[String]): Unit
  def setGeneratedSources(scope: Scope, newGeneratedSources: Seq[GeneratedSource]): Unit
  def diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]]
  def clear(): Unit
}

object BloopBuildClient {
  def create(
    logger: Logger
  ): BloopBuildClient =
    create(logger, out = System.err, keepDiagnostics = false)
  def create(
    logger: Logger,
    keepDiagnostics: Boolean
  ): BloopBuildClient =
    create(
      logger,
      out = logger.compilerOutputStream,
      keepDiagnostics = keepDiagnostics
    )
  def create(
    logger: Logger,
    out: PrintStream,
    keepDiagnostics: Boolean
  ): BloopBuildClient =
    new ConsoleBloopBuildClient(
      logger,
      out,
      keepDiagnostics
    )
}
