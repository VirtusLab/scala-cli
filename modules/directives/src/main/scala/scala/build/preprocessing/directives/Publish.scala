package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.publish.{Developer, License, Vcs}
import scala.build.options.{BuildOptions, JavaOpt, PostBuildOptions, PublishOptions, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Publish")
@DirectivePrefix("publish.")
@DirectiveExamples("//> using publish.organization \"io.github.myself\"")
@DirectiveExamples("//> using publish.name \"my-library\"")
@DirectiveExamples("//> using publish.version \"0.1.1\"")
@DirectiveUsage(
  "//> using publish.(organization|name|version) [value]",
  """`//> using publish.organization `"value"
    |`//> using publish.name `"value"
    |`//> using publish.version `"value"
    |""".stripMargin
)
@DirectiveDescription("Set parameters for publishing")
@DirectiveLevel(SpecificationLevel.RESTRICTED)
// format: off
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
  // format: on
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
