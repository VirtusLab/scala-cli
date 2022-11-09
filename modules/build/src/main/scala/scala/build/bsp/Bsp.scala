package scala.build.bsp

import java.io.{InputStream, OutputStream}

import scala.build.errors.BuildException
import scala.build.input.Inputs
import scala.concurrent.Future

trait Bsp {
  def run(initialInputs: Inputs): Future[Unit]
  def shutdown(): Unit
}

object Bsp {
  def create(
    argsToInputs: Seq[String] => Either[BuildException, Inputs],
    bspReloadableOptionsReference: BspReloadableOptions.Reference,
    threads: BspThreads,
    in: InputStream,
    out: OutputStream,
    actionableDiagnostics: Option[Boolean]
  ): Bsp =
    new BspImpl(
      argsToInputs,
      bspReloadableOptionsReference,
      threads,
      in,
      out,
      actionableDiagnostics
    )
}
