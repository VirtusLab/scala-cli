package scala.cli.publish

import coursier.publish.Content
import coursier.publish.signing.Signer

import java.nio.file.Path

import scala.build.Logger
import scala.cli.signing.shared.PasswordOption
import scala.util.Properties

final case class BouncycastleExternalSigner(
  secretKey: os.Path,
  passwordOpt: Option[PasswordOption],
  launcher: os.Path,
  logger: Logger
) extends Signer {

  private def withFileContent[T](content: Content)(f: os.Path => T): T =
    content match {
      case file: Content.File => f(os.Path(file.path, os.pwd))
      case m: Content.InMemory =>
        val permsOpt =
          if (Properties.isWin) None
          else Some("rw-------": os.PermSet)
        val tmpFile = os.temp(m.content0, perms = permsOpt.orNull)
        try f(tmpFile)
        finally os.remove(tmpFile)
    }

  def sign(content: Content): Either[String, String] =
    withFileContent(content) { path =>
      val passwordArgs = passwordOpt.toSeq.flatMap(p => Seq("--password", p.asString.value))
      val proc =
        os.proc(launcher, "pgp", "sign", passwordArgs, "--secret-key", secretKey, "--stdout", path)
      logger.debug(s"Running command ${proc.command.flatMap(_.value)}")
      val res    = proc.call(stdin = os.Inherit, check = false)
      val output = res.out.text().trim
      if (res.exitCode == 0) Right(output)
      else Left(output)
    }
}

object BouncycastleExternalSigner {

  def apply(
    secretKey: Path,
    passwordOrNull: PasswordOption,
    launcher: Path,
    logger: Logger
  ): BouncycastleExternalSigner =
    BouncycastleExternalSigner(
      os.Path(secretKey, os.pwd),
      Option(passwordOrNull),
      os.Path(launcher, os.pwd),
      logger
    )

}
