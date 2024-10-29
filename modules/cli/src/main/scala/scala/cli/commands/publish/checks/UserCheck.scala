package scala.cli.commands.publish.checks

import java.net.URI

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.PublishOptions as BPublishOptions
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions, RepoParams, SetSecret}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.errors.MissingPublishOptionError

final case class UserCheck(
  options: PublishSetupOptions,
  configDb: () => ConfigDb,
  workspace: os.Path,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Repository
  def fieldName     = "user"
  def directivePath = "publish" + (if (options.publishParams.setupCi) ".ci" else "") + ".user"

  private def userOpt(pubOpt: BPublishOptions) =
    CheckUtils.getHostOpt(
      options,
      pubOpt,
      workspace,
      logger
    ) match {
      case None => Right(None)
      case Some(host) =>
        configDb().get(Keys.publishCredentials).wrapConfigException.map { credListOpt =>
          credListOpt.flatMap { credList =>
            credList
              .iterator
              .filter(_.host == host)
              .map(_.user)
              .collectFirst {
                case Some(p) =>
                  p
              }
          }
        }
    }

  def check(pubOpt: BPublishOptions): Boolean =
    pubOpt.retained(options.publishParams.setupCi).repoUser.nonEmpty || {
      !options.publishParams.setupCi && (userOpt(pubOpt) match {
        case Left(ex) =>
          logger.debug("Ignoring error while trying to get user from config")
          logger.debug(ex)
          true
        case Right(valueOpt) =>
          valueOpt.isDefined
      })
    }

  def defaultValue(pubOpt: BPublishOptions): Either[BuildException, OptionCheck.DefaultValue] =
    either {
      if (options.publishParams.setupCi) {
        val user0 = options.publishRepo.user match {
          case Some(value0) => value0.toConfig
          case None =>
            value(userOpt(pubOpt)) match {
              case Some(user) =>
                logger.message("publish.user:")
                logger.message(
                  s"  using ${Keys.publishCredentials.fullName} from Scala CLI configuration"
                )
                user
              case None =>
                value {
                  Left {
                    new MissingPublishOptionError(
                      "publish user",
                      "--user",
                      "publish.credentials",
                      configKeys = Seq(Keys.publishCredentials.fullName)
                    )
                  }
                }
            }
        }

        OptionCheck.DefaultValue.simple(
          "env:PUBLISH_USER",
          Nil,
          Seq(SetSecret("PUBLISH_USER", user0.get(), force = true))
        )
      }
      else
        CheckUtils.getHostOpt(
          options,
          pubOpt,
          workspace,
          logger
        ) match {
          case None =>
            logger.debug("No host, not checking for publish repository user")
            OptionCheck.DefaultValue.empty
          case Some(host) =>
            if (value(userOpt(pubOpt).wrapConfigException).isDefined) {
              logger.message("publish.credentials:")
              logger.message(
                s"  found user for $host in ${Keys.publishCredentials.fullName} in Scala CLI configuration"
              )
              OptionCheck.DefaultValue.empty
            }
            else {
              val optionName =
                CheckUtils.getRepoOpt(options, pubOpt).map(r => s"publish user for $r").getOrElse(
                  "publish user"
                )
              value {
                Left {
                  new MissingPublishOptionError(
                    optionName,
                    "",
                    "publish.credentials",
                    configKeys = Seq(Keys.publishCredentials.fullName)
                  )
                }
              }
            }
        }
    }
}
