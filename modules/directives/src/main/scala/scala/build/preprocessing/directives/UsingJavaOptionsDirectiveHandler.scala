package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, JavaOptions, ShadowingSeq}

case object UsingJavaOptionsDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Java options"
  def description = "Add Java options which will be passed when running an application."
  def usage       = "//> using java-opt _options_ | //> using javaOpt _options_"
  override def usageMd =
    """`//> using java-opt `_options_
      |
      |`//> using javaOpt `_options_""".stripMargin
  override def examples = Seq(
    "//> using javaOpt \"-Xmx2g\", \"-Dsomething=a\""
  )
  override def isRestricted = false

  def keys = Seq("javaOpt", "javaOptions", "java-opt", "java-options")
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValuesContainer =>
      val javaOptions = groupedValuesContainer.scopedStringValues
      val options = BuildOptions(
        javaOptions = JavaOptions(
          javaOpts = ShadowingSeq.from(
            javaOptions.map(_.positioned.map(JavaOpt(_)))
          )
        )
      )
      ProcessedDirective(Some(options), Seq.empty)
    }

}
