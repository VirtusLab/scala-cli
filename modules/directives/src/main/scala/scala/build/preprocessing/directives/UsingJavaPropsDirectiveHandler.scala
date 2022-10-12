package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOpt, JavaOptions, ShadowingSeq}

case object UsingJavaPropsDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Java properties"
  def description = "Add Java properties"
  def usage       = "//> using java-prop|javaProp _key=val_"
  override def usageMd =
    """`//> using javaProp `_key=value_
      |`//> using javaProp `_key_""".stripMargin
  override def examples = Seq(
    "//> using javaProp \"foo1=bar\", \"foo2\""
  )

  override def scalaSpecificationLevel = SpecificationLevel.MUST

  def keys = Seq("javaProp")
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValuesContainer =>
      val javaProps = groupedValuesContainer.scopedStringValues

      val javaOpts = javaProps.map {
        case ScopedValue(positioned, _) =>
          positioned.map { v =>
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
      ProcessedDirective(Some(options), Seq.empty)
    }

}
