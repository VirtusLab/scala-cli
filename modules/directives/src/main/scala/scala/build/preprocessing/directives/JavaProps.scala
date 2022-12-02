package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Java properties")
@DirectiveExamples("//> using javaProp \"foo1=bar\", \"foo2\"")
@DirectiveUsage(
  "//> using java-prop|javaProp _key=val_",
  """`//> using javaProp `_key=value_
    |`//> using javaProp `_key_""".stripMargin
)
@DirectiveDescription("Add Java properties")
@DirectiveLevel(SpecificationLevel.MUST)
// format: off
final case class JavaProps(
  @DirectiveName("javaProp")
    javaProperty: List[Positioned[String]] = Nil
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = {
    val javaOpts = javaProperty.map { positioned =>
      positioned.map { v =>
        v.split("=") match {
          case Array(k)    => JavaOpt(s"-D$k")
          case Array(k, v) => JavaOpt(s"-D$k=$v")
        }
      }
    }
    val buildOpt = BuildOptions(
      javaOptions = options.JavaOptions(
        javaOpts = ShadowingSeq.from(javaOpts)
      )
    )
    Right(buildOpt)
  }
}

object JavaProps {
  val handler: DirectiveHandler[JavaProps] = DirectiveHandler.derive
}
