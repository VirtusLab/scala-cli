package scala.cli.commands.shared

object HelpMessages {
  val passwordOption = "A github token used to access GitHub. Not needed in most cases."
  val docsWebsiteReference =
    "Detailed documentation can be found on our website: https://scala-cli.virtuslab.org"
  val acceptedInputs: String =
    """Multiple inputs can be passed at once.
      |Paths to directories, URLs and supported file types are accepted as inputs.
      |Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
      |For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
      |All supported types of inputs can be mixed with each other.""".stripMargin
  def commandConfigurations(cmdName: String): String =
    s"""Specific $cmdName configurations can be specified with both command line options and using directives defined in sources.
       |Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
       |Using directives can be defined in all supported input source file types.""".stripMargin
}
