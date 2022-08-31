package scala.cli.commands.util
import coursier.core.Version

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.FetchExternalBinary
import scala.build.options.BuildOptions
import scala.cli.commands.FmtOptions
import scala.cli.commands.util.SharedOptionsUtil._
import scala.util.Properties

object FmtOptionsUtil {
  implicit class FmtOptionsOps(v: FmtOptions) {
    import v._
    def binaryUrl(version: String): (String, Boolean) = {
      val osArchSuffix0 = osArchSuffix.map(_.trim).filter(_.nonEmpty)
        .getOrElse(FetchExternalBinary.platformSuffix())
      val tag0 = scalafmtTag.getOrElse("v" + version)
      val gitHubOrgName0 = scalafmtGithubOrgName.getOrElse {
        if (Version(version) < Version("3.5.9"))
          "scala-cli/scalafmt-native-image"
        else // from version 3.5.9 scalafmt-native-image repository was moved to VirtusLab organisation
          "virtuslab/scalafmt-native-image"
      }
      val extension0 = if (Properties.isWin) ".zip" else ".gz"
      val url =
        s"https://github.com/$gitHubOrgName0/releases/download/$tag0/scalafmt-$osArchSuffix0$extension0"
      (url, !tag0.startsWith("v"))
    }

    def buildOptions: Either[BuildException, BuildOptions] = shared.buildOptions()

    def scalafmtCliOptions: List[String] =
      scalafmtArg :::
        (if (check && !scalafmtArg.contains("--check")) List("--check") else Nil) :::
        (if (scalafmtHelp && !scalafmtArg.exists(Set("-h", "-help", "--help"))) List("--help")
         else Nil) :::
        (if (respectProjectFilters && !scalafmtArg.contains("--respect-project-filters"))
           List("--respect-project-filters")
         else Nil)
  }
}
