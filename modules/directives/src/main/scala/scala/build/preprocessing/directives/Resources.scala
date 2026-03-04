package scala.build.preprocessing.directives

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.WithBuildRequirements.*
import scala.build.options.{BuildOptions, ClassPathOptions, Scope, WithBuildRequirements}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Resource directories")
@DirectiveExamples("//> using resourceDir ./resources")
@DirectiveExamples("//> using test.resourceDir ./resources")
@DirectiveUsage(
  """//> using resourceDir _path_
    |
    |//> using resourceDirs _path1_ _path2_ …""".stripMargin,
  """`//> using resourceDir` _path_
    |
    |`//> using resourceDirs` _path1_ _path2_ …
    |
    |`//> using test.resourceDir` _path_
    |
    |`//> using test.resourceDirs` _path1_ _path2_ …
    |
    |""".stripMargin
)
@DirectiveDescription("Manually add a resource directory to the class path")
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class Resources(
  @DirectiveName("resourceDir")
  resourceDirs: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil),
  @DirectiveName("test.resourceDir")
  @DirectiveName("test.resourceDirs")
  testResourceDirs: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil)
) extends HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = List(
    Resources.buildOptions(resourceDirs).map(_.withEmptyRequirements),
    Resources.buildOptions(testResourceDirs).map(_.withScopeRequirement(Scope.Test))
  )
}

object Resources {
  val handler: DirectiveHandler[Resources] = DirectiveHandler.derive
  def buildOptions(resourceDirs: DirectiveValueParser.WithScopePath[List[Positioned[String]]])
    : Either[BuildException, BuildOptions] = Right {
    val paths = resourceDirs.value.map(_.value)

    val (virtualRootOpt, rootOpt) = Directive.osRootResource(resourceDirs.scopePath)

    // TODO Return a BuildException for malformed paths

    val paths0 = rootOpt
      .toList
      .flatMap { root =>
        paths.map(os.Path(_, root))
      }
    val virtualPaths = virtualRootOpt.map { virtualRoot =>
      paths.map(path => virtualRoot / os.SubPath(path))
    }
    // warnIfNotExistsPath(paths0, logger) // this should be reported elsewhere (more from BuildOptions)

    BuildOptions(
      classPathOptions = ClassPathOptions(
        resourcesDir = paths0,
        resourcesVirtualDir = virtualPaths.toList.flatten
      )
    )
  }
}
