package scala.cli.signing.commands

import caseapp.core.RemainingArgs
import caseapp.core.app.Command

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets

import scala.cli.signing.util.{BouncycastleSigner, Util}

object PgpSign extends Command[PgpSignOptions] {

  override def names = List(
    List("pgp", "sign")
  )

  def run(options: PgpSignOptions, args: RemainingArgs): Unit = {

    val privateKey = BouncycastleSigner.readSecretKey {
      new ByteArrayInputStream(Util.maybeDecodeBase64(options.secretKey.getBytes().value))
    }
    val signer = BouncycastleSigner(privateKey, options.password.get())

    val allArgs = args.all

    if (options.stdout && allArgs.length > 1) {
      System.err.println(s"--stdout cannot be specified with multiple input files.")
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
          System.err.println(err)
          sys.exit(1)
        case Right(value) =>
          dest match {
            case Right(destPath) =>
              if (options.force)
                os.write.over(destPath, value)
              else if (os.exists(destPath)) {
                System.err.println(
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
