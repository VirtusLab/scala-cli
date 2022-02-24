package scala.build.preprocessing.directives
import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, DependencyFormatError}
import scala.build.options.{BuildOptions, ClassPathOptions, ShadowingSeq}
import scala.build.{Logger, Positioned}

case object UsingDependencyDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Dependency"
  def description = "Add dependencies"
  def usage =
    "//> using lib \"org:name:ver\" | //> using libs \"org:name:ver\", \"org2:name2:ver2\""
  override def usageMd = "`//> using lib \"`_org_`:`name`:`ver\""
  override def examples = Seq(
    "//> using lib \"org.scalatest::scalatest:3.2.10\"",
    "//> using lib \"org.scalameta::munit:0.7.29\"",
    "//> using lib \"tabby:tabby:0.2.3,url=https://github.com/bjornregnell/tabby/releases/download/v0.2.3/tabby_3-0.2.3.jar\""
  )

  private def parseDependency(depStr: String): Either[BuildException, AnyDependency] =
    DependencyParser.parse(depStr)
      .left.map(err => new DependencyFormatError(depStr, err))

  def keys = Seq("lib", "libs")
  def handleValues(
                    scopedDirective: ScopedDirective,
                    logger: Logger
                  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).flatMap { groupedScopedValuesContainer =>
      groupedScopedValuesContainer.scopedStringValues
        .map {
          case ScopedValue(positioned, _) =>
            // Really necessary? (might already be handled by the coursier-dependency library)
            val dep0 = positioned.value.filter(!_.isSpaceChar)

            parseDependency(dep0).map(Positioned(positioned.positions, _))
        }
        .sequence
        .left.map(CompositeBuildException(_))

    }.map { extraDependencies =>

      ProcessedDirective(
        Some(BuildOptions(
          classPathOptions = ClassPathOptions(
            extraDependencies = ShadowingSeq.from(extraDependencies)
          )
        )),
        Seq.empty
      )
    }

}
