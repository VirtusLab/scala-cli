package scala.cli.commands

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

// format: off
final case class SharedJvmOptions(
  @Recurse
    sharedDebug: SharedDebugOptions = SharedDebugOptions(),

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
    javacOption: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Port for BSP debugging")
  @Hidden
    bspDebugPort: Option[String] = None
)
// format: on

object SharedJvmOptions {
  implicit lazy val parser: Parser[SharedJvmOptions]            = Parser.derive
  implicit lazy val help: Help[SharedJvmOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedJvmOptions] = JsonCodecMaker.make
}
