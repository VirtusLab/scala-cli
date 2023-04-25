package scala.build.preprocessing.directives
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException, WrongJarPathError}
import scala.build.options.{BuildOptions, ClassPathOptions, Scope, WithBuildRequirements}
import scala.build.preprocessing.ScopePath
import scala.build.preprocessing.directives.CustomJar.buildOptions
import scala.build.{Logger, Positioned}
import scala.cli.commands.SpecificationLevel
import scala.util.{Failure, Success, Try}

@DirectiveGroupName("Custom JAR")
@DirectiveExamples(
  "//> using jar \"/Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar\""
) @DirectiveExamples(
  "//> using test.jar \"/Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar\""
)
@DirectiveUsage(
  "`//> using jar `_path_ | `//> using jars `_path1_, _path2_ …",
  """//> using jar _path_
    |
    |//> using jars _path1_, _path2_ …""".stripMargin
)
@DirectiveDescription("Manually add JAR(s) to the class path")
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class CustomJar(
  @DirectiveName("jars")
  jar: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil),
  @DirectiveName("test.jar")
  @DirectiveName("test.jars")
  testJar: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil)
) extends HasBuildOptionsWithRequirements {
  override def buildOptionsWithRequirements
    : Either[BuildException, List[WithBuildRequirements[BuildOptions]]] =
    Seq(
      buildOptions(jar).map(_.withEmptyRequirements),
      buildOptions(testJar).map(_.withScopeRequirement(Scope.Test))
    )
      .sequence
      .left
      .map(CompositeBuildException(_))
      .map(_.toList)

}

object CustomJar {
  val handler: DirectiveHandler[CustomJar] = DirectiveHandler.derive

  def buildOptions(jar: DirectiveValueParser.WithScopePath[List[Positioned[String]]])
    : Either[BuildException, BuildOptions] = {
    val cwd = jar.scopePath
    jar.value
      .map { posPathStr =>
        val eitherRootPathOrBuildException =
          Directive.osRoot(cwd, posPathStr.positions.headOption)
        eitherRootPathOrBuildException.flatMap { root =>
          Try(os.Path(posPathStr.value, root))
            .toEither
            .left.map(new WrongJarPathError(_))
        }
      }
      .sequence
      .left.map(CompositeBuildException(_))
      .map { paths =>
        BuildOptions(
          classPathOptions = ClassPathOptions(
            extraClassPath = paths
          )
        )
      }
  }
}
