package scala.cli.export

import scala.build.options.BuildOptions
import scala.build.Sources

sealed abstract class BuildTool extends Product with Serializable {
  def export(options: BuildOptions, sources: Sources): Project
}
