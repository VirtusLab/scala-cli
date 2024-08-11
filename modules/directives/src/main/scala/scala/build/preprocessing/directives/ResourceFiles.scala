package scala.build.preprocessing.directives

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.WithBuildRequirements.*
import scala.build.options.{BuildOptions, ClassPathOptions, Scope, WithBuildRequirements}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Resource files")
@DirectiveExamples("//> using resourceFile ./resource.txt")
@DirectiveExamples("//> using test.resourceFile ./resource.txt")
@DirectiveUsage(
  """//> using resourceFile _path_
    |
    |//> using resourceFiles _path1_ _path2_ …""".stripMargin,
  """`//> using resourceFile` _path_
    |
    |`//> using resourceFiles` _path1_ _path2_ …""".stripMargin
)
@DirectiveDescription("Manually add a resource files to the class path")
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class ResourceFiles(
  @DirectiveName("resourceFile")
  resourceFiles: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil),
  @DirectiveName("test.resourceFile")
  @DirectiveName("test.resourceFiles")
  testResourceFiles: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
    DirectiveValueParser.WithScopePath.empty(Nil)
) extends HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = List(
    Resources.buildOptions(resourceFiles).map(_.withEmptyRequirements),
    Resources.buildOptions(testResourceFiles).map(_.withScopeRequirement(Scope.Test))
  )
}

object ResourceFiles {
  val handler: DirectiveHandler[ResourceFiles] = DirectiveHandler.derive
  def buildOptions(resourceFiles: DirectiveValueParser.WithScopePath[List[Positioned[String]]])
    : Either[BuildException, BuildOptions] = Right {
    val paths = resourceFiles.value.map(_.value)

    val (virtualRootOpt, rootOpt) = Directive.osRootResource(resourceFiles.scopePath)

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
        resourceFiles = paths0,
        resourcesVirtualDir = virtualPaths.toList.flatten
      )
    )
  }
}
