package scala.cli.commands.publish.checks

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.PublishOptions as BPublishOptions
import scala.cli.commands.publish.ConfigUtil.*
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

  def defaultValue(pubOpt: BPublishOptions): Either[BuildException, OptionCheck.DefaultValue] =
    either {
      // FIXME No headOption, add all of options.publishParams.developer values…
      val strValue = options.publishParams.developer.headOption match {
        case None =>
          val nameOpt  = value(configDb().get(Keys.userName).wrapConfigException)
          val emailOpt = value(configDb().get(Keys.userEmail).wrapConfigException)
          val urlOpt   = value(configDb().get(Keys.userUrl).wrapConfigException)

          (nameOpt, emailOpt, urlOpt) match {
            case (Some(name), Some(email), Some(url)) =>
              logger.message("developers:")
              logger.message(s"  using $name <$email> ($url) from config")
              DeveloperCheck.directiveValue(name = name, email = email, url = url)
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

object DeveloperCheck {
  private val gitHubProfileUrl = "https?://(?:www\\.)?github\\.com/([^/]+)/?".r

  /** The developer ID to publish: the GitHub login if `url` is a GitHub profile, else `name` */
  def developerId(name: String, url: String): String =
    url.trim match {
      case gitHubProfileUrl(login) => login
      case _                       => name
    }

  /** A `publish.developer` directive value, in its `id|name|URL|email` format */
  def directiveValue(name: String, email: String, url: String): String =
    s"${developerId(name, url)}|$name|$url|$email"
}
