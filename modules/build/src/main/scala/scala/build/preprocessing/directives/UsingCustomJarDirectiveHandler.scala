package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Position
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions}
import scala.build.preprocessing.ScopePath

case object UsingCustomJarDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Custom JAR"
  def description = "Manually add JAR(s) to the class path"
  def usage       = "`using jar `_path_ | `using jars `_path1_ _path2_ …"
  override def usageMd =
    """using jar _path_
      |
      |using jars _path1_ _path2_ …""".stripMargin

  override def examples = Seq(
    "using jar \"/Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar\""
  )

  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("jar" | "jars", paths @ _*) =>
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

  override def keys = Seq("jar", "jars")
  override def handleValues(
    values: Seq[Any],
    cwd: ScopePath,
    positionOpt: Option[Position]
  ): Either[BuildException, BuildOptions] = either {

    val root = value(Directive.osRoot(cwd, positionOpt))
    val extraJars = DirectiveUtil.stringValues(values).map { p =>
      // FIXME Handle malformed paths here
      os.Path(p, root)
    }

    BuildOptions(
      classPathOptions = ClassPathOptions(
        extraClassPath = extraJars
      )
    )
  }
}
