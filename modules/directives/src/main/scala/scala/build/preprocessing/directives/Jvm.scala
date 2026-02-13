package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("JVM version")
@DirectiveExamples("//> using jvm 11")
@DirectiveExamples("//> using jvm temurin:11")
@DirectiveExamples("//> using jvm graalvm:21")
@DirectiveUsage(
  "//> using jvm _value_",
  "`//> using jvm` _value_"
)
@DirectiveDescription(
  "Use a specific JVM, such as `14`, `temurin:11`, or `graalvm:21`, or `system`. " +
    "scala-cli uses [coursier](https://get-coursier.io/) to fetch JVMs, so you can use `cs java --available` to list the available JVMs."
)
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class Jvm(jvm: Option[Positioned[String]] = None) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = {
    val buildOpt = BuildOptions(
      javaOptions = options.JavaOptions(
        jvmIdOpt = jvm
      )
    )
    Right(buildOpt)
  }
}

object Jvm {
  val handler: DirectiveHandler[Jvm] = DirectiveHandler.derive
}
