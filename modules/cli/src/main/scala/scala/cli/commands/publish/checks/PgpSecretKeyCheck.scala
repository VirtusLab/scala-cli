package scala.cli.commands.publish.checks

import coursier.cache.{ArchiveCache, FileCache}
import coursier.util.Task
import sttp.client3.*
import sttp.model.Uri

import java.util.Base64

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException, MalformedCliInputError}
import scala.build.options.publish.ConfigPasswordOption
import scala.build.options.publish.ConfigPasswordOption.*
import scala.build.options.PublishOptions as BPublishOptions
import scala.cli.commands.config.ThrowawayPgpSecret
import scala.cli.commands.pgp.{KeyServer, PgpProxyMaker}
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions, SetSecret}
import scala.cli.commands.util.JvmUtils
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.errors.MissingPublishOptionError
import scala.cli.signing.shared.PasswordOption
import scala.cli.util.ConfigPasswordOptionHelpers.*
import scala.cli.util.MaybeConfigPasswordOption

final case class PgpSecretKeyCheck(
  options: PublishSetupOptions,
  coursierCache: FileCache[Task],
  configDb: () => ConfigDb,
  logger: Logger,
  backend: SttpBackend[Identity, Any]
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Signing
  def fieldName     = "pgp-secret-key"
  def directivePath = "publish" + (if (options.publishParams.setupCi) ".ci" else "") + ".secretKey"

  def check(pubOpt: BPublishOptions): Boolean = {
    val opt0 = pubOpt.retained(options.publishParams.setupCi)

    opt0.repository.orElse(options.publishRepo.publishRepository).contains("github") ||
    (
      opt0.secretKey.isDefined &&
      opt0.secretKeyPassword.isDefined &&
      opt0.publicKey.isDefined &&
      isKeyUploaded(opt0.publicKey.get.get(configDb()).toOption.map(_.toCliSigning))
        .getOrElse(false)
    ) ||
    opt0.gpgSignatureId.isDefined
  }

  private val base64Chars = (('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') ++ Seq('+', '/', '='))
    .map(_.toByte)
    .toSet

  // kind of meh, ideally we should know beforehand whether we are handed base64 or not
  private def maybeEncodeBase64(input: Array[Byte]): String =
    if (input.nonEmpty && input.forall(base64Chars.contains))
      new String(input.map(_.toChar))
    else
      Base64.getEncoder().encodeToString(input)

  def javaCommand: Either[BuildException, () => String] = either {
    () =>
      value(JvmUtils.javaOptions(options.sharedJvm)).javaHome(
        ArchiveCache().withCache(coursierCache),
        coursierCache,
        logger.verbosity
      ).value.javaCommand
  }

  private lazy val keyServers: Either[BuildException, Seq[Uri]] = {
    val rawKeyServers = options.sharedPgp.keyServer.filter(_.trim.nonEmpty)
    if (rawKeyServers.filter(_.trim.nonEmpty).isEmpty)
      Right(KeyServer.allDefaults)
    else
      rawKeyServers
        .map { keyServerUriStr =>
          Uri.parse(keyServerUriStr).left.map { err =>
            new MalformedCliInputError(
              s"Malformed key server URI '$keyServerUriStr': $err"
            )
          }
        }
        .sequence
        .left.map(CompositeBuildException(_))
  }

  private def isKeyUploaded(pubKeyOpt: Option[PasswordOption]): Either[BuildException, Boolean] =
    either {
      pubKeyOpt match {
        case Some(pubKey) =>
          val keyId = value {
            (new PgpProxyMaker).get().keyId(
              pubKey.get().value,
              "[generated key]",
              coursierCache,
              logger,
              value(javaCommand),
              options.scalaSigning.cliOptions()
            )
          }

          value(keyServers).forall { keyServer =>
            KeyServer.check(keyId, keyServer, backend) match
              case Right(Right(_)) => true
              case Right(Left(msg)) =>
                logger.debug(
                  s"""Response from $keyServer:
                     |$msg
                     |""".stripMargin
                )
                false
              case Left(err) =>
                logger.debug(s"Error checking $keyId at $keyServer: $err")
                false
          }
        case None => false
      }
    }

  private case class PGPKeys(
    secretKeyOpt: Option[ConfigPasswordOption],
    secretKeyPasswordOpt: Option[ConfigPasswordOption],
    publicKeyOpt: Option[ConfigPasswordOption]
  )

  val missingSecretKeyError = new MissingPublishOptionError(
    "publish secret key",
    "--secret-key",
    "publish.secretKey",
    configKeys = Seq(Keys.pgpSecretKey.fullName),
    extraMessage =
      "also specify publish.secretKeyPassword / --secret-key-password if needed." +
        (if (options.publishParams.setupCi)
           " Alternatively, pass --random-secret-key"
         else "")
  )

  private lazy val keysFromOptions: PGPKeys =
    PGPKeys(
      options.publishParams.secretKey.map(_.configPasswordOptions()),
      options.publishParams.secretKeyPassword.map(_.configPasswordOptions()),
      options.publicKey.map(ConfigPasswordOption.ActualOption.apply)
    )

  private lazy val maybeKeysFromConfig: Either[BuildException, PGPKeys] =
    for {
      secretKeyOpt <- configDb().get(Keys.pgpSecretKey).wrapConfigException
      pubKeyOpt    <- configDb().get(Keys.pgpPublicKey).wrapConfigException
      passwordOpt  <- configDb().get(Keys.pgpSecretKeyPassword).wrapConfigException
    } yield PGPKeys(
      secretKeyOpt.map(sk => ConfigPasswordOption.ActualOption(sk.toCliSigning)),
      passwordOpt.map(p => ConfigPasswordOption.ActualOption(p.toCliSigning)),
      pubKeyOpt.map(pk => ConfigPasswordOption.ActualOption(pk.toCliSigning))
    )

  private def getRandomPGPKeys: Either[BuildException, PGPKeys] = either {
    val maybeMail = options.randomSecretKeyMail.toRight(
      new MissingPublishOptionError(
        "the e-mail address to associate to the random key pair",
        "--random-secret-key-mail",
        ""
      )
    )

    val passwordSecret = options.publishParams.secretKeyPassword
      .map(_.configPasswordOptions())
      .map { configPasswordOption =>
        configPasswordOption
          .get(configDb()).wrapConfigException
          .map(_.get().toCliSigning)
          .orThrow
      }
      .getOrElse(ThrowawayPgpSecret.pgpPassPhrase())

    val (pgpPublic, pgpSecret0) = value {
      ThrowawayPgpSecret.pgpSecret(
        value(maybeMail),
        passwordSecret,
        logger,
        coursierCache,
        value(javaCommand),
        options.scalaSigning.cliOptions()
      )
    }

    val pgpSecretBase64 = pgpSecret0.map(Base64.getEncoder.encodeToString)

    PGPKeys(
      Some(ConfigPasswordOption.ActualOption(PasswordOption.Value(pgpSecretBase64))),
      Some(ConfigPasswordOption.ActualOption(PasswordOption.Value(passwordSecret))),
      Some(ConfigPasswordOption.ActualOption(PasswordOption.Value(pgpPublic)))
    )
  }

  private def uploadKey(keyIdOpt: Option[ConfigPasswordOption]): Either[BuildException, Unit] =
    either {
      keyIdOpt match
        case None =>
          logger.message("") // printing an empty line, for readability
          logger.message(
            "Warning: no public key passed, not checking if the key needs to be uploaded to a key server."
          )
        case Some(pubKeyConfigPasswordOption) =>
          val publicKeyString = pubKeyConfigPasswordOption.get(configDb())
            .orThrow
            .get()
            .value

          val keyId = (new PgpProxyMaker).get()
            .keyId(
              publicKeyString,
              "[generated key]",
              coursierCache,
              logger,
              value(javaCommand),
              options.scalaSigning.cliOptions()
            ).orThrow

          value(keyServers)
            .map { keyServer =>
              if (options.dummy) {
                logger.message("") // printing an empty line, for readability
                logger.message(s"Would upload key 0x${keyId.stripPrefix("0x")} to $keyServer")
                Right(())
              }
              else {
                val e: Either[BuildException, Unit] = either {
                  val checkResp = value {
                    KeyServer.check(keyId, keyServer, backend)
                      .left.map(msg =>
                        new PgpSecretKeyCheck.KeyServerError(
                          s"Error getting key $keyId from $keyServer: $msg"
                        )
                      )
                  }
                  logger.debug(s"Key server check response: $checkResp")
                  val check = checkResp.isRight
                  if (!check) {
                    val resp = value {
                      KeyServer.add(publicKeyString, keyServer, backend)
                        .left.map(msg =>
                          new PgpSecretKeyCheck.KeyServerError(
                            s"Error uploading key $keyId to $keyServer: $msg"
                          )
                        )
                    }
                    logger.debug(s"Key server upload response: $resp")
                    logger.message("") // printing an empty line, for readability
                    logger.message(s"Uploaded key 0x${keyId.stripPrefix("0x")} to $keyServer")
                  }
                }
                e
              }
            }
            .sequence
            .left.map(CompositeBuildException(_))
            .map(_ => ())
    }

  def defaultValue(pubOpt: BPublishOptions): Either[BuildException, OptionCheck.DefaultValue] =
    either {
      val retainedOptions = pubOpt.retained(options.publishParams.setupCi)
      val (setupKeys, areConfigDefaults) = if (retainedOptions.secretKey.isDefined) {
        val publicKeySetup = if (retainedOptions.publicKey.isEmpty)
          keysFromOptions.publicKeyOpt
        else None

        val passwordSetup = if (retainedOptions.secretKeyPassword.isEmpty)
          keysFromOptions.secretKeyPasswordOpt
        else None

        (PGPKeys(None, passwordSetup, publicKeySetup), false)
      }
      else {
        val randomSecretKey = options.randomSecretKey.getOrElse(false)

        if (keysFromOptions.secretKeyOpt.isDefined)
          (keysFromOptions, false)
        else if (randomSecretKey && options.publishParams.setupCi)
          (getRandomPGPKeys.orThrow, false)
        else {
          val keysFromConfig = maybeKeysFromConfig.orThrow
          if (keysFromConfig.secretKeyOpt.isDefined)
            logger.message(s"$fieldName:")
            logger.message("  found keys in config")
          else
            throw missingSecretKeyError
          if (keysFromConfig.publicKeyOpt.isEmpty)
            logger.message("  warning: no PGP public key found in config")
          if (keysFromConfig.secretKeyPasswordOpt.isEmpty)
            logger.message("  warning: no PGP secret key password found in config")

          (keysFromConfig, true)
        }
      }

      val publicKeyOpt = retainedOptions.publicKey.orElse(setupKeys.publicKeyOpt)

      if (options.publishParams.setupCi) {
        val (passwordSetSecret, passwordDirectives) = setupKeys.secretKeyPasswordOpt
          .map { p =>
            val dir    = "publish.ci.secretKeyPassword" -> "env:PUBLISH_SECRET_KEY_PASSWORD"
            val secret = p.get(configDb()).orThrow.get()
            val setSec = SetSecret("PUBLISH_SECRET_KEY_PASSWORD", secret, force = true)
            (Seq(setSec), Seq(dir))
          }
          .getOrElse((Nil, Nil))

        val keySetSecrets = setupKeys.secretKeyOpt match
          case Some(configPasswordOption) =>
            val secret = configPasswordOption.get(configDb())
              .orThrow
              .getBytes()

            Seq(SetSecret(
              "PUBLISH_SECRET_KEY",
              secret.map(maybeEncodeBase64),
              force = true
            ))
          case _ => Nil

        val (publicKeySetSecret, publicKeyDirective) = setupKeys.publicKeyOpt
          .map { p =>
            val dir    = "publish.ci.publicKey" -> "env:PUBLISH_PUBLIC_KEY"
            val secret = p.get(configDb()).orThrow.get()
            val setSec = SetSecret("PUBLISH_PUBLIC_KEY", secret, force = true)
            (Seq(setSec), Seq(dir))
          }
          .getOrElse((Nil, Nil))

        val secretsToSet    = keySetSecrets ++ passwordSetSecret ++ publicKeySetSecret
        val extraDirectives = passwordDirectives ++ publicKeyDirective

        OptionCheck.DefaultValue(
          () => uploadKey(publicKeyOpt).map(_ => Some("env:PUBLISH_SECRET_KEY")),
          extraDirectives,
          secretsToSet
        )
      }
      else if (areConfigDefaults)
        OptionCheck.DefaultValue(
          () => uploadKey(publicKeyOpt).map(_ => None),
          Nil,
          Nil
        )
      else {
        def getDirectiveValue(configPasswordOpt: Option[ConfigPasswordOption]): Option[String] =
          configPasswordOpt.collect {
            case ActualOption(passwordOption) =>
              val optionValue = passwordOption.asString.value

              if (optionValue.startsWith("file:")) {
                val path = os.Path(optionValue.stripPrefix("file:"))
                scala.util.Try(path.relativeTo(os.pwd))
                  .map(p => s"file:${p.toString}")
                  .getOrElse(optionValue)

              }
              else optionValue
            case ConfigOption(fullName) => s"config:$fullName"
          }

        val passwordDirectives = getDirectiveValue(setupKeys.secretKeyPasswordOpt).map {
          "publish.secretKeyPassword" -> _
        }.toSeq

        val secretKeyDirValue = getDirectiveValue(setupKeys.secretKeyOpt)

        val publicKeyDirective = getDirectiveValue(setupKeys.publicKeyOpt).map {
          "publish.publicKey" -> _
        }.toSeq

        val extraDirectives = passwordDirectives ++ publicKeyDirective

        if (passwordDirectives.exists(_._2.startsWith("value:")))
          logger.diagnostic(
            "The secret value of PGP private key password will be written to a potentially public file!"
          )

        if (secretKeyDirValue.exists(_.startsWith("value:")))
          logger.diagnostic(
            "The secret value of PGP private key will be written to a potentially public file!"
          )

        OptionCheck.DefaultValue(
          () => uploadKey(publicKeyOpt).map(_ => secretKeyDirValue),
          extraDirectives,
          Nil
        )
      }
    }
}

object PgpSecretKeyCheck {
  final class KeyServerError(message: String) extends BuildException(message)
}
