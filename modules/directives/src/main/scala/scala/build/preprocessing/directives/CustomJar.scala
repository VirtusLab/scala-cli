package scala.build.preprocessing.directives
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException, WrongJarPathError}
import scala.build.options.WithBuildRequirements.*
import scala.build.options.{BuildOptions, ClassPathOptions, Scope, WithBuildRequirements}
import scala.build.preprocessing.ScopePath
import scala.build.preprocessing.directives.ClasspathUtils.*
import scala.build.preprocessing.directives.CustomJar.JarType
import scala.build.{Logger, Positioned}
import scala.cli.commands.SpecificationLevel
import scala.util.{Failure, Success, Try}

@DirectiveGroupName("Custom JAR")
@DirectiveExamples(
  "//> using jar /Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar"
)
@DirectiveExamples(
  "//> using test.jar /Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar"
)
@DirectiveExamples("//> using sourceJar /path/to/custom-jar-sources.jar")
@DirectiveExamples(
  "//> using sourceJars /path/to/custom-jar-sources.jar /path/to/another-jar-sources.jar"
)
@DirectiveExamples("//> using test.sourceJar /path/to/test-custom-jar-sources.jar")
@DirectiveUsage(
  "`//> using jar `_path_ | `//> using jars `_path1_ _path2_ …",
  """//> using jar _path_
    |
    |//> using jars _path1_ _path2_ …""".stripMargin
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
    DirectiveValueParser.WithScopePath.empty(Nil),
  @DirectiveName("sources.jar")
  @DirectiveName("sourcesJars")
  @DirectiveName("sources.jars")
  @DirectiveName("sourceJar")
  @DirectiveName("source.jar")
  @DirectiveName("sourceJars")
  @DirectiveName("source.jars")
  sourcesJar: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil),
  @DirectiveName("test.sources.jar")
  @DirectiveName("test.sourcesJars")
  @DirectiveName("test.sources.jars")
  @DirectiveName("test.sourceJar")
  @DirectiveName("test.source.jar")
  @DirectiveName("test.sourceJars")
  @DirectiveName("test.source.jars")
  testSourcesJar: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil)
) extends HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] =
    List(
      CustomJar.buildOptions(jar, JarType.Jar)
        .map(_.withEmptyRequirements),
      CustomJar.buildOptions(testJar, JarType.Jar)
        .map(_.withScopeRequirement(Scope.Test)),
      CustomJar.buildOptions(sourcesJar, JarType.SourcesJar)
        .map(_.withEmptyRequirements),
      CustomJar.buildOptions(testSourcesJar, JarType.SourcesJar)
        .map(_.withScopeRequirement(Scope.Test))
    )
}

object CustomJar {
  val handler: DirectiveHandler[CustomJar] = DirectiveHandler.derive
  enum JarType:
    case Jar, SourcesJar
  def buildOptions(
    jar: DirectiveValueParser.WithScopePath[List[Positioned[String]]],
    jarType: JarType
  ): Either[BuildException, BuildOptions] = {
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
        val classPathOptions = jarType match
          case JarType.Jar =>
            val (sourceJars, regularJars) = paths.partition(_.hasSourceJarSuffix)
            ClassPathOptions(extraClassPath = regularJars, extraSourceJars = sourceJars)
          case JarType.SourcesJar => ClassPathOptions(extraSourceJars = paths)
        BuildOptions(classPathOptions = classPathOptions)
      }
  }
}
