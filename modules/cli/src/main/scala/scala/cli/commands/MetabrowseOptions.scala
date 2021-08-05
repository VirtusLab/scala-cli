package scala.cli.commands

import caseapp._

import scala.build.options.BuildOptions
import scala.util.Properties

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
    port: Int = 4000,

  @Hidden
    osArchSuffix: Option[String] = None,
  @Hidden
    metabrowseTag: Option[String] = None,
  @Hidden
    metabrowseGitHubOrgName: Option[String] = None,
  @Hidden
    metabrowseExtension: Option[String] = None,
  @Hidden
    metabrowseLauncher: Option[String] = None,
  @Hidden
    metabrowseDialect: Option[String] = None
) {

  def metabrowseBinaryUrl(scalaVersion: String): (String, Boolean) = {
    val osArchSuffix0 = osArchSuffix.map(_.trim).filter(_.nonEmpty)
      .getOrElse(MetabrowseOptions.platformSuffix)
    val metabrowseTag0 = metabrowseTag.getOrElse("latest")
    val metabrowseGitHubOrgName0 = metabrowseGitHubOrgName.getOrElse("alexarchambault/metabrowse")
    val metabrowseExtension0 = if (Properties.isWin) ".zip" else ".gz"
    val url = s"https://github.com/$metabrowseGitHubOrgName0/releases/download/$metabrowseTag0/metabrowse-$scalaVersion-$osArchSuffix0$metabrowseExtension0"
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

  private def platformSuffix: String = {
    val arch = sys.props("os.arch").toLowerCase(java.util.Locale.ROOT) match {
      case "amd64" => "x86_64"
      case other => other
    }
    val os =
      if (Properties.isWin) "pc-win32"
      else if (Properties.isLinux) "pc-linux"
      else if (Properties.isMac) "apple-darwin"
      else sys.error(s"Unrecognized OS: ${sys.props("os.name")}")
    s"$arch-$os"
  }

}
