package scala.cli.signing.commands

import caseapp._

import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers._

@HelpMessage("Create PGP key pair")
final case class PgpCreateOptions(
  email: String,
  password: PasswordOption,
  dest: Option[String] = None,
  pubDest: Option[String] = None,
  secretDest: Option[String] = None
) {
  def publicKeyPath: os.Path = {
    val str = pubDest.filter(_.trim.nonEmpty)
      .orElse(secretDest.filter(_.trim.nonEmpty).map(_.stripSuffix(".skr") + ".pub"))
      .orElse(dest.filter(_.trim.nonEmpty).map(_ + ".pub"))
      .getOrElse("key.pub")
    os.Path(str, os.pwd)
  }
  def secretKeyPath: os.Path = {
    val str = secretDest.filter(_.trim.nonEmpty)
      .orElse(pubDest.filter(_.trim.nonEmpty).map(_.stripSuffix(".pub") + ".skr"))
      .orElse(dest.filter(_.trim.nonEmpty).map(_ + ".skr"))
      .getOrElse("key.skr")
    os.Path(str, os.pwd)
  }
}

object PgpCreateOptions {
  implicit lazy val parser: Parser[PgpCreateOptions] = Parser.derive
  implicit lazy val help: Help[PgpCreateOptions]     = Help.derive
}
