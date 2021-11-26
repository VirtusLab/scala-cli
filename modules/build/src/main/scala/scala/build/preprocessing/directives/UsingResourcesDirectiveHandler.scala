package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Position
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
          val root   = value(Directive.osRoot(cwd, Some(directive.position)))
          val paths0 = paths.map(os.Path(_, root))
          BuildOptions(
            classPathOptions = ClassPathOptions(
              extraClassPath = paths0
            )
          )
        }
        Some(res)
      case _ =>
        None
    }

  override def keys = Seq("resourceDir", "resourceDirs")
  override def handleValues(
    values: Seq[Any],
    cwd: ScopePath,
    positionOpt: Option[Position]
  ): Either[BuildException, BuildOptions] = either {
    val root   = value(Directive.osRoot(cwd, positionOpt))
    val paths  = DirectiveUtil.stringValues(values)
    val paths0 = paths.map(os.Path(_, root))
    BuildOptions(
      classPathOptions = ClassPathOptions(
        extraClassPath = paths0
      )
    )
  }
}
