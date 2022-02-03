package scala.build.preprocessing.directives
import scala.build.errors.BuildException
import scala.build.options.collections.BuildOptionsConverterImplicits._
import scala.build.options.collections.OptionPrefixes
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.preprocessing.ScopePath
import scala.build.{Logger, Positioned}

case object UsingJavaPropsDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Java properties"
  def description = "Add Java properties"
  def usage       = "//> using java-prop|javaProp _key=val_"
  override def usageMd =
    """`//> using javaProp_ `_key=value_
      |`//> using javaProp_ `_key_""".stripMargin
  override def examples = Seq(
    "//> using javaProp \"foo1=bar\", \"foo2\""
  )

  override def keys = Seq("javaProp")
  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val javaProps = DirectiveUtil.stringValues(directive.values, path, cwd)
    val javaOpts = javaProps.map { case (value, position, _) =>
      value.split("=") match {
        case Array(k)    => Positioned(position, s"-D$k")
        case Array(k, v) => Positioned(position, s"-D$k=$v")
      }
    }
    val options = BuildOptions(javaOptions =
      JavaOptions(javaOpts = javaOpts.toStringOptionsList(OptionPrefixes.javaPrefixes))
    )
    Right(ProcessedDirective(Some(options), Seq.empty))
  }
}
