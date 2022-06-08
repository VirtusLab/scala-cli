package scala.cli.commands

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._


// format: off
final case class SharedCompilationServerOptions(
  @Group("Compilation server")
  @HelpMessage("Protocol to use to open a BSP connection with Bloop")
  @ValueDescription("tcp|local|default")
  @Hidden
    bloopBspProtocol: Option[String] = None,
  @Group("Compilation server")
  @HelpMessage("Socket file to use to open a BSP connection with Bloop")
  @ValueDescription("path")
  @Hidden
    bloopBspSocket: Option[String] = None,

  @Group("Compilation server")
  @HelpMessage("Host the compilation server should bind to")
  @ValueDescription("host")
  @Hidden
    bloopHost: Option[String] = None,
  @Group("Compilation server")
  @HelpMessage("Port the compilation server should bind to (pass `-1` to pick a random port)")
  @ValueDescription("port|-1")
  @Hidden
    bloopPort: Option[Int] = None,
  @Group("Compilation server")
  @HelpMessage("Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)")
  @ValueDescription("path")
  @Hidden
    bloopDaemonDir: Option[String] = None,

  @Group("Compilation server")
  @HelpMessage("If Bloop isn't already running, the version we should start")
  @ValueDescription("version")
  @Hidden
    bloopVersion: Option[String] = None,

  @Hidden
  @Group("Compilation server")
  @HelpMessage("Maximum duration to wait for the BSP connection to be opened")
  @ValueDescription("duration")
    bloopBspTimeout: Option[String] = None,
  @Hidden
  @Group("Compilation server")
  @HelpMessage("Duration between checks of the BSP connection state")
  @ValueDescription("duration")
    bloopBspCheckPeriod: Option[String] = None,
  @Hidden
  @Group("Compilation server")
  @HelpMessage("Maximum duration to wait for the compilation server to start up")
  @ValueDescription("duration")
    bloopStartupTimeout: Option[String] = None,

  @Group("Compilation server")
  @HelpMessage("Include default JVM options for Bloop")
  @Hidden
    bloopDefaultJavaOpts: Boolean = true,
  @Group("Compilation server")
  @Hidden
    bloopJavaOpt: List[String] = Nil,
  @Group("Compilation server")
  @HelpMessage("Bloop global options file")
  @Hidden
    bloopGlobalOptionsFile: Option[String] = None,

  @Group("Compilation server")
  @HelpMessage("JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)")
  @Hidden
    bloopJvm: Option[String] = None,

  @Group("Compilation server")
  @HelpMessage("Working directory for Bloop, if it needs to be started")
  @Hidden
    bloopWorkingDir: Option[String] = None,

  @Group("Compilation server")
  @HelpMessage("Enable / disable compilation server")
  @Hidden
    server: Option[Boolean] = None
)
// format: on

object SharedCompilationServerOptions {
  lazy val parser: Parser[SharedCompilationServerOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedCompilationServerOptions, parser.D] = parser
  implicit lazy val help: Help[SharedCompilationServerOptions]                      = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedCompilationServerOptions] = JsonCodecMaker.make
}
