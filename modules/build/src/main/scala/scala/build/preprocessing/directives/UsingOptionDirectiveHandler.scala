package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value

import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.preprocessing.ScopePath

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
    values: Seq[Value[_]],
    cwd: ScopePath
  ): Either[BuildException, BuildOptions] = {
    val scalacOptions = DirectiveUtil.stringValues(values)
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalacOptions = scalacOptions.map(_._1)
      )
    )
    Right(options)
  }
}
