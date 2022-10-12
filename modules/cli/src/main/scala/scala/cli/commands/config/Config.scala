package scala.cli.commands.config

import caseapp.core.RemainingArgs
import coursier.cache.ArchiveCache

import java.util.Base64

import scala.build.Logger
import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException, MalformedCliInputError}
import scala.cli.commands.ScalaCommand
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.util.CommonOps.*
import scala.cli.commands.util.JvmUtils
import scala.cli.config.{
  ConfigDb,
  Keys,
  PasswordOption,
  PublishCredentials,
  RepositoryCredentials,
  Secret
}

object Config extends ScalaCommand[ConfigOptions] {
  override def hidden                  = true
  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED

  override def runCommand(options: ConfigOptions, args: RemainingArgs, logger: Logger): Unit = {
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

            val mail = options.email
              .filter(_.trim.nonEmpty)
              .orElse {
                db.get(Keys.userEmail)
                  .wrapConfigException
                  .orExit(logger)
              }
              .getOrElse {
                System.err.println(
                  s"Error: --email ... not specified, and ${Keys.userEmail.fullName} not set (either is required to generate a PGP key)"
                )
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
                  JvmUtils.javaOptions(options.jvm).orExit(logger).javaHome(
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
              .wrapConfigException
              .orExit(logger)
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
                    .wrapConfigException
                    .orExit(logger)
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
                def parseSecret(input: String): Either[BuildException, Option[PasswordOption]] =
                  if (input.trim.isEmpty) Right(None)
                  else
                    PasswordOption.parse(input)
                      .left.map(err =>
                        new MalformedCliInputError(s"Malformed secret value '$input': $err")
                      )
                      .map(Some(_))
                entry match {
                  case Keys.repositoryCredentials =>
                    if (options.unset)
                      values match {
                        case Seq(host) =>
                          val valueOpt = db.get(Keys.repositoryCredentials)
                            .wrapConfigException
                            .orExit(logger)
                          def notFound(): Unit =
                            logger.message(
                              s"No ${Keys.repositoryCredentials.fullName} found for host $host"
                            )
                          valueOpt match {
                            case None => notFound()
                            case Some(credList) =>
                              val idx = credList.indexWhere(_.host == host)
                              if (idx < 0) notFound()
                              else {
                                val updatedCredList = credList.take(idx) ::: credList.drop(idx + 1)
                                db.set(Keys.repositoryCredentials, updatedCredList)
                                db.save(directories.dbPath.toNIO).wrapConfigException.orExit(logger)
                              }
                          }
                        case _ =>
                          System.err.println(
                            s"Usage: $progName config --remove ${Keys.repositoryCredentials.fullName} host"
                          )
                          sys.exit(1)
                      }
                    else {
                      val (host, rawUser, rawPassword, realmOpt) = values match {
                        case Seq(host, rawUser, rawPassword) => (host, rawUser, rawPassword, None)
                        case Seq(host, rawUser, rawPassword, realm) =>
                          (host, rawUser, rawPassword, Some(realm))
                        case _ =>
                          System.err.println(
                            s"Usage: $progName config ${Keys.repositoryCredentials.fullName} host user password [realm]"
                          )
                          System.err.println(
                            "Note that user and password are assumed to be secrets, specified like value:... or env:ENV_VAR_NAME, see https://scala-cli.virtuslab.org/docs/reference/password-options for more details"
                          )
                          sys.exit(1)
                      }
                      val (userOpt, passwordOpt) = (parseSecret(rawUser), parseSecret(rawPassword))
                        .traverseN
                        .left.map(CompositeBuildException(_))
                        .orExit(logger)
                      val credentials = RepositoryCredentials(
                        host,
                        userOpt,
                        passwordOpt,
                        realm = realmOpt,
                        optional = options.optional,
                        matchHost = options.matchHost.orElse(Some(true)),
                        httpsOnly = options.httpsOnly,
                        passOnRedirect = options.passOnRedirect
                      )
                      val previousValueOpt =
                        db.get(Keys.repositoryCredentials).wrapConfigException.orExit(logger)
                      val newValue = credentials :: previousValueOpt.getOrElse(Nil)
                      db.set(Keys.repositoryCredentials, newValue)
                    }

                  case Keys.publishCredentials =>
                    val (host, rawUser, rawPassword, realmOpt) = values match {
                      case Seq(host, rawUser, rawPassword) => (host, rawUser, rawPassword, None)
                      case Seq(host, rawUser, rawPassword, realm) =>
                        (host, rawUser, rawPassword, Some(realm))
                      case _ =>
                        System.err.println(
                          s"Usage: $progName config ${Keys.publishCredentials.fullName} host user password [realm]"
                        )
                        System.err.println(
                          "Note that user and password are assumed to be secrets, specified like value:... or env:ENV_VAR_NAME, see https://scala-cli.virtuslab.org/docs/reference/password-options for more details"
                        )
                        sys.exit(1)
                    }
                    val (userOpt, passwordOpt) = (parseSecret(rawUser), parseSecret(rawPassword))
                      .traverseN
                      .left.map(CompositeBuildException(_))
                      .orExit(logger)
                    val credentials =
                      PublishCredentials(host, userOpt, passwordOpt, realm = realmOpt)
                    val previousValueOpt =
                      db.get(Keys.publishCredentials).wrapConfigException.orExit(logger)
                    val newValue = credentials :: previousValueOpt.getOrElse(Nil)
                    db.set(Keys.publishCredentials, newValue)

                  case _ =>
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
                }

                db.save(directories.dbPath.toNIO)
                  .wrapConfigException
                  .orExit(logger)
              }
          }
      }
    }
  }
}
