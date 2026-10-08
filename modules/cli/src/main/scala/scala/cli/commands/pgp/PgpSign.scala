package scala.cli.commands.pgp

import caseapp.core.RemainingArgs

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets

import scala.build.Logger
import scala.cli.commands.ScalaCommand
import scala.cli.signing.util.{BouncycastleSetup, BouncycastleSigner, Util}

object PgpSign extends ScalaCommand[PgpSignOptions] {

  override def hidden                  = true
  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def names                   = List(
    List("pgp", "sign")
  )

  override def runCommand(options: PgpSignOptions, args: RemainingArgs, logger: Logger): Unit = {
    BouncycastleSetup.ensureProviderRegistered()

    // This key is potentially private (not secret) - may have no password
    val secretKey = BouncycastleSigner.readSecretKey {
      new ByteArrayInputStream(Util.maybeDecodeBase64(options.secretKey.getBytes().value))
    }
    val signer = BouncycastleSigner(secretKey, options.password.map(_.get()))

    val allArgs = args.all

    if (options.stdout && allArgs.length > 1) {
      logger.error("--stdout cannot be specified with multiple input files.")
      sys.exit(1)
    }

    for (arg <- args.all) {
      val path = os.Path(arg, os.pwd)
      val dest =
        if (options.stdout) Left(System.out)
        else Right(path / os.up / s"${path.last}.asc")

      val res = signer.sign { f =>
        var is: InputStream = null
        try {
          is = os.read.inputStream(path)
          val b    = Array.ofDim[Byte](16 * 1024)
          var read = 0
          while ({
            read = is.read(b)
            read >= 0
          })
            if (read > 0)
              f(b, 0, read)
        }
        finally is.close()
      }

      res match {
        case Left(err) =>
          logger.error(err)
          sys.exit(1)
        case Right(value) =>
          dest match {
            case Right(destPath) =>
              if (options.force)
                os.write.over(destPath, value)
              else if (os.exists(destPath)) {
                logger.error(
                  s"Error: ${arg + ".asc"} already exists. Pass --force to force overwriting it."
                )
                sys.exit(1)
              }
              else
                os.write(destPath, value)
            case Left(outputStream) =>
              outputStream.write(value.getBytes(StandardCharsets.UTF_8))
          }
      }
    }
  }
}
