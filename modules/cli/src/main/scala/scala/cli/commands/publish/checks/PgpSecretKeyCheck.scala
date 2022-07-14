package scala.cli.commands.publish.checks

import coursier.cache.{ArchiveCache, FileCache}
import coursier.util.Task
import sttp.client3._
import sttp.model.Uri

import java.util.Base64

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, MalformedCliInputError}
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.config.ThrowawayPgpSecret
import scala.cli.commands.pgp.{KeyServer, PgpProxyMaker}
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions, SetSecret}
import scala.cli.commands.util.JvmUtils
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.errors.MissingPublishOptionError
import scala.cli.signing.shared.PasswordOption
import scala.cli.util.MaybeConfigPasswordOptionHelpers._

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
    opt0.secretKey.isDefined
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

  def javaCommand: () => String =
    () =>
      JvmUtils.javaOptions(options.sharedJvm).javaHome(
        ArchiveCache().withCache(coursierCache),
        coursierCache,
        logger.verbosity
      ).value.javaCommand

  def defaultValue(): Either[BuildException, OptionCheck.DefaultValue] =
    either {
      if (options.publishParams.setupCi) {
        val (pubKeyOpt, secretKey, passwordOpt) = options.publishParams.secretKey match {
          case Some(secretKey) =>
            val pubKeyOpt = options.publicKey.map(_.get())
            val passwordOpt =
              value(options.publishParams.secretKeyPassword.map(_.get(configDb())).sequence)
            (pubKeyOpt, Left(secretKey), passwordOpt)
          case None =>
            value(configDb().get(Keys.pgpSecretKey)) match {
              case Some(secretKey) =>
                val pubKeyOpt   = value(configDb().get(Keys.pgpPublicKey))
                val passwordOpt = value(configDb().get(Keys.pgpSecretKeyPassword))
                (
                  pubKeyOpt.map(_.get()),
                  Right(secretKey),
                  passwordOpt
                )
              case None =>
                val randomSecretKey = options.randomSecretKey.getOrElse(false)
                if (randomSecretKey) {
                  val password = {
                    val res = value {
                      options.publishParams
                        .secretKeyPassword
                        .map(_.get(configDb()))
                        .sequence
                    }
                    res
                      .map(_.get())
                      .getOrElse(ThrowawayPgpSecret.pgpPassPhrase())
                  }
                  val mail = value {
                    options.randomSecretKeyMail
                      .toRight(
                        new MissingPublishOptionError(
                          "random secret key mail",
                          "--random-secret-key-mail",
                          ""
                        )
                      )
                  }
                  val (pgpPublic, pgpSecret0) = value {
                    ThrowawayPgpSecret.pgpSecret(
                      mail,
                      password,
                      logger,
                      coursierCache,
                      javaCommand
                    )
                  }
                  val pgpSecretBase64 = pgpSecret0.map(Base64.getEncoder.encodeToString)
                  (
                    Some(pgpPublic),
                    Right(PasswordOption.Value(pgpSecretBase64)),
                    Some(PasswordOption.Value(password))
                  )
                }
                else
                  value {
                    Left(
                      new MissingPublishOptionError(
                        "publish secret key",
                        "--secret-key",
                        "publish.secretKey",
                        configKeys = Seq(Keys.pgpSecretKey.fullName),
                        extraMessage =
                          ", and specify publish.secretKeyPassword / --secret-key-password if needed." +
                            (if (options.publishParams.setupCi)
                               " Alternatively, pass --random-secret-key"
                             else "")
                      )
                    )
                  }
            }
        }
        val pushKey: () => Either[BuildException, Unit] = pubKeyOpt match {
          case Some(pubKey) =>
            val keyId = value {
              (new PgpProxyMaker).get().keyId(
                pubKey.value,
                "[generated key]",
                coursierCache,
                logger,
                javaCommand
              )
            }
            val keyServers = value {
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
            () =>
              keyServers
                .map { keyServer =>
                  if (options.dummy) Right(())
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
                          KeyServer.add(pubKey.value, keyServer, backend)
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
          case None =>
            logger.message(
              "Warning: no public key passed, not checking if the key needs to be uploaded to a key server."
            )
            () => Right(())
        }
        val (passwordSetSecret, extraDirectives) = passwordOpt
          .map { p =>
            if (options.publishParams.setupCi) {
              val dir    = "publish.ci.secretKeyPassword" -> "env:PUBLISH_SECRET_KEY_PASSWORD"
              val setSec = SetSecret("PUBLISH_SECRET_KEY_PASSWORD", p.get(), force = true)
              (Seq(setSec), Seq(dir))
            }
            else {
              val dir = "publish.secretKeyPassword" -> p.asString.value
              (Nil, Seq(dir))
            }
          }
          .getOrElse((Nil, Nil))

        val setSecrets =
          Seq(SetSecret(
            "PUBLISH_SECRET_KEY",
            secretKey match {
              case Left(p)  => value(p.get(configDb())).getBytes().map(maybeEncodeBase64)
              case Right(p) => p.getBytes().map(maybeEncodeBase64)
            },
            force = true
          )) ++ passwordSetSecret
        OptionCheck.DefaultValue(
          () => pushKey().map(_ => Some("env:PUBLISH_SECRET_KEY")),
          extraDirectives,
          setSecrets
        )
      }
      else if (value(configDb().get(Keys.pgpSecretKey)).isDefined) {
        val hasPubKey   = value(configDb().get(Keys.pgpPublicKey)).isDefined
        val hasPassword = value(configDb().get(Keys.pgpSecretKeyPassword)).isDefined
        if (!hasPubKey)
          logger.message("Warning: no PGP public key found in config")
        if (!hasPassword)
          logger.message("Warning: no PGP secret key password found in config")
        OptionCheck.DefaultValue.empty
      }
      else
        value {
          Left(
            new MissingPublishOptionError(
              "publish secret key",
              "",
              "publish.secretKey",
              configKeys = Seq(Keys.pgpSecretKey.fullName)
            )
          )
        }
    }
}

object PgpSecretKeyCheck {
  final class KeyServerError(message: String) extends BuildException(message)
}
