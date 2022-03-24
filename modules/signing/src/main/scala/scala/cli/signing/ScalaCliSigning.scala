package scala.cli.signing

import caseapp._
import caseapp.core.app.CommandsEntryPoint
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.Security

import scala.cli.signing.commands.{PgpCreate, PgpSign, PgpVerify}
import scala.util.Properties

object ScalaCliSigning extends CommandsEntryPoint {

  def commands = Seq(
    PgpCreate,
    PgpSign,
    PgpVerify
  )

  def progName = "scala-cli-signing"
  override def description =
    "scala-cli-signing is a command-line tool to create PGP keys, and use them to sign files and verify signatures."

  override def enableCompleteCommand    = true
  override def enableCompletionsCommand = true

  override def main(args: Array[String]): Unit = {

    Security.addProvider(new BouncyCastleProvider)

    if (Properties.isWin && System.console() != null && coursier.paths.Util.useJni())
      coursier.jniutils.WindowsAnsiTerminal.enableAnsiOutput()

    super.main(args)
  }

}
