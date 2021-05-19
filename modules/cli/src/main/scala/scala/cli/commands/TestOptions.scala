package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

final case class TestOptions(
  @Recurse
    shared: SharedOptions,
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions()
)

object TestOptions {
  implicit val parser = Parser[TestOptions]
  implicit val help = Help[TestOptions]
}
