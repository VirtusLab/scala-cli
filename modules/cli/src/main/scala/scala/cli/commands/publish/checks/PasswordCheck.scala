package scala.cli.commands.publish.checks

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions, SetSecret}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.errors.MissingPublishOptionError

final case class PasswordCheck(
  options: PublishSetupOptions,
  configDb: () => ConfigDb,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Repository
  def fieldName     = "password"
  def directivePath = "publish" + (if (options.publishParams.setupCi) ".ci" else "") + ".password"

  def check(pubOpt: BPublishOptions): Boolean =
    !options.publishParams.setupCi ||
    pubOpt.retained(options.publishParams.setupCi).repoPassword.nonEmpty

  def defaultValue(): Either[BuildException, OptionCheck.DefaultValue] =
    either {
      if (options.publishParams.setupCi) {
        val password = options.publishRepo.password match {
          case Some(password0) => password0
          case None =>
            val passwordOpt = value(configDb().get(Keys.sonatypePassword))
            passwordOpt match {
              case Some(password0) =>
                logger.message("publish.password:")
                logger.message(
                  s"  using ${Keys.sonatypePassword.fullName} from Scala CLI configuration"
                )
                password0
              case None =>
                value {
                  Left {
                    new MissingPublishOptionError(
                      "publish password",
                      "--password",
                      "publish.password",
                      configKeys = Seq(Keys.sonatypePassword.fullName)
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
      else if (value(configDb().get(Keys.sonatypePassword)).isDefined) {
        logger.message("publish.password:")
        logger.message(s"  found ${Keys.sonatypePassword.fullName} in Scala CLI configuration")
        OptionCheck.DefaultValue.empty
      }
      else
        value {
          Left {
            new MissingPublishOptionError(
              "publish password",
              "",
              "publish.password",
              configKeys = Seq(Keys.sonatypePassword.fullName)
            )
          }
        }
    }
}
