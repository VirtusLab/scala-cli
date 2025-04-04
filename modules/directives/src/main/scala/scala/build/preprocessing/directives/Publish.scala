package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.publish.{Developer, License, Vcs}
import scala.build.options.{BuildOptions, PostBuildOptions, PublishOptions}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Publish")
@DirectivePrefix("publish.")
@DirectiveExamples("//> using publish.organization io.github.myself")
@DirectiveExamples("//> using publish.name my-library")
@DirectiveExamples("//> using publish.moduleName scala-cli_3")
@DirectiveExamples("//> using publish.version 0.1.1")
@DirectiveExamples("//> using publish.url https://github.com/VirtusLab/scala-cli")
@DirectiveExamples("//> using publish.license MIT")
@DirectiveExamples("//> using publish.vcs https://github.com/VirtusLab/scala-cli.git")
@DirectiveExamples("//> using publish.vcs github:VirtusLab/scala-cli")
@DirectiveExamples("//> using publish.description \"Lorem ipsum dolor sit amet\"")
@DirectiveExamples("//> using publish.developer alexme|Alex Me|https://alex.me")
@DirectiveExamples(
  "//> using publish.developers alexme|Alex Me|https://alex.me Gedochao|Gedo Chao|https://github.com/Gedochao"
)
@DirectiveExamples("//> using publish.scalaVersionSuffix _2.13")
@DirectiveExamples("//> using publish.scalaVersionSuffix _3")
@DirectiveExamples("//> using publish.scalaPlatformSuffix _sjs1")
@DirectiveExamples("//> using publish.scalaPlatformSuffix _native0.4")
@DirectiveUsage(
  "//> using publish.[key] [value]",
  """`//> using publish.organization` value
    |
    |`//> using publish.name` value
    |
    |`//> using publish.moduleName` value
    |
    |`//> using publish.version` value
    |
    |`//> using publish.url` value
    |
    |`//> using publish.license` value
    |
    |`//> using publish.vcs` value
    |`//> using publish.scm` value
    |`//> using publish.versionControl` value
    |
    |`//> using publish.description` value
    |
    |`//> using publish.developer` value
    |`//> using publish.developers` value1 value2
    |
    |`//> using publish.scalaVersionSuffix` value
    |
    |`//> using publish.scalaPlatformSuffix` value
    |
    |""".stripMargin
)
@DirectiveDescription("Set parameters for publishing")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class Publish(
  organization: Option[Positioned[String]] = None,
  name: Option[Positioned[String]] = None,
  moduleName: Option[Positioned[String]] = None,
  version: Option[Positioned[String]] = None,
  url: Option[Positioned[String]] = None,
  license: Option[Positioned[String]] = None,
  @DirectiveName("scm")
  @DirectiveName("versionControl")
  vcs: Option[Positioned[String]] = None,
  description: Option[String] = None,
  @DirectiveName("developer")
  developers: List[Positioned[String]] = Nil,
  scalaVersionSuffix: Option[String] = None,
  scalaPlatformSuffix: Option[String] = None
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = either {

    val maybeLicense = license
      .map(License.parse)
      .sequence
    val maybeVcs = vcs
      .map(Vcs.parse)
      .sequence
    val maybeDevelopers = developers
      .map(Developer.parse)
      .sequence
      .left.map(CompositeBuildException(_))

    val (licenseOpt, vcsOpt, developers0) = value {
      (maybeLicense, maybeVcs, maybeDevelopers)
        .traverseN
        .left.map(CompositeBuildException(_))
    }

    val publishOptions = PublishOptions(
      organization = organization,
      name = name,
      moduleName = moduleName,
      version = version,
      url = url,
      license = licenseOpt,
      versionControl = vcsOpt,
      description = description,
      developers = developers0,
      scalaVersionSuffix = scalaVersionSuffix,
      scalaPlatformSuffix = scalaPlatformSuffix
    )

    BuildOptions(
      notForBloopOptions = PostBuildOptions(
        publishOptions = publishOptions
      )
    )
  }
}

object Publish {
  val handler: DirectiveHandler[Publish] = DirectiveHandler.derive
}
