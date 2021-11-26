package scala.build.preprocessing.directives
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.preprocessing.{ScopePath, Scoped}

case object UsingOptionDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Compiler options"
  def description = "Add Scala compiler options"
  def usage       = "using option _option_ | using options _option1_ _option2_ …"
  override def usageMd =
    """`using option `_option_
      |
      |`using options `_option1_ _option2_ …""".stripMargin
  override def examples = Seq(
    "using option -Xasync",
    "using options -Xasync -Xfatal-warnings"
  )

  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("option" | "options", options @ _*) =>
        val opts = BuildOptions(
          scalaOptions = ScalaOptions(
            scalacOptions = options
          )
        )
        Some(Right(opts))
      case _ => None
    }

  override def keys = Seq("option", "options")
  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, (Option[BuildOptions], Seq[Scoped[BuildOptions]])] = {
    val values        = directive.values
    val scalacOptions = DirectiveUtil.stringValues(values, path, cwd)
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalacOptions = scalacOptions.map(_._1)
      )
    )
    Right((Some(options), Seq.empty))
  }
}
