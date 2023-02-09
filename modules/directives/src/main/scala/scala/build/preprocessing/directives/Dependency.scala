package scala.build.preprocessing.directives
import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.directives.*
import scala.build.errors.{BuildException, CompositeBuildException, DependencyFormatError}
import scala.build.options.{BuildOptions, ClassPathOptions, ShadowingSeq}
import scala.build.preprocessing.ScopePath
import scala.build.{Logger, Positioned}
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using dep \"org.scalatest::scalatest:3.2.10\"")
@DirectiveExamples("//> using dep \"org.scalameta::munit:0.7.29\"")
@DirectiveExamples(
  "//> using dep \"tabby:tabby:0.2.3,url=https://github.com/bjornregnell/tabby/releases/download/v0.2.3/tabby_3-0.2.3.jar\""
)
@DirectiveUsage(
  "//> using dep \"org:name:ver\" | //> using deps \"org:name:ver\", \"org2:name2:ver2\"",
  "`//> using dep \"`_org_`:`name`:`ver\""
)
@DirectiveDescription("Add dependencies")
@DirectiveLevel(SpecificationLevel.MUST)
final case class Dependency(
  @DirectiveName("lib")
  @DirectiveName("libs")
  @DirectiveName("deps")
  dep: List[Positioned[String]] = Nil
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = either {
    val maybeDependencies = dep
      .map { posStr =>
        posStr
          .map { str =>
            DependencyParser.parse(str)
              .left.map(err => new DependencyFormatError(str, err))
          }
          .eitherSequence
      }
      .sequence
      .left.map(CompositeBuildException(_))
    val dependencies = value(maybeDependencies)
    BuildOptions(
      classPathOptions = ClassPathOptions(
        extraDependencies = ShadowingSeq.from(dependencies)
      )
    )
  }
}

object Dependency {
  val handler: DirectiveHandler[Dependency] = DirectiveHandler.derive
}
