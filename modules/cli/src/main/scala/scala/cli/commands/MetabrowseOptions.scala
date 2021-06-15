package scala.cli.commands

import caseapp._

import scala.build.Build

@HelpMessage("Browse Scala code and its dependencies in the browser")
final case class MetabrowseOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @HelpMessage("Download and add rt.jar in the class path")
    addRtJar: Option[Boolean] = None,

  @Group("Metabrowse server")
  @HelpMessage("Bind to host")
  @Name("H")
    host: String = "localhost",
  @Group("Metabrowse server")
  @HelpMessage("Bind to port")
  @Name("p")
    port: Int = 4000
) {

  def buildOptions: Build.Options =
    shared.buildOptions(jmhOptions = None, jmhVersion = None)
      .copy(fetchSources = Some(true))
}
