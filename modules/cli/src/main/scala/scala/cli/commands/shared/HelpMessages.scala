package scala.cli.commands.shared

import scala.cli.ScalaCli

object HelpMessages {
  lazy val PowerString: String = if ScalaCli.allowRestrictedFeatures then "" else "--power "
  val passwordOption           = "A github token used to access GitHub. Not needed in most cases."
  private val docsWebsiteUrl   = "https://scala-cli.virtuslab.org"
  val docsWebsiteReference =
    s"Detailed documentation can be found on our website: $docsWebsiteUrl"
  def commandDocWebsiteReference(websiteSuffix: String): String =
    s"For detailed documentation refer to our website: $docsWebsiteUrl/docs/commands/$websiteSuffix"
  val installationDocsWebsiteReference =
    s"For detailed installation instructions refer to our website: $docsWebsiteUrl/install"
  val acceptedInputs: String =
    """Multiple inputs can be passed at once.
      |Paths to directories, URLs and supported file types are accepted as inputs.
      |Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
      |For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
      |All supported types of inputs can be mixed with each other.""".stripMargin
  lazy val bloopInfo: String =
    s"""Bloop is the build server used by ${ScalaCli.fullRunnerName}.
       |For more information about Bloop, refer to https://scalacenter.github.io/bloop/""".stripMargin
  def commandConfigurations(cmdName: String): String =
    s"""Specific $cmdName configurations can be specified with both command line options and using directives defined in sources.
       |Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
       |Using directives can be defined in all supported input source file types.""".stripMargin
}
