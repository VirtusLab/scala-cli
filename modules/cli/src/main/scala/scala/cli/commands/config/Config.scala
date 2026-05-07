package scala.cli.commands.config

import caseapp.core.RemainingArgs
import caseapp.core.help.HelpFormat

import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException, MalformedCliInputError}
import scala.build.internal.util.WarningMessages
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.build.internals.FeatureType
import scala.build.{Directories, Logger}
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.{ScalaCommand, SpecificationLevel}
import scala.cli.config.*
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils

object Config extends ScalaCommand[ConfigOptions] {
  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.MUST

  override def helpFormat: HelpFormat = super.helpFormat
    .withHiddenGroup(HelpGroup.Java)
    .withPrimaryGroup(HelpGroup.Config)

  override def runCommand(options: ConfigOptions, args: RemainingArgs, logger: Logger): Unit = {
    val directories = Directories.directories

    if (options.dump) {
      val content = os.read.bytes(directories.dbPath)
      System.out.write(content)
    }
    else {
      val db = ConfigDbUtils.configDb.orExit(logger)

      def unrecognizedKey(key: String): Nothing = {
        System.err.println(s"Error: unrecognized key $key")
        sys.exit(1)
      }

      args.all match {
        case Seq() =>
          if (options.createPgpKey) {
            if (options.pgpPassword.isEmpty) {
              logger.error(
                s"--pgp-password not specified, use 'none' to create an unprotected keychain or 'random' to generate a password"
              )
              sys.exit(1)
            }
            val coursierCache = options.coursier.coursierCache(logger)
            val secKeyEntry   = Keys.pgpSecretKey
            val pubKeyEntry   = Keys.pgpPublicKey

            val mail = options.email
              .filter(_.trim.nonEmpty)
              .orElse {
                db.get(Keys.userEmail)
                  .wrapConfigException
                  .orExit(logger)
              }
              .getOrElse {
                logger.error(
                  s"--email ... not specified, and ${Keys.userEmail.fullName} not set (either is required to generate a PGP key)"
                )
                sys.exit(1)
              }

            val passwordOpt = if (options.pgpPassword.contains("none"))
              None
            else if (options.pgpPassword.contains("random"))
              Some(ThrowawayPgpSecret.pgpPassPhrase())
            else
              options.pgpPassword.map(scala.cli.signing.shared.Secret.apply)

            val (pgpPublic, pgpSecret) =
              ThrowawayPgpSecret.pgpSecret(
                mail,
                passwordOpt,
                logger,
                coursierCache,
                options.jvm,
                options.coursier,
                options.scalaSigning.cliOptions()
              ).orExit(logger)

            db.set(secKeyEntry, PasswordOption.Value(pgpSecret.toConfig))
            db.set(pubKeyEntry, PasswordOption.Value(pgpPublic.toConfig))
            db.save(directories.dbPath.toNIO)
              .wrapConfigException
              .orExit(logger)

            logger.message("PGP keychains written to config")
            if (options.pgpPassword.contains("random"))
              passwordOpt.foreach { password =>
                println(
                  s"""Password: ${password.value}
                     |Don't lose it!
                     |""".stripMargin
                )
              }
          }
          else {
            System.err.println("No argument passed")
            sys.exit(1)
          }
        case Seq(name, values @ _*) =>
          Keys.map.get(name) match {
            case None => unrecognizedKey(name)
            case Some(powerEntry)
                if (powerEntry.isRestricted || powerEntry.isExperimental) &&
                !allowRestrictedFeatures =>
              logger.error(WarningMessages.powerConfigKeyUsedInSip(powerEntry))
              sys.exit(1)
            case Some(entry) =>
              if entry.isExperimental && !shouldSuppressExperimentalFeatureWarnings then
                logger.experimentalWarning(entry.fullName, FeatureType.ConfigKey)
              if !shouldSuppressDeprecatedFeatureWarnings then
                entry.deprecationMessage.foreach { msg =>
                  logger.deprecationWarning(entry.fullName, msg, FeatureType.ConfigKey)
                }
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
                        if (options.passwordValue && entry.isPasswordOption)
                          PasswordOption.parse(v) match {
                            case Left(err) =>
                              System.err.println(err)
                              sys.exit(1)
                            case Right(passwordOption) =>
                              val password = passwordOption.getBytes
                              System.out.write(password.value)
                          }
                        else
                          println(v)
                    case None =>
                      logger.debug(s"No value found for $name")
                  }
                }
              else {
                val shouldSave = entry match {
                  case Keys.repositoryCredentials =>
                    if options.unset then
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
                            case None           => notFound()
                            case Some(credList) =>
                              val idx = credList.indexWhere(_.host == host)
                              if idx < 0
                              then notFound()
                              else {
                                val updatedCredList = credList.take(idx) ::: credList.drop(idx + 1)
                                db.set(Keys.repositoryCredentials, updatedCredList)
                                db.save(directories.dbPath.toNIO).wrapConfigException.orExit(logger)
                              }
                          }
                        case _ =>
                          logger.message(
                            s"Usage: $progName config --remove ${Keys.repositoryCredentials.fullName} host"
                          )
                          sys.exit(1)
                      }
                      true
                    else {
                      val (host, userOpt, passwordOpt, realmOpt) =
                        parseHostUserPasswordRealm(
                          values,
                          Keys.repositoryCredentials.fullName,
                          logger
                        )
                      val credentials =
                        if options.passwordValue then
                          RepositoryCredentials(
                            host,
                            userOpt.map(user => PasswordOption.Value(user.get())),
                            passwordOpt.map(password => PasswordOption.Value(password.get())),
                            realm = realmOpt,
                            optional = options.optional,
                            matchHost = options.matchHost.orElse(Some(true)),
                            httpsOnly = options.httpsOnly,
                            passOnRedirect = options.passOnRedirect
                          )
                        else
                          RepositoryCredentials(
                            host,
                            userOpt,
                            passwordOpt,
                            realm = realmOpt,
                            optional = options.optional,
                            matchHost = options.matchHost.orElse(Some(true)),
                            httpsOnly = options.httpsOnly,
                            passOnRedirect = options.passOnRedirect
                          )
                      addCredentialEntry(Keys.repositoryCredentials, credentials, db, logger)
                    }

                  case Keys.publishCredentials =>
                    val (host, userOpt, passwordOpt, realmOpt) =
                      parseHostUserPasswordRealm(values, Keys.publishCredentials.fullName, logger)
                    val credentials =
                      if options.passwordValue then
                        PublishCredentials(
                          host,
                          userOpt.map(user => PasswordOption.Value(user.get())),
                          passwordOpt.map(password => PasswordOption.Value(password.get())),
                          realm = realmOpt
                        )
                      else
                        PublishCredentials(host, userOpt, passwordOpt, realm = realmOpt)
                    addCredentialEntry(Keys.publishCredentials, credentials, db, logger)

                  case _ =>
                    val finalValues =
                      if options.passwordValue && entry.isPasswordOption then
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

                    checkIfAskForUpdate(entry, finalValues, db, options)

                    db.setFromString(entry, finalValues)
                      .wrapConfigException
                      .orExit(logger)
                    true
                }

                if shouldSave then
                  db.save(directories.dbPath.toNIO)
                    .wrapConfigException
                    .orExit(logger)
                else logger.message(s"Skipped saving config for key ${entry.fullName}")
              }
          }
      }
    }

    logger.flushExperimentalWarnings
    logger.flushDeprecationWarnings
  }

  private def parseConfigSecret(input: String): Either[BuildException, Option[PasswordOption]] =
    if input.trim.isEmpty then Right(None)
    else
      PasswordOption.parse(input)
        .left.map(err =>
          new MalformedCliInputError(s"Malformed secret value '$input': $err")
        )
        .map(Some(_))

  private def parseHostUserPasswordRealm(
    values: Seq[String],
    keyFullName: String,
    logger: Logger
  ): (String, Option[PasswordOption], Option[PasswordOption], Option[String]) =
    val (host, rawUser, rawPassword, realmOpt) = values match
      case Seq(host, rawUser, rawPassword)        => (host, rawUser, rawPassword, None)
      case Seq(host, rawUser, rawPassword, realm) =>
        (host, rawUser, rawPassword, Some(realm))
      case _ =>
        logger.error(s"Usage: $progName config $keyFullName host user password [realm]")
        logger.error(
          "Note that user and password are assumed to be secrets, specified like value:... or env:ENV_VAR_NAME, see https://scala-cli.virtuslab.org/docs/reference/password-options for more details"
        )
        sys.exit(1)
    val (userOpt, passwordOpt) =
      (parseConfigSecret(rawUser), parseConfigSecret(rawPassword))
        .traverseN
        .left.map(CompositeBuildException(_))
        .orExit(logger)
    (host, userOpt, passwordOpt, realmOpt)

  private def addCredentialEntry[T <: CredentialsValue](
    entry: Key.CredentialsEntry[T, ?],
    credentials: T,
    db: ConfigDb,
    logger: Logger
  ): Boolean =
    val previousValueOpt = db.get(entry).wrapConfigException.orExit(logger)
    if previousValueOpt.exists(_.contains(credentials)) then
      logger.message(
        s"$warnPrefix: an identical ${entry.fullName} entry for host ${credentials.host} is already configured, a duplicate won't be saved."
      )
      false
    else
      db.set(entry, credentials :: previousValueOpt.getOrElse(Nil))
      true

  /** Check whether to ask for an update depending on the provided key.
    */
  private def checkIfAskForUpdate(
    entry: Key[?],
    newValues: Seq[String],
    db: ConfigDb,
    options: ConfigOptions
  ): Unit = entry match {
    case listEntry: Key.StringListEntry =>
      val previousValue = db.get(listEntry).wrapConfigException.orExit(logger).getOrElse(Nil)

      confirmUpdateValue(
        listEntry.fullName,
        previousValue,
        newValues,
        options
      ).wrapConfigException.orExit(logger)
    case _ => ()
  }

  /** If the new value is different from the previous value, ask user for confirmation or suggest to
    * use --force option. If the new value is the same as the previous value, confirm the operation.
    * If force option is provided, skip the confirmation.
    */
  private def confirmUpdateValue(
    keyFullName: String,
    previousValues: Seq[String],
    newValues: Seq[String],
    options: ConfigOptions
  ): Either[Exception, Unit] =
    val (newValuesStr, previousValueStr) = (newValues.mkString(", "), previousValues.mkString(", "))
    val shouldUpdate = !options.force && newValuesStr != previousValueStr && previousValues.nonEmpty

    if shouldUpdate then
      val interactive = options.global.logging.verbosityOptions.interactiveInstance()
      val msg         =
        s"Do you want to change the key '$keyFullName' from '$previousValueStr' to '$newValuesStr'?"
      interactive.confirmOperation(msg) match {
        case Some(true) => Right(())
        case _          => Left(new Exception(
            s"Unable to change the value for the key: '$keyFullName' from '$previousValueStr' to '$newValuesStr' without the force flag. Please pass -f or --force to override."
          ))
      }
    else Right(())
}
