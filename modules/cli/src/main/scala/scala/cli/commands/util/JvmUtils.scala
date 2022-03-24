package scala.cli.commands
package util

import java.io.File

import scala.build.options.JavaOptions
import scala.build.{Os, Position, Positioned}
import scala.util.Properties

object JvmUtils {
  def javaOptions(opts: SharedJvmOptions) = {
    import opts._

    val (javacFilePlugins, javacPluginDeps) =
      javacPlugin
        .filter(_.trim.nonEmpty)
        .partition { input =>
          input.contains(File.separator) ||
          (Properties.isWin && input.contains("/")) ||
          input.count(_ == ':') < 2
        }

    JavaOptions(
      javaHomeOpt = javaHome.filter(_.nonEmpty).map(v =>
        Positioned(Seq(Position.CommandLine("--java-home")), os.Path(v, Os.pwd))
      ),
      jvmIdOpt = jvm.filter(_.nonEmpty),
      jvmIndexOpt = jvmIndex.filter(_.nonEmpty),
      jvmIndexOs = jvmIndexOs.map(_.trim).filter(_.nonEmpty),
      jvmIndexArch = jvmIndexArch.map(_.trim).filter(_.nonEmpty),
      javacPluginDependencies = SharedOptionsUtil.parseDependencies(
        javacPluginDeps.map(Positioned.none(_)),
        ignoreErrors = false
      ),
      javacPlugins = javacFilePlugins.map(s => Positioned.none(os.Path(s, Os.pwd))),
      javacOptions = javacOption
    )
  }
}
