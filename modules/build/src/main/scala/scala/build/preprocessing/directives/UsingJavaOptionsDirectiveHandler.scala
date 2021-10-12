package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOptions}

case object UsingJavaOptionsDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Java options"
  def description = "Adds Java options"
  def usage       = "using java-opt _options_ | using javaOpt _options_"
  override def usageMd =
    "`using java-opt `_options_ | `using javaOpt `_options_"
  override def examples = Seq(
    "using javaOpt -Xmx2g -Dsomething=a"
  )

  def handle(directive: Directive): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("javaOpt" | "java-opt", javaOpts @ _*) =>
        val options = BuildOptions(
          javaOptions = JavaOptions(
            javaOpts = javaOpts
          )
        )
        Some(Right(options))
      case _ =>
        None
    }

  override def keys = Seq("javaOpt", "javaOptions", "java-opt", "java-options")
  override def handleValues(values: Seq[Any]): Either[BuildException, BuildOptions] = {
    val javaOpts = DirectiveUtil.stringValues(values)
    val options = BuildOptions(
      javaOptions = JavaOptions(
        javaOpts = javaOpts
      )
    )
    Right(options)
  }
}
