package scala.build.preprocessing.directives

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

  def handle(directive: Directive): Option[Either[String, BuildOptions]] =
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
}
