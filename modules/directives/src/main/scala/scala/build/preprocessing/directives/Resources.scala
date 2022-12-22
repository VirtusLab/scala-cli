package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions, JavaOpt, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Resource directories")
@DirectiveExamples("//> using resourceDir \"./resources\"")
@DirectiveUsage(
  """//> using resourceDir _path_
    |
    |//> using resourceDirs _path1_, _path2_ …""".stripMargin,
  """`//> using resourceDir `_path_
    |
    |`//> using resourceDirs `_path1_, _path2_ …""".stripMargin
)
@DirectiveDescription("Manually add a resource directory to the class path")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class Resources(
  @DirectiveName("resourceDir")
    resourceDirs: DirectiveValueParser.WithScopePath[List[Positioned[String]]] =
      DirectiveValueParser.WithScopePath.empty(Nil)
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = {
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

    val buildOpt = BuildOptions(
      classPathOptions = ClassPathOptions(
        resourcesDir = paths0,
        resourcesVirtualDir = virtualPaths.toList.flatten
      )
    )
    Right(buildOpt)
  }
}

object Resources {
  val handler: DirectiveHandler[Resources] = DirectiveHandler.derive
}
