package scala.cli.commands.config

import caseapp.core.RemainingArgs
import coursier.cache.ArchiveCache

import java.util.Base64

import scala.cli.commands.ScalaCommand
import scala.cli.commands.publish.ConfigUtil._
import scala.cli.commands.util.CommonOps._
import scala.cli.commands.util.JvmUtils
import scala.cli.config.{ConfigDb, Keys, PasswordOption, Secret}

object Config extends ScalaCommand[ConfigOptions] {
  override def hidden       = true
  override def isRestricted = true

  def run(options: ConfigOptions, args: RemainingArgs): Unit = {

    val logger      = options.logging.logger
    val directories = options.directories.directories

    if (options.dump) {
      val content = os.read.bytes(directories.dbPath)
      System.out.write(content)
    }
    else {
      val db = ConfigDb.open(directories.dbPath.toNIO)
        .wrapConfigException
        .orExit(logger)

      def unrecognizedKey(key: String): Nothing = {
        System.err.println(s"Error: unrecognized key $key")
        sys.exit(1)
      }

      args.all match {
        case Seq() =>
          if (options.createPgpKey) {
            val coursierCache       = options.coursier.coursierCache(logger.coursierLogger(""))
            val secKeyEntry         = Keys.pgpSecretKey
            val secKeyPasswordEntry = Keys.pgpSecretKeyPassword
            val pubKeyEntry         = Keys.pgpPublicKey

            val mail = db.get(Keys.userEmail)
              .wrapConfigException
              .orExit(logger)
              .getOrElse {
                System.err.println("Error: user.email not set (required to generate PGP key)")
                sys.exit(1)
              }

            val password = ThrowawayPgpSecret.pgpPassPhrase()
            val (pgpPublic, pgpSecret0) =
              ThrowawayPgpSecret.pgpSecret(
                mail,
                password,
                logger,
                coursierCache,
                () =>
                  JvmUtils.javaOptions(options.jvm).javaHome(
                    ArchiveCache().withCache(coursierCache),
                    coursierCache,
                    logger.verbosity
                  ).value.javaCommand
              ).orExit(logger)
            val pgpSecretBase64 = pgpSecret0.map(Base64.getEncoder.encodeToString)

            db.set(secKeyEntry, PasswordOption.Value(pgpSecretBase64.toConfig))
            db.set(secKeyPasswordEntry, PasswordOption.Value(password.toConfig))
            db.set(pubKeyEntry, PasswordOption.Value(pgpPublic.toConfig))
            db.save(directories.dbPath.toNIO)
          }
          else {
            System.err.println("No argument passed")
            sys.exit(1)
          }
        case Seq(name, values @ _*) =>
          Keys.map.get(name) match {
            case None => unrecognizedKey(name)
            case Some(entry) =>
              if (values.isEmpty)
                if (options.unset) {
                  db.remove(entry)
                  db.save(directories.dbPath.toNIO)
                }
                else {
                  val valueOpt = db.getAsString(entry)
                    .wrapConfigException
                    .orExit(logger)
                  valueOpt match {
                    case Some(value) =>
                      for (v <- value)
                        if (options.password && entry.isPasswordOption)
                          PasswordOption.parse(v) match {
                            case Left(err) =>
                              System.err.println(err)
                              sys.exit(1)
                            case Right(passwordOption) =>
                              val password = passwordOption.getBytes()
                              System.out.write(password.value)
                          }
                        else
                          println(v)
                    case None =>
                      logger.debug(s"No value found for $name")
                  }
                }
              else {
                val finalValues =
                  if (options.passwordValue && entry.isPasswordOption)
                    values.map { input =>
                      PasswordOption.parse(input) match {
                        case Left(err) =>
                          System.err.println(err)
                          sys.exit(1)
                        case Right(passwordOption) =>
                          PasswordOption.Value(passwordOption.get()).asString.value
                      }
                    }
                  else
                    values

                db.setFromString(entry, finalValues)
                  .wrapConfigException
                  .orExit(logger)
                db.save(directories.dbPath.toNIO)
              }
          }
      }
    }
  }
}
