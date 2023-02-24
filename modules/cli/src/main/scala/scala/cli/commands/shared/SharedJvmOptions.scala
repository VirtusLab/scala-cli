package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.tags

// format: off
final case class SharedJvmOptions(
  @Recurse
    sharedDebug: SharedDebugOptions = SharedDebugOptions(),

  @Group(HelpGroup.Java.toString)
  @HelpMessage("Set the Java home directory")
  @Tag(tags.should)
  @ValueDescription("path")
    javaHome: Option[String] = None,

  @Group(HelpGroup.Java.toString)
  @HelpMessage("Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`")
  @ValueDescription("jvm-name")
  @Tag(tags.should)
  @Name("j")
  @Tag(tags.inShortHelp)
    jvm: Option[String] = None,
  @Group(HelpGroup.Java.toString)
  @HelpMessage("JVM index URL")
  @ValueDescription("url")
  @Tag(tags.implementation)
  @Hidden
    jvmIndex: Option[String] = None,
  @Group(HelpGroup.Java.toString)
  @HelpMessage("Operating system to use when looking up in the JVM index")
  @ValueDescription("linux|linux-musl|darwin|windows|…")
  @Tag(tags.implementation)
  @Hidden
    jvmIndexOs: Option[String] = None,
  @Group(HelpGroup.Java.toString)
  @HelpMessage("CPU architecture to use when looking up in the JVM index")
  @ValueDescription("amd64|arm64|arm|…")
  @Tag(tags.implementation)
  @Hidden
    jvmIndexArch: Option[String] = None,

  @Group(HelpGroup.Java.toString)
  @HelpMessage("Javac plugin dependencies or files")
  @Tag(tags.should)
  @Hidden
    javacPlugin: List[String] = Nil,

  @Group(HelpGroup.Java.toString)
  @HelpMessage("Javac options")
  @Name("javacOpt")
  @Tag(tags.should)
  @Tag(tags.inShortHelp)
  @Hidden
    javacOption: List[String] = Nil,

  @Group(HelpGroup.Java.toString)
  @Tag(tags.implementation)
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
