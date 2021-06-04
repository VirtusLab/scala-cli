package scala.cli.commands.packager

import scala.build.Logger

trait NativePackager {
  def run(logger: Logger): Unit
}
