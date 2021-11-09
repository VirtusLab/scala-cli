package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, CompositeBuildException}
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
    values: Seq[Value[_]],
    cwd: ScopePath
  ): Either[BuildException, BuildOptions] = either {
    val extraJars: Seq[Either[BuildException, os.Path]] = DirectiveUtil.stringValues(values).map {
      case (p, pos) =>
        val root = Directive.osRoot(cwd, Some(pos))
        // FIXME Handle malformed paths here
        root.map(os.Path(p, _))
    }

    val errors = extraJars.collect {
      case Left(error) => error
    }

    val res =
      if (errors.nonEmpty) Left(CompositeBuildException(errors))
      else Right(extraJars.collect {
        case Right(value) => value
      })

    BuildOptions(
      classPathOptions = ClassPathOptions(
        extraClassPath = value(res)
      )
    )
  }
}
