package scala.cli.commands.github

import caseapp._

import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers._
import scala.cli.commands.LoggingOptions
import scala.cli.signing.shared.Secret

// format: off
final case class SharedSecretOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  token: PasswordOption = PasswordOption.Value(Secret("")),
  @ExtraName("repo")
    repository: String = ""
) {
  // format: on

  lazy val (repoOrg, repoName) =
    repository.split('/') match {
      case Array(org, name) => (org, name)
      case _ =>
        sys.error(s"Malformed repository: '$repository' (expected 'org/name')")
    }
}

object SharedSecretOptions {
  lazy val parser: Parser[SharedSecretOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedSecretOptions, parser.D] = parser
  implicit lazy val help: Help[SharedSecretOptions]                      = Help.derive
}
