package scala.cli.commands

import caseapp.*

import scala.cli.commands.common.HasSharedOptions

// format: off
@HelpMessage("Browse Scala code and its dependencies in the browser")
final case class MetabrowseOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @HelpMessage("Download and add `rt.jar` in the class path")
  @Tag(tags.experimental)
    addRtJar: Option[Boolean] = None,

  @Group("Metabrowse server")
  @HelpMessage("Bind to host")
  @Name("H")
  @Tag(tags.experimental)
    host: String = "localhost",
  @Group("Metabrowse server")
  @HelpMessage("Bind to port")
  @Tag(tags.experimental)
  @Name("p")
    port: Int = 4000,

  @Hidden
  @Tag(tags.experimental)
    osArchSuffix: Option[String] = None,
  @Hidden
  @Tag(tags.experimental)
    metabrowseTag: Option[String] = None,
  @Hidden
  @Tag(tags.experimental)
    metabrowseGithubOrgName: Option[String] = None,
  @Hidden
  @Tag(tags.experimental)
    metabrowseExtension: Option[String] = None,
  @Hidden
  @Tag(tags.experimental)
    metabrowseLauncher: Option[String] = None,
  @Hidden
  @Tag(tags.experimental)
    metabrowseDialect: Option[String] = None
) extends HasSharedOptions {
  // format: on
}

object MetabrowseOptions {
  implicit lazy val parser: Parser[MetabrowseOptions] = Parser.derive
  implicit lazy val help: Help[MetabrowseOptions]     = Help.derive
}
