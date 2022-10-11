package scala.cli.commands.pgp

import caseapp.core.RemainingArgs
import coursier.cache.ArchiveCache

import scala.cli.commands.ScalaCommand
import scala.cli.commands.pgp.{KeyServer, PgpProxyMaker}
import scala.cli.commands.util.CommonOps.*
import scala.cli.commands.util.{JvmUtils, ScalaCliSttpBackend}
import scala.cli.internal.PgpProxyMakerSubst

object PgpPush extends ScalaCommand[PgpPushOptions] {

  override def hidden       = true
  override def isRestricted = true
  override def names = List(
    List("pgp", "push")
  )

  override def runCommand(options: PgpPushOptions, args: RemainingArgs): Unit = {

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

    lazy val coursierCache = options.coursier.coursierCache(logger.coursierLogger(""))

    for (key <- all) {
      val path = os.Path(key, os.pwd)
      if (!os.exists(path)) {
        System.err.println(s"Error: $key not found")
        sys.exit(1)
      }
      val keyContent = os.read(path)

      val javaCommand = () =>
        JvmUtils.javaOptions(options.jvm).orExit(logger).javaHome(
          ArchiveCache().withCache(coursierCache),
          coursierCache,
          logger.verbosity
        ).value.javaCommand

      val keyId =
        if (options.forceSigningBinary)
          (new PgpProxyMaker).get()
            .keyId(keyContent, key, coursierCache, logger, javaCommand)
            .orExit(logger)
        else
          (new PgpProxyMakerSubst).get()
            .keyId(keyContent, key, coursierCache, logger, javaCommand)
            .orExit(logger)

      if (keyId.isEmpty)
        if (options.force) {
          if (logger.verbosity >= 0)
            System.err.println(
              s"Warning: $key doesn't look like a PGP public key, proceeding anyway."
            )
        }
        else {
          System.err.println(
            s"Error: $key doesn't look like a PGP public key. " +
              "Use --force to force uploading it anyway."
          )
          sys.exit(1)
        }

      val res = KeyServer.add(
        keyContent,
        keyServerUri,
        backend
      )

      res match {
        case Left(error) =>
          System.err.println(s"Error uploading key to $keyServerUri.")
          if (logger.verbosity >= 0)
            System.err.println(s"Server response: $error")
          sys.exit(1)
        case Right(_) =>
          val name =
            if (keyId.isEmpty) key
            else "0x" + keyId.stripPrefix("0x")
          logger.message(s"Key $name uploaded to $keyServerUri")
      }
    }
  }
}
