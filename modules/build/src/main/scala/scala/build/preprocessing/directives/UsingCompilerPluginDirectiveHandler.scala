package scala.build.preprocessing.directives

import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, DependencyFormatError}
import scala.build.options.{BuildOptions, ClassPathOptions, ScalaOptions}
import scala.build.preprocessing.ScopePath
import scala.build.{Position, Positioned}

case object UsingCompilerPluginDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Compiler plugins"
  def description      = "Adds compiler plugins"
  def usage            = "using plugin org:name:ver | using plugins org:name:ver org2:name2:ver2"
  override def usageMd = "`using plugin `_org_`:`name`:`ver"
  override def examples = Seq(
    "using lib org.typelevel:::kind-projector:0.13.2"
  )

  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("plugin" | "plugins", depStr @ _*) =>
        val parsedDeps = depStr.map(parseDependency)
        val errors     = parsedDeps.flatMap(_.left.toOption)

        Some(
          if (errors.nonEmpty) Left(CompositeBuildException(errors))
          else {
            val deps = parsedDeps.flatMap(_.right.toOption)
            Right(BuildOptions(
              scalaOptions = ScalaOptions(
                compilerPlugins = deps.map(dep => Positioned(Seq(directive.position), dep))
              )
            ))
          }
        )
      case _ => None
    }

  private def parseDependency(depStr: String): Either[BuildException, AnyDependency] =
    DependencyParser.parse(depStr)
      .left.map(err => new DependencyFormatError(depStr, err))

  override def keys = Seq("lib", "libs")
  override def handleValues(
    values: Seq[Any],
    cwd: ScopePath,
    positionOpt: Option[Position]
  ): Either[BuildException, BuildOptions] = either {

    val extraDependencies = value {
      DirectiveUtil.stringValues(values)
        .map { dep =>
          // Really necessary? (might already be handled by the coursier-dependency library)
          val dep0 = dep.filter(!_.isSpaceChar)

          parseDependency(dep0)
        }
        .sequence
        .left.map(errors => errors.mkString(", "))
    }

    BuildOptions(
      classPathOptions = ClassPathOptions(
        extraDependencies = extraDependencies
          .map(Positioned.none(_))
      )
    )
  }
}
