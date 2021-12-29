package scala.build.preprocessing.directives
import scala.build.EitherCps.{either, value}
import scala.build.Ops.EitherSeqOps
import scala.build.errors.{BuildException, CompositeBuildException, NoResourcePathFoundError}
import scala.build.options.{BuildOptions, ClassPathOptions}
import scala.build.preprocessing.ScopePath

case object UsingResourcesDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Resource directories"
  def description = "Manually add a resource directory to the class path"
  def usage = """// using resource _path_
                |
                |// using resources _path1_, _path2_ …""".stripMargin
  override def usageMd =
    """`// using resourceDir `_path_
      |
      |`// using resourceDirs `_path1_, _path2_ …""".stripMargin
  override def examples = Seq(
    "// using resourceDir \"./resources\""
  )

  override def keys = Seq("resourceDir", "resourceDirs")
  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, ProcessedUsingDirective] = either {
    val (virtualRootOpt, rootOpt) = Directive.osRootResource(cwd)
    val paths                     = DirectiveUtil.stringValues(directive.values, path, cwd)
    val paths0: Seq[os.Path] = value {
      rootOpt.toList.flatMap(root => paths.map(_._1).map(os.Path(_, root)))
        .map(validatePath)
        .sequence
        .left.map(CompositeBuildException(_))
    }
    val virtualPaths = virtualRootOpt.map(virtualRoot =>
      paths.map(_._1).map(path => virtualRoot / os.SubPath(path))
    )

    ProcessedDirective(
      Some(BuildOptions(
        classPathOptions = ClassPathOptions(
          resourcesDir = paths0,
          resourcesVirtualDir = virtualPaths.toList.flatten
        )
      )),
      Seq.empty
    )
  }
  def validatePath(path: os.Path): Either[BuildException, os.Path] =
    if (os.exists(path))
      Right(path)
    else
      Left(new NoResourcePathFoundError(path))
}
