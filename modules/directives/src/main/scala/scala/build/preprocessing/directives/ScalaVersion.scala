package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, MaybeScalaVersion, ScalaOptions, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Scala version")
@DirectiveExamples("//> using scala \"3.0.2\"")
@DirectiveExamples("//> using scala \"2.13\"")
@DirectiveExamples("//> using scala \"2\"")
@DirectiveExamples("//> using scala \"2.13.6\", \"2.12.16\"")
@DirectiveUsage(
  "//> using scala _version_+",
  "`//> using scala `_version_+"
)
@DirectiveDescription("Set the default Scala version")
@DirectiveLevel(SpecificationLevel.MUST)
// format: off
final case class ScalaVersion(
  scala: List[DirectiveValueParser.MaybeNumericalString] = Nil
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] =
    scala match {
      case Nil => Right(BuildOptions())
      case first :: others =>
        val buildOpt = BuildOptions(
          scalaOptions = ScalaOptions(
            scalaVersion = Some(MaybeScalaVersion(first.value)),
            extraScalaVersions = others.map(_.value).toSet
          )
        )
        Right(buildOpt)
    }
}

object ScalaVersion {
  val handler: DirectiveHandler[ScalaVersion] = DirectiveHandler.derive
}
