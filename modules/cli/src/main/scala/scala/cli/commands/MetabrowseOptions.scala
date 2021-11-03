package scala.cli.commands

import caseapp._

import scala.build.options.BuildOptions
import scala.cli.internal.FetchExternalBinary
import scala.util.Properties

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
) {
  // format: on

  def metabrowseBinaryUrl(scalaVersion: String): (String, Boolean) = {
    val osArchSuffix0 = osArchSuffix.map(_.trim).filter(_.nonEmpty)
      .getOrElse(FetchExternalBinary.platformSuffix(supportsMusl = false))
    val metabrowseTag0           = metabrowseTag.getOrElse("latest")
    val metabrowseGithubOrgName0 = metabrowseGithubOrgName.getOrElse("alexarchambault/metabrowse")
    val metabrowseExtension0     = if (Properties.isWin) ".zip" else ".gz"
    val url =
      s"https://github.com/$metabrowseGithubOrgName0/releases/download/$metabrowseTag0/metabrowse-$scalaVersion-$osArchSuffix0$metabrowseExtension0"
    (url, !metabrowseTag0.startsWith("v"))
  }

  def buildOptions: BuildOptions = {
    val baseOptions = shared.buildOptions(enableJmh = false, jmhVersion = None)
    baseOptions.copy(
      classPathOptions = baseOptions.classPathOptions.copy(
        fetchSources = Some(true)
      ),
      javaOptions = baseOptions.javaOptions.copy(
        jvmIdOpt = baseOptions.javaOptions.jvmIdOpt.orElse(Some("8"))
      )
    )
  }
}

object MetabrowseOptions {
  implicit lazy val parser: Parser[MetabrowseOptions] = Parser.derive
  implicit lazy val help: Help[MetabrowseOptions]     = Help.derive
}
