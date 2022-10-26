package scala.cli.commands

import caseapp.*

import scala.cli.commands.common.HasSharedOptions

// format: off
@HelpMessage("Browse Scala code and its dependencies in the browser")
final case class MetabrowseOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @HelpMessage("Download and add `rt.jar` in the class path")
    addRtJar: Option[Boolean] = None,

  @Group("Metabrowse server")
  @HelpMessage("Bind to host")
  @Name("H")
    host: String = "localhost",
  @Group("Metabrowse server")
  @HelpMessage("Bind to port")
  @Name("p")
    port: Int = 4000,

  @Hidden
    osArchSuffix: Option[String] = None,
  @Hidden
    metabrowseTag: Option[String] = None,
  @Hidden
    metabrowseGithubOrgName: Option[String] = None,
  @Hidden
    metabrowseExtension: Option[String] = None,
  @Hidden
    metabrowseLauncher: Option[String] = None,
  @Hidden
    metabrowseDialect: Option[String] = None
) extends HasSharedOptions {
  // format: on
}

object MetabrowseOptions {
  implicit lazy val parser: Parser[MetabrowseOptions] = Parser.derive
  implicit lazy val help: Help[MetabrowseOptions]     = Help.derive
}
