package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.WithBuildRequirements.*
import scala.build.options.{BuildOptions, JavaOpt, Scope, ShadowingSeq, WithBuildRequirements}
import scala.build.preprocessing.directives.DirectiveUtil.*
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Java properties")
@DirectiveExamples("//> using javaProp foo1=bar foo2")
@DirectiveExamples("//> using test.javaProp foo3=bar foo4")
@DirectiveUsage(
  "//> using javaProp _key=val_",
  """`//> using javaProp` _key=value_
    |
    |`//> using javaProp` _key_
    |
    |`//> using test.javaProp` _key=value_
    |
    |`//> using test.javaProp` _key_
    |""".stripMargin
)
@DirectiveDescription("Add Java properties")
@DirectiveLevel(SpecificationLevel.MUST)
final case class JavaProps(
  @DirectiveName("javaProp")
  javaProperty: List[Positioned[String]] = Nil,
  @DirectiveName("test.javaProperty")
  @DirectiveName("test.javaProp")
  testJavaProperty: List[Positioned[String]] = Nil
) extends HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = List(
    JavaProps.buildOptions(javaProperty).map(_.withEmptyRequirements),
    JavaProps.buildOptions(testJavaProperty).map(_.withScopeRequirement(Scope.Test))
  )
}

object JavaProps {
  val handler: DirectiveHandler[JavaProps] = DirectiveHandler.derive
  def buildOptions(javaProperties: List[Positioned[String]]): Either[BuildException, BuildOptions] =
    Right {
      val javaOpts: Seq[Positioned[JavaOpt]] = javaProperties.map { positioned =>
        positioned.map { v =>
          v.split("=") match {
            case Array(k)    => JavaOpt(s"-D$k")
            case Array(k, v) => JavaOpt(s"-D$k=$v")
          }
        }
      }
      BuildOptions(
        javaOptions = options.JavaOptions(
          javaOpts = ShadowingSeq.from(javaOpts)
        )
      )
    }
}
