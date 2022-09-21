package scala.cli.commands.publish.checks

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.ConfigUtil._
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions, SetSecret}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.errors.MissingPublishOptionError

final case class UserCheck(
  options: PublishSetupOptions,
  configDb: () => ConfigDb,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Repository
  def fieldName     = "user"
  def directivePath = "publish" + (if (options.publishParams.setupCi) ".ci" else "") + ".user"
  def check(pubOpt: BPublishOptions): Boolean =
    !options.publishParams.setupCi ||
    pubOpt.retained(options.publishParams.setupCi).repoUser.nonEmpty
  def defaultValue(): Either[BuildException, OptionCheck.DefaultValue] =
    either {
      if (options.publishParams.setupCi) {
        val user0 = options.publishRepo.user match {
          case Some(value0) => value0
          case None =>
            val userOpt = value(configDb().get(Keys.sonatypeUser).wrapConfigException)
            userOpt match {
              case Some(user) =>
                logger.message("publish.user:")
                logger.message(
                  s"  using ${Keys.sonatypeUser.fullName} from Scala CLI configuration"
                )
                user
              case None =>
                value {
                  Left {
                    new MissingPublishOptionError(
                      "publish user",
                      "--user",
                      "publish.user",
                      configKeys = Seq(Keys.sonatypeUser.fullName)
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
      else if (value(configDb().get(Keys.sonatypeUser).wrapConfigException).isDefined) {
        logger.message("publish.user:")
        logger.message(s"  found ${Keys.sonatypeUser.fullName} in Scala CLI configuration")
        OptionCheck.DefaultValue.empty
      }
      else
        value {
          Left {
            new MissingPublishOptionError(
              "publish user",
              "",
              "publish.user",
              configKeys = Seq(Keys.sonatypeUser.fullName)
            )
          }
        }
    }
}
