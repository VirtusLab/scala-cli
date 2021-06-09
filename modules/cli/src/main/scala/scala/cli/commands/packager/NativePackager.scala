package scala.cli.commands.packager

import scala.build.Logger

trait NativePackager {

  def sourceAppPath: os.Path
  def packageName: String

  protected val basePath = sourceAppPath / os.RelPath("../")

  def run(logger: Logger): Unit
}
