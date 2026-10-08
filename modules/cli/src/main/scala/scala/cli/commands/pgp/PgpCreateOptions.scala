package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared.{GlobalOptions, HasGlobalOptions, HelpGroup}
import scala.cli.commands.tags
import scala.cli.signing.PasswordOption
import scala.cli.signing.util.ArgParsers.*

// format: off
@HelpMessage("Create PGP key pair")
final case class PgpCreateOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("E-mail address to associate to the key pair")
  @Tag(tags.experimental)
  @Tag(tags.inShortHelp)
    email: String,
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Password to protect the secret key with, e.g. value:… / env:… / file:… / command:… (no password if not specified)")
  @ValueDescription("password")
  @Tag(tags.experimental)
    password: Option[PasswordOption] = None,
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Path prefix to write the key pair to (DEST.pub and DEST.skr)")
  @ValueDescription("path")
  @Tag(tags.experimental)
    dest: Option[String] = None,
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Path to write the public key to (key.pub by default)")
  @ValueDescription("path")
  @Tag(tags.experimental)
    pubDest: Option[String] = None,
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Path to write the secret key to (key.skr by default)")
  @ValueDescription("path")
  @Tag(tags.experimental)
    secretDest: Option[String] = None
) extends HasGlobalOptions {
  // format: on
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
