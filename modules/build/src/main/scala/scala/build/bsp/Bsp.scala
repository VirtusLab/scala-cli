package scala.build.bsp

import java.io.{InputStream, OutputStream}

import scala.build.errors.BuildException
import scala.build.input.{Inputs, ScalaCliInvokeData}
import scala.concurrent.Future

trait Bsp {
  def run(initialInputs: Inputs, initialBspOptions: BspReloadableOptions): Future[Unit]
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
  )(using ScalaCliInvokeData): Bsp =
    new BspImpl(
      argsToInputs,
      bspReloadableOptionsReference,
      threads,
      in,
      out,
      actionableDiagnostics
    )
}
