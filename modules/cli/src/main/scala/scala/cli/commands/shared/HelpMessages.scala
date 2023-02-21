package scala.cli.commands.shared

import scala.cli.ScalaCli

object HelpMessages {
  lazy val PowerString: String = if ScalaCli.allowRestrictedFeatures then "" else "--power "
  val passwordOption           = "A github token used to access GitHub. Not needed in most cases."
  private val docsWebsiteUrl   = "https://scala-cli.virtuslab.org"
  def shortHelpMessage(
    cmdName: String,
    helpHeader: String,
    includeFullHelpReference: Boolean = true,
    needsPower: Boolean = false
  ): String = {
    val maybeFullHelpReference =
      if includeFullHelpReference then
        s"""
           |${HelpMessages.commandFullHelpReference(cmdName, needsPower)}""".stripMargin
      else ""
    s"""$helpHeader
       |$maybeFullHelpReference
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
  }

  val docsWebsiteReference =
    s"Detailed documentation can be found on our website: $docsWebsiteUrl"
  def commandFullHelpReference(commandName: String, needsPower: Boolean = false): String = {
    val maybePowerString = if needsPower then "--power " else ""
    s"""You are currently viewing the basic help for the $commandName sub-command. You can view the full help by running: 
       |   ${ScalaCli.progName} $maybePowerString$commandName --help-full""".stripMargin
  }

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
  lazy val restrictedCommandUsedInSip: String =
    s"""This command is restricted and requires setting the `--power` option to be used.
       |You can pass it explicitly or set it globally by running:
       |   ${Console.BOLD}${ScalaCli.progName} config power true${Console.RESET}""".stripMargin
}
