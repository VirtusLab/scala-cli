package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, JavaOptions, ShadowingSeq}
import scala.build.preprocessing.ScopePath

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

  def keys = Seq("javaProp")
  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val javaProps = DirectiveUtil.stringValues(directive.values, path, cwd)
    val javaOpts = javaProps.map {
      case (value, _) =>
        value.map { v =>
          v.split("=") match {
            case Array(k)    => JavaOpt(s"-D$k")
            case Array(k, v) => JavaOpt(s"-D$k=$v")
          }
        }
    }
    val options = BuildOptions(
      javaOptions = JavaOptions(
        javaOpts = ShadowingSeq.from(javaOpts)
      )
    )
    Right(ProcessedDirective(Some(options), Seq.empty))
  }
}
