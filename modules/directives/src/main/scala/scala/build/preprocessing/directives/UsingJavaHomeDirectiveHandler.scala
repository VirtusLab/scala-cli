package scala.build.preprocessing.directives
import scala.build.errors.{BuildException, WrongJavaHomePathError}
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.{Logger, Positioned}
import scala.util.{Failure, Success}

case object UsingJavaHomeDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Java home"
  def description = "Sets Java home used to run your application or tests"
  def usage       = "//> using java-home|javaHome _path_"
  override def usageMd =
    """`//> using java-home `_path_
      |
      |`//> using javaHome `_path_""".stripMargin
  override def examples = Seq(
    "//> using java-home \"/Users/Me/jdks/11\""
  )
  override def scalaSpecificationLevel = SpecificationLevel.SHOULD

  override def getValueNumberBounds(key: String): UsingDirectiveValueNumberBounds =
    UsingDirectiveValueNumberBounds(1, 1)

  def keys = Seq("java-home", "javaHome")
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).flatMap { groupedValuesContainer =>
      val rawHome: Positioned[String] =
        groupedValuesContainer.scopedStringValues
          .head.positioned
      Directive.osRoot(scopedDirective.cwd, rawHome.positions.headOption).flatMap { root =>
        scala.util.Try(os.Path(rawHome.value, root)) match {
          case Failure(exception) =>
            Left(new WrongJavaHomePathError(rawHome.value, exception))
          case Success(homePath) => Right(ProcessedDirective(
              Some(BuildOptions(
                javaOptions = JavaOptions(
                  javaHomeOpt = Some(Positioned(rawHome.positions, homePath))
                )
              )),
              Seq.empty
            ))
        }

      }
    }

}
