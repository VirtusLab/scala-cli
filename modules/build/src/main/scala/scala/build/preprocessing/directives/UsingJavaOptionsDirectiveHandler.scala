package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.preprocessing.ScopePath
import scala.build.options.collections.BuildOptionsConverterImplicits._
import scala.build.options.collections.OptionPrefixes

case object UsingJavaOptionsDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Java options"
  def description = "Add Java options"
  def usage       = "//> using java-opt _options_ | //> using javaOpt _options_"
  override def usageMd =
    """`//> using java-opt `_options_
      |
      |`//> using javaOpt `_options_""".stripMargin
  override def examples = Seq(
    "//> using javaOpt \"-Xmx2g\", \"-Dsomething=a\""
  )

  override def keys = Seq("javaOpt", "javaOptions", "java-opt", "java-options")
  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val javaOpts = DirectiveUtil.stringValues(directive.values, path, cwd)
    val options = BuildOptions(
      javaOptions = JavaOptions(
        javaOpts = javaOpts.map { case (v, pos, _) =>
          scala.build.Positioned(Seq(pos), v)
        }.toStringOptionsList(OptionPrefixes.javaPrefixes)
      )
    )
    Right(ProcessedDirective(Some(options), Seq.empty))
  }
}
