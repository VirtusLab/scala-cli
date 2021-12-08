package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Position
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.preprocessing.ScopePath

case object UsingJavaHomeDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Java home"
  def description = "Sets Java home used to run your application or tests"
  def usage       = "using java-home|javaHome _path_"
  override def usageMd =
    """`using java-home `_path_ 
      |
      |`using javaHome `_path_""".stripMargin
  override def examples = Seq(
    "using java-home \"/Users/Me/jdks/11\""
  )

  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("java-home" | "javaHome", path) =>
        val res = either {
          val root = value(Directive.osRoot(cwd, Some(directive.position)))
          val home = os.Path(path, root)
          BuildOptions(
            javaOptions = JavaOptions(
              javaHomeOpt = Some(home)
            )
          )
        }
        Some(res)
      case _ => None
    }

  override def keys = Seq("java-home", "javaHome")
  override def handleValues(
    values: Seq[Any],
    cwd: ScopePath,
    positionOpt: Option[Position]
  ): Either[BuildException, BuildOptions] = either {
    val rawHome = value {
      DirectiveUtil.stringValues(values)
        .lastOption
        .toRight("No value passed to javaHome directive")
    }
    val root = value(Directive.osRoot(cwd, positionOpt))
    // FIXME Might throw
    val home = os.Path(rawHome, root)
    BuildOptions(
      javaOptions = JavaOptions(
        javaHomeOpt = Some(home)
      )
    )
  }
}
