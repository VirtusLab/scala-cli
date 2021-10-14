package scala.build.preprocessing.directives

import scala.build.Os
import scala.build.Position
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions}
import scala.build.preprocessing.ScopePath

case object UsingResourcesDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Resources"
  def description = "Manually adds a resource directory to the class path"
  def usage       = "using resource _path_ | using resources _path1_ _path2_ …"
  override def usageMd =
    "`using resource `_path_ | `using resources `_path1_ _path2_ …"
  override def examples = Seq(
    "using resource \"./resources\""
  )

  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("resource" | "resources", paths @ _*) =>
        val paths0 = paths.map(os.Path(_, Os.pwd)) // FIXME Wrong cwd, might throw too
        val options = BuildOptions(
          classPathOptions = ClassPathOptions(
            extraClassPath = paths0
          )
        )
        Some(Right(options))
      case _ =>
        None
    }

  override def keys = Seq("resource", "resources")
  override def handleValues(
    values: Seq[Any],
    cwd: ScopePath,
    positionOpt: Option[Position]
  ): Either[BuildException, BuildOptions] = {
    val paths  = DirectiveUtil.stringValues(values)
    val paths0 = paths.map(os.Path(_, Os.pwd)) // FIXME Wrong cwd, might throw too
    val options = BuildOptions(
      classPathOptions = ClassPathOptions(
        extraClassPath = paths0
      )
    )
    Right(options)
  }
}
