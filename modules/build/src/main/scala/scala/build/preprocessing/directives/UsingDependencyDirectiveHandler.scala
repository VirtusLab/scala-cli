package scala.build.preprocessing.directives
import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, DependencyFormatError}
import scala.build.options.{BuildOptions, ClassPathOptions, ShadowingSeq}
import scala.build.preprocessing.ScopePath
import scala.build.{Logger, Positioned}

case object UsingDependencyDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Dependency"
  def description = "Add dependencies"
  def usage =
    "//> using lib \"org:name:ver\" | //> using libs \"org:name:ver\", \"org2:name2:ver2\""
  override def usageMd = "`//> using lib \"`_org_`:`name`:`ver\""
  override def examples = Seq(
    "//> using lib \"org.scalatest::scalatest:3.2.10\"",
    "//> using lib \"org.scalameta::munit:0.7.29\""
  )

  private def parseDependency(depStr: String): Either[BuildException, AnyDependency] =
    DependencyParser.parse(depStr)
      .left.map(err => new DependencyFormatError(depStr, err))

  override def keys = Seq("lib", "libs")
  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = either {
    val values = directive.values
    val extraDependencies = value {
      DirectiveUtil.stringValues(values, path, cwd)
        .map {
          case (dep, pos, _) =>
            // Really necessary? (might already be handled by the coursier-dependency library)
            val dep0 = dep.filter(!_.isSpaceChar)

            parseDependency(dep0).map(Positioned(Seq(pos), _))
        }
        .sequence
        .left.map(errors => errors.mkString(", "))
    }

    ProcessedDirective(
      Some(BuildOptions(
        classPathOptions = ClassPathOptions(
          extraDependencies = ShadowingSeq(extraDependencies)
        )
      )),
      Seq.empty
    )
  }
}
