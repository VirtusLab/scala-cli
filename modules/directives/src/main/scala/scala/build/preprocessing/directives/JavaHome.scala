package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.directives.*
import scala.build.errors.{BuildException, WrongJavaHomePathError}
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.{Logger, Positioned}
import scala.cli.commands.SpecificationLevel
import scala.util.{Failure, Success, Try}

@DirectiveGroupName("Java home")
@DirectiveExamples("//> using java-home \"/Users/Me/jdks/11\"")
@DirectiveUsage(
  "//> using java-home|javaHome _path_",
  """`//> using java-home `_path_
    |
    |`//> using javaHome `_path_""".stripMargin
)
@DirectiveDescription("Sets Java home used to run your application or tests")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class JavaHome(
  javaHome: DirectiveValueParser.WithScopePath[Option[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(None)
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = either {
    javaHome.value match {
      case None => BuildOptions()
      case Some(homePosStr) =>
        val root = value(Directive.osRoot(javaHome.scopePath, homePosStr.positions.headOption))
        val home = value {
          homePosStr
            .map { homeStr =>
              Try(os.Path(homeStr, root)).toEither.left.map { ex =>
                new WrongJavaHomePathError(homeStr, ex)
              }
            }
            .eitherSequence
        }
        BuildOptions(
          javaOptions = JavaOptions(
            javaHomeOpt = Some(home)
          )
        )
    }
  }
}

object JavaHome {
  val handler: DirectiveHandler[JavaHome] = DirectiveHandler.derive
}
