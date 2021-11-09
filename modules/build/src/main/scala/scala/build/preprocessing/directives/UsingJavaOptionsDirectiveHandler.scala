package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value

import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.preprocessing.ScopePath

case object UsingJavaOptionsDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Java options"
  def description = "Add Java options"
  def usage       = "using java-opt _options_ | using javaOpt _options_"
  override def usageMd =
    """`using java-opt `_options_
      |
      |`using javaOpt `_options_""".stripMargin
  override def examples = Seq(
    "using javaOpt -Xmx2g -Dsomething=a"
  )

  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("javaOpt" | "java-opt", javaOpts @ _*) =>
        val options = BuildOptions(
          javaOptions = JavaOptions(
            javaOpts = javaOpts.map(o => scala.build.Positioned(List(directive.position), o))
          )
        )
        Some(Right(options))
      case _ =>
        None
    }

  override def keys = Seq("javaOpt", "javaOptions", "java-opt", "java-options")
  override def handleValues(
    values: Seq[Value[_]],
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, BuildOptions] = {
    val javaOpts = DirectiveUtil.stringValues(values, path)
    val options = BuildOptions(
      javaOptions = JavaOptions(
        javaOpts = javaOpts.map(o => scala.build.Positioned(Seq(o._2), o._1))
      )
    )
    Right(options)
  }
}
