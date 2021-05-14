package scala.cli.commands

import caseapp._

final case class CleanOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions()
)
