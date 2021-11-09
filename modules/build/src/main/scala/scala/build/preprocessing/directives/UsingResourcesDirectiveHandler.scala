package scala.build.preprocessing.directives

<<<<<<< HEAD
import scala.build.EitherCps.either
import scala.build.Position
import scala.build.errors.BuildException
=======
import com.virtuslab.using_directives.custom.model.Value

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, CompositeBuildException}
>>>>>>> b349648 (Bump using_directives version. Improve directives handlers. Add positions to directives.)
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
    cwd: ScopePath
  ): Either[BuildException, BuildOptions] = either {

    val (virtualRootOpt, rootOpt) = Directive.osRootResource(cwd)
    val paths                     = DirectiveUtil.stringValues(values)
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
