package scala.cli.commands.pgp

import caseapp.core.RemainingArgs

import scala.cli.commands.ScalaCommand
import scala.cli.commands.pgp.KeyServer
import scala.cli.commands.util.CommonOps._
import scala.cli.commands.util.ScalaCliSttpBackend

object PgpPull extends ScalaCommand[PgpPullOptions] {

  override def hidden       = true
  override def isRestricted = true
  override def names = List(
    List("pgp", "pull")
  )

  def run(options: PgpPullOptions, args: RemainingArgs): Unit = {

    val logger  = options.logging.logger
    val backend = ScalaCliSttpBackend.httpURLConnection(logger)

    val keyServerUri = options.shared.keyServerUriOptOrExit(logger).getOrElse {
      KeyServer.default
    }

    val all = args.all

    if (!options.allowEmpty && all.isEmpty) {
      System.err.println("No key passed as argument.")
      sys.exit(1)
    }

    // val lookupEndpoint = keyServerUri

    for (keyId <- all)
      KeyServer.check(keyId, keyServerUri, backend) match {
        case Left(err) =>
          System.err.println(s"Error checking $keyId: $err")
          sys.exit(1)
        case Right(Right(content)) =>
          println(content)
        case Right(Left(message)) =>
          if (logger.verbosity >= 0)
            System.err.println(s"Key $keyId not found: $message")
          sys.exit(1)
      }
  }
}
