package scala.cli.commands.pgp

import caseapp.*
import sttp.model.Uri

import scala.build.Logger
import scala.cli.commands.shared.LoggingOptions

// format: off
final case class SharedPgpPushPullOptions(
  @Group("PGP")
  @HelpMessage("Key server to push / pull keys from")
  @ValueDescription("URL")
    keyServer: List[String] = Nil
) {
  // format: on

  def keyServerUriOptOrExit(logger: Logger): Option[Uri] =
    keyServer
      .filter(_.trim.nonEmpty)
      .lastOption
      .map { addr =>
        Uri.parse(addr) match {
          case Left(err) =>
            if (logger.verbosity >= 0)
              System.err.println(s"Error parsing key server address '$addr': $err")
            sys.exit(1)
          case Right(uri) => uri
        }
      }
}

object SharedPgpPushPullOptions {
  implicit lazy val parser: Parser[SharedPgpPushPullOptions] = Parser.derive
  implicit lazy val help: Help[SharedPgpPushPullOptions]     = Help.derive
}
