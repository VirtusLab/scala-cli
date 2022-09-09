package scala.cli.publish

import coursier.publish.Content
import coursier.publish.signing.Signer

import scala.build.Logger
import scala.cli.signing.shared.PasswordOption
import scala.util.Properties

final case class BouncycastleExternalSigner(
  secretKey: PasswordOption,
  passwordOpt: Option[PasswordOption],
  command: Seq[String],
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
        os.proc(
          command,
          "pgp",
          "sign",
          passwordArgs,
          "--secret-key",
          secretKey.asString.value,
          "--stdout",
          path
        )
      logger.debug(s"Running command ${proc.command.flatMap(_.value)}")
      val res    = proc.call(stdin = os.Inherit, check = false)
      val output = res.out.trim()
      if (res.exitCode == 0) Right(output)
      else Left(output)
    }
}

object BouncycastleExternalSigner {

  def apply(
    secretKey: PasswordOption,
    passwordOrNull: PasswordOption,
    command: Array[String],
    logger: Logger
  ): BouncycastleExternalSigner =
    BouncycastleExternalSigner(
      secretKey,
      Option(passwordOrNull),
      command.toSeq,
      logger
    )

}
