package scala.cli.commands.publish.checks

import java.net.URI

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.ConfigUtil._
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions, RepoParams, SetSecret}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.errors.MissingPublishOptionError

final case class PasswordCheck(
  options: PublishSetupOptions,
  configDb: () => ConfigDb,
  workspace: os.Path,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Repository
  def fieldName     = "password"
  def directivePath = "publish" + (if (options.publishParams.setupCi) ".ci" else "") + ".password"

  private def hostOpt(pubOpt: BPublishOptions): Option[String] = {
    val repo = pubOpt.contextual(options.publishParams.setupCi).repository.getOrElse(
      RepositoryCheck.defaultRepository
    )
    RepoParams(
      repo,
      pubOpt.versionControl.map(_.url),
      workspace,
      None,
      false,
      null,
      logger
    ) match {
      case Left(ex) =>
        logger.debug("Caught exception when trying to compute host to check user credentials")
        logger.debug(ex)
        None
      case Right(params) =>
        Some(new URI(params.repo.snapshotRepo.root).getHost)
    }
  }

  private def passwordOpt(pubOpt: BPublishOptions) = hostOpt(pubOpt) match {
    case None => Right(None)
    case Some(host) =>
      configDb().get(Keys.publishCredentials).wrapConfigException.map { credListOpt =>
        credListOpt.flatMap { credList =>
          credList
            .iterator
            .filter(_.host == host)
            .map(_.password)
            .collectFirst {
              case Some(p) => p
            }
        }
      }
  }

  def check(pubOpt: BPublishOptions): Boolean =
    pubOpt.retained(options.publishParams.setupCi).repoPassword.nonEmpty || {
      !options.publishParams.setupCi && (passwordOpt(pubOpt) match {
        case Left(ex) =>
          logger.debug("Ignoring error while trying to get password from config")
          logger.debug(ex)
          true
        case Right(valueOpt) =>
          valueOpt.isDefined
      })
    }

  def defaultValue(pubOpt: BPublishOptions): Either[BuildException, OptionCheck.DefaultValue] =
    either {

      if (options.publishParams.setupCi) {
        val password = options.publishRepo.password match {
          case Some(password0) => password0.toConfig
          case None =>
            value(passwordOpt(pubOpt)) match {
              case Some(password0) =>
                logger.message("publish.credentials:")
                logger.message(
                  s"  using ${Keys.publishCredentials.fullName} from Scala CLI configuration"
                )
                password0
              case None =>
                value {
                  Left {
                    new MissingPublishOptionError(
                      "publish password",
                      "--password",
                      "publish.credentials",
                      configKeys = Seq(Keys.publishCredentials.fullName)
                    )
                  }
                }
            }
        }

        OptionCheck.DefaultValue.simple(
          "env:PUBLISH_PASSWORD",
          Nil,
          Seq(SetSecret("PUBLISH_PASSWORD", password.get(), force = true))
        )
      }
      else
        hostOpt(pubOpt) match {
          case None =>
            logger.debug("No host, not checking for publish repository password")
            OptionCheck.DefaultValue.empty
          case Some(host) =>
            if (value(passwordOpt(pubOpt)).isDefined) {
              logger.message("publish.password:")
              logger.message(
                s"  found password for $host in ${Keys.publishCredentials.fullName} in Scala CLI configuration"
              )
              OptionCheck.DefaultValue.empty
            }
            else
              value {
                Left {
                  new MissingPublishOptionError(
                    "publish password",
                    "",
                    "publish.credentials",
                    configKeys = Seq(Keys.publishCredentials.fullName)
                  )
                }
              }
        }
    }
}
