package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, WrongJarPathError}
import scala.build.options.{BuildOptions, ClassPathOptions}
import scala.util.{Failure, Success}

case object UsingCustomJarDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Custom JAR"
  def description = "Manually add JAR(s) to the class path"
  def usage       = "`//> using jar `_path_ | `//> using jars `_path1_, _path2_ …"
  override def usageMd =
    """//> using jar _path_
      |
      |//> using jars _path1_, _path2_ …""".stripMargin

  override def examples = Seq(
    "//> using jar \"/Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar\""
  )
  override def scalaSpecificationLevel = SpecificationLevel.SHOULD

  def keys = Seq("jar", "jars")
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).flatMap { groupedScopedValuesContainer =>
      groupedScopedValuesContainer.scopedStringValues.map {
        case ScopedValue(positioned, _) =>
          val eitherRootPathOrBuildException =
            Directive.osRoot(scopedDirective.cwd, positioned.positions.headOption)
          eitherRootPathOrBuildException.flatMap { root =>
            scala.util.Try(os.Path(positioned.value, root)) match {
              case Failure(exception) => Left(new WrongJarPathError(exception.getLocalizedMessage))
              case Success(jarPath)   => Right(jarPath)
            }
          }
      }
        .sequence
        .left.map(CompositeBuildException(_))

    }.map { pathSequence =>

      ProcessedDirective(
        Some(BuildOptions(
          classPathOptions = ClassPathOptions(
            extraClassPath = pathSequence
          )
        )),
        Seq.empty
      )

    }

}
