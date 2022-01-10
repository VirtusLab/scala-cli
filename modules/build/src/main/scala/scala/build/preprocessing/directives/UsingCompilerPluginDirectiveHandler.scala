package scala.build.preprocessing.directives
import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, DependencyFormatError}
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.preprocessing.ScopePath
import scala.build.{Logger, Positioned}

case object UsingCompilerPluginDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Compiler plugins"
  def description = "Adds compiler plugins"
  def usage =
    "// using plugin \"org:name:ver\" | // using plugins \"org:name:ver\", \"org2:name2:ver2\""
  override def usageMd = "`using plugin `_org_`:`name`:`ver"
  override def examples = Seq(
    "// using plugin \"org.typelevel:::kind-projector:0.13.2\""
  )

  private def parseDependency(depStr: String): Either[BuildException, AnyDependency] =
    DependencyParser.parse(depStr)
      .left.map(err => new DependencyFormatError(depStr, err))

  override def keys = Seq("plugin", "plugins")
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
        scalaOptions = ScalaOptions(
          compilerPlugins = extraDependencies
        )
      )),
      Seq.empty
    )
  }
}
