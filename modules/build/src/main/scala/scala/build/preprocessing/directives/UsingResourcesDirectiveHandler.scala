package scala.build.preprocessing.directives

import scala.build.Os
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions}

case object UsingResourcesDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Resources"
  def description = "Manually adds a resource directory to the class path"
  def usage       = "using resource _path_ | using resources _path1_ _path2_ …"
  override def usageMd =
    "`using resource `_path_ | `using resources `_path1_ _path2_ …"
  override def examples = Seq(
    "using resource \"./resources\""
  )

  def handle(directive: Directive): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("resource" | "resources", paths @ _*) =>
        val paths0 = paths.map(os.Path(_, Os.pwd))
        val options = BuildOptions(
          classPathOptions = ClassPathOptions(
            extraClassPath = paths0
          )
        )
        Some(Right(options))
      case _ =>
        None
    }
}
