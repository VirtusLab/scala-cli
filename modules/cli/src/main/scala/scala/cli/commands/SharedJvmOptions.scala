package scala.cli.commands

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import java.io.File

import scala.build.options.JavaOptions
import scala.build.{Os, Position, Positioned}
import scala.util.Properties

// format: off
final case class SharedJvmOptions(

  @Group("Java")
  @HelpMessage("Set the Java home directory")
  @ValueDescription("path")
    javaHome: Option[String] = None,

  @Group("Java")
  @HelpMessage("Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`")
  @ValueDescription("jvm-name")
  @Name("j")
    jvm: Option[String] = None,
  @Group("Java")
  @HelpMessage("JVM index URL")
  @ValueDescription("url")
  @Hidden
    jvmIndex: Option[String] = None,
  @Group("Java")
  @HelpMessage("Operating system to use when looking up in the JVM index")
  @ValueDescription("linux|linux-musl|darwin|windows|…")
  @Hidden
    jvmIndexOs: Option[String] = None,
  @Group("Java")
  @HelpMessage("CPU architecture to use when looking up in the JVM index")
  @ValueDescription("amd64|arm64|arm|…")
  @Hidden
    jvmIndexArch: Option[String] = None,

  @Group("Java")
  @HelpMessage("Javac plugin dependencies or files")
  @Hidden
    javacPlugin: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Javac options")
  @Name("javacOpt")
  @Hidden
    javacOption: List[String] = Nil
) {
  // format: on

  private lazy val (javacFilePlugins, javacPluginDeps) =
    javacPlugin
      .filter(_.trim.nonEmpty)
      .partition { input =>
        input.contains(File.separator) ||
        (Properties.isWin && input.contains("/")) ||
        input.count(_ == ':') < 2
      }

  def javaOptions = JavaOptions(
    javaHomeOpt = javaHome.filter(_.nonEmpty).map(v =>
      Positioned(Seq(Position.CommandLine("--java-home")), os.Path(v, Os.pwd))
    ),
    jvmIdOpt = jvm.filter(_.nonEmpty),
    jvmIndexOpt = jvmIndex.filter(_.nonEmpty),
    jvmIndexOs = jvmIndexOs.map(_.trim).filter(_.nonEmpty),
    jvmIndexArch = jvmIndexArch.map(_.trim).filter(_.nonEmpty),
    javacPluginDependencies = SharedOptions.parseDependencies(
      javacPluginDeps.map(Positioned.none(_)),
      ignoreErrors = false
    ),
    javacPlugins = javacFilePlugins.map(s => Positioned.none(os.Path(s, Os.pwd))),
    javacOptions = javacOption
  )

}

object SharedJvmOptions {
  lazy val parser: Parser[SharedJvmOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedJvmOptions, parser.D] = parser
  implicit lazy val help: Help[SharedJvmOptions]                      = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedJvmOptions]       = JsonCodecMaker.make
}
