package scala.cli.commands

import caseapp._

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.Positioned
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.{BuildOptions, PublishOptions => BPublishOptions}

// format: off
final case class PublishOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    compileCross: CompileCrossOptions = CompileCrossOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions(),

  @Group("Publishing")
  @HelpMessage("Directory where temporary files for publishing should be written")
  @Hidden
    workingDir: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("Organization to publish artifacts under")
    organization: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("Module name to publish artifacts as")
    moduleName: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("Version to publish artifacts as")
    version: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("URL to put in publishing metadata")
    url: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("License to put in publishing metadata")
  @ValueDescription("name:URL")
    license: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("VCS information to put in publishing metadata")
    vcs: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("Description to put in publishing metadata")
    description: Option[String] = None,
  @Group("Publishing")
  @HelpMessage("Developer(s) to add in publishing metadata, like \"alex|Alex|https://alex.info\" or \"alex|Alex|https://alex.info|alex@alex.me\"")
  @ValueDescription("id|name|URL|email")
    developer: List[String] = Nil,

  @Group("Publishing")
  @Hidden
  @HelpMessage("Scala version suffix to append to the module name, like \"_2.13\" or \"_3\"")
  @ValueDescription("suffix")
    scalaVersionSuffix: Option[String] = None,
  @Group("Publishing")
  @Hidden
  @HelpMessage("Scala platform suffix to append to the module name, like \"_sjs1\" or \"_native0.4\"")
  @ValueDescription("suffix")
    scalaPlatformSuffix: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("Repository to publish to")
  @ValueDescription("URL or path")
  @ExtraName("R")
  @ExtraName("publishRepo")
    publishRepository: Option[String] = None,

  @Group("Publishing")
  @HelpMessage("Whether to build and publish source JARs")
    sources: Option[Boolean] = None
) {
  // format: on

  def buildOptions: Either[BuildException, BuildOptions] = either {
    val baseOptions = shared.buildOptions(enableJmh = false, jmhVersion = None)
    baseOptions.copy(
      mainClass = mainClass.mainClass.filter(_.nonEmpty),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        publishOptions = baseOptions.notForBloopOptions.publishOptions.copy(
          organization = organization.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          name = moduleName.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          version = version.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          url = url.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine(_)),
          license = value {
            license
              .map(_.trim).filter(_.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(BPublishOptions.parseLicense(_))
              .sequence
          },
          versionControl = value {
            vcs.map(_.trim).filter(_.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(BPublishOptions.parseVcs(_))
              .sequence
          },
          description = description.map(_.trim).filter(_.nonEmpty),
          developers = value {
            developer.filter(_.trim.nonEmpty)
              .map(Positioned.commandLine(_))
              .map(BPublishOptions.parseDeveloper(_))
              .sequence
              .left.map(CompositeBuildException(_))
          },
          scalaVersionSuffix = scalaVersionSuffix.map(_.trim),
          scalaPlatformSuffix = scalaPlatformSuffix.map(_.trim),
          repository = publishRepository.filter(_.trim.nonEmpty),
          sourceJar = sources
        )
      )
    )
  }
}

object PublishOptions {
  implicit lazy val parser: Parser[PublishOptions] = Parser.derive
  implicit lazy val help: Help[PublishOptions]     = Help.derive
}
