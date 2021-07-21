package scala.cli.commands

import caseapp._

final case class SharedJvmOptions(
  @Group("Java")
  @HelpMessage("Use a specific JVM, such as 14, adopt:11, or graalvm:21, or system")
  @ValueDescription("jvm-name")
  @Name("j")
    jvm: Option[String] = None,
  @Group("Java")
  @HelpMessage("JVM index URL")
  @ValueDescription("url")
    jvmIndex: Option[String] = None,
  @Group("Java")
  @HelpMessage("Operating system to use when looking up in the JVM index")
  @ValueDescription("linux|linux-musl|darwin|windows|…")
    jvmIndexOs: Option[String] = None,
  @Group("Java")
  @HelpMessage("CPU architecture to use when looking up in the JVM index")
  @ValueDescription("amd64|arm64|arm|…")
    jvmIndexArch: Option[String] = None
)
