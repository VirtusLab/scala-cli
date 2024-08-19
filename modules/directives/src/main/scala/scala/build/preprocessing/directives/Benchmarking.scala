package scala.build.preprocessing.directives

import dependency.*

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.{
  BuildOptions,
  ClassPathOptions,
  JmhOptions,
  ScalaNativeOptions,
  ShadowingSeq
}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Benchmarking options")
@DirectiveExamples(s"//> using jmhVersion ${Constants.jmhVersion}")
@DirectiveUsage(
  "//> using jmh _value_ | using jmhVersion _value_",
  """`//> using jmh` _value_
    |
    |`//> using jmhVersion` _value_
    """.stripMargin.trim
)
@DirectiveDescription("Add benchmarking options")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
case class Benchmarking(
  jmh: Option[Boolean] = None,
  jmhVersion: Option[Positioned[String]] = None
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    Right(
      BuildOptions(
        jmhOptions = JmhOptions(
          enableJmh = jmh,
          runJmh = jmh,
          jmhVersion = jmhVersion.map(_.value)
        )
      )
    )
}

object Benchmarking {
  val handler: DirectiveHandler[Benchmarking] = DirectiveHandler.derive
}
