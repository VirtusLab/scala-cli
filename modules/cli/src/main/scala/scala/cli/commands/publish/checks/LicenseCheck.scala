package scala.cli.commands.publish.checks

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}
import scala.cli.commands.publish.{OptionCheck, PublishSetupOptions}

final case class LicenseCheck(
  options: PublishSetupOptions,
  logger: Logger
) extends OptionCheck {
  def kind          = OptionCheck.Kind.Extra
  def fieldName     = "license"
  def directivePath = "publish.license"

  def check(pubOpt: BPublishOptions): Boolean =
    pubOpt.license.nonEmpty

  private def defaultLicense = "Apache-2.0"
  def defaultValue(pubOpt: BPublishOptions): Either[BuildException, OptionCheck.DefaultValue] = {
    val license = options.publishParams.license.getOrElse {
      logger.message("license:")
      logger.message(s"  using $defaultLicense (default)")
      defaultLicense
    }
    Right(OptionCheck.DefaultValue.simple(license, Nil, Nil))
  }
}
