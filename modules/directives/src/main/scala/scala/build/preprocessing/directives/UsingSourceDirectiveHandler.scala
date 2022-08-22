package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, WrongSourcePathError}
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.{Logger, Positioned}
import scala.util.Try

case object UsingSourceDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Custom sources"
  def description = "Manually add sources to the Scala CLI project"
  def usage       = "`//> using file `_path_ | `//> using files `_path1_, _path2_ …"
  override def usageMd =
    """//> using file hello.sc
      |
      |//> using files Utils.scala, Helper.scala …""".stripMargin

  override def examples = Seq(
    "//> using file \"utils.scala\""
  )

  def keys                  = Seq("file", "files")
  override def isRestricted = false
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = either {
    val groupedScopedValuesContainer = value(checkIfValuesAreExpected(scopedDirective))
    val pathSequence = value {
      groupedScopedValuesContainer.scopedStringValues
        .map {
          case ScopedValue(positioned, _) =>
            val eitherRootPathOrBuildException =
              Directive.osRoot(scopedDirective.cwd, positioned.positions.headOption)
            eitherRootPathOrBuildException.flatMap { root =>
              val sourcePath = Try(os.Path(positioned.value, root)).toEither.left.map { ex =>
                new WrongSourcePathError(ex.getLocalizedMessage, ex)
              }
              sourcePath.map(path => Positioned(positioned.positions, path))
            }
        }
        .sequence
        .left.map(CompositeBuildException(_))
    }

    ProcessedDirective(
      Some(BuildOptions(
        internal = InternalOptions(
          extraSourceFiles = pathSequence
        )
      )),
      Seq.empty
    )
  }

}
