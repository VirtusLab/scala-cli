package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value

import scala.build.EitherCps.either
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions}
import scala.build.preprocessing.ScopePath

case object UsingResourcesDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Resource directories"
  def description = "Manually add a resource directory to the class path"
  def usage = """using resource _path_
                |
                |using resources _path1_ _path2_ …""".stripMargin
  override def usageMd =
    """`using resourceDir `_path_
      |
      |`using resourceDirs `_path1_ _path2_ …""".stripMargin
  override def examples = Seq(
    "using resourceDir \"./resources\""
  )

  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("resourceDir" | "resourceDirs", paths @ _*) =>
        val res = either {
          val (virtualRootOpt, rootOpt) = Directive.osRootResource(cwd)
          val paths0                    = rootOpt.map(root => paths.map(os.Path(_, root)))
          val virtualPaths = virtualRootOpt.map(virtualRoot =>
            paths.map(path => virtualRoot / os.SubPath(path))
          )
          BuildOptions(
            classPathOptions = ClassPathOptions(
              extraClassPath = paths0.toList.flatten,
              resourceVirtualDir = virtualPaths.toList.flatten
            )
          )
        }
        Some(res)
      case _ =>
        None
    }

  override def keys = Seq("resourceDir", "resourceDirs")
  override def handleValues(
    values: Seq[Value[_]],
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, BuildOptions] = either {

    val (virtualRootOpt, rootOpt) = Directive.osRootResource(cwd)
    val paths                     = DirectiveUtil.stringValues(values, path)
    val paths0                    = rootOpt.map(root => paths.map(_._1).map(os.Path(_, root)))
    val virtualPaths = virtualRootOpt.map(virtualRoot =>
      paths.map(_._1).map(path => virtualRoot / os.SubPath(path))
    )

    BuildOptions(
      classPathOptions = ClassPathOptions(
        extraClassPath = paths0.toList.flatten,
        resourceVirtualDir = virtualPaths.toList.flatten
      )
    )
  }
}
