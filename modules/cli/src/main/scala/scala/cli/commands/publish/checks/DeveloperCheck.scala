package scala.cli.commands.publish.checks

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.errors.MissingPublishOptionError

final case class DeveloperCheck(
  options: PublishSetupOptions,
  configDb: () => ConfigDb,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Extra
  def fieldName     = "developers"
  def directivePath = "publish.developer"

  def check(pubOpt: BPublishOptions): Boolean =
    pubOpt.developers.nonEmpty

  def defaultValue(): Either[BuildException, OptionCheck.DefaultValue] =
    either {
      // FIXME No headOption, add all of options.publishParams.developer values…
      val strValue = options.publishParams.developer.headOption match {
        case None =>
          val nameOpt  = value(configDb().get(Keys.userName))
          val emailOpt = value(configDb().get(Keys.userEmail))
          val urlOpt   = value(configDb().get(Keys.userUrl))

          (nameOpt, emailOpt, urlOpt) match {
            case (Some(name), Some(email), Some(url)) =>
              logger.message("developers:")
              logger.message(s"  using $name <$email> ($url) from config")
              s"$name|$email|$url"
            case _ =>
              value {
                Left {
                  new MissingPublishOptionError(
                    "developer",
                    "--developer",
                    "publish.developer",
                    configKeys = Seq(
                      Keys.userName.fullName,
                      Keys.userEmail.fullName,
                      Keys.userUrl.fullName
                    )
                  )
                }
              }
          }
        case Some(value) =>
          value
      }

      OptionCheck.DefaultValue.simple(strValue, Nil, Nil)
    }
}
