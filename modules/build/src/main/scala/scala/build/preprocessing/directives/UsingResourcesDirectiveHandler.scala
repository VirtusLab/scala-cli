package scala.build.preprocessing.directives
import scala.build.EitherCps.either
import scala.build.errors.BuildException
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
    val paths0                    = rootOpt.map(root => paths.map(_._1).map(os.Path(_, root)))
    val virtualPaths = virtualRootOpt.map(virtualRoot =>
      paths.map(_._1).map(path => virtualRoot / os.SubPath(path))
    )

    ProcessedDirective(
      Some(BuildOptions(
        classPathOptions = ClassPathOptions(
          extraClassPath = paths0.toList.flatten,
          resourceVirtualDir = virtualPaths.toList.flatten
        )
      )),
      Seq.empty
    )
  }
}
