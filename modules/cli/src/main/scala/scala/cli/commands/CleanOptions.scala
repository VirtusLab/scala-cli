package scala.cli.commands

import caseapp._

@HelpMessage("Clean-up workspace")
final case class CleanOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions()
)
