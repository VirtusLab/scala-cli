package scala.build.preprocessing.directives

import scala.build.directives._
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Repository")
@DirectiveExamples("//> using repository jitpack")
@DirectiveExamples("//> using repository sonatype:snapshots")
@DirectiveExamples("//> using repository m2Local")
@DirectiveExamples(
  "//> using repository https://maven-central.storage-download.googleapis.com/maven2"
)
@DirectiveUsage(
  "//> using repository _repository_",
  "`//> using repository` _repository_"
)
@DirectiveDescription(Repository.usageMsg)
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class Repository(
  @DirectiveName("repository")
  repositories: List[String] = Nil
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = {
    val buildOpt = BuildOptions(
      classPathOptions = ClassPathOptions(
        extraRepositories = repositories
      )
    )
    Right(buildOpt)
  }
}

object Repository {
  val handler: DirectiveHandler[Repository] = DirectiveHandler.derive

  val usageMsg =
    """Add repositories for dependency resolution.
      |
      |Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository""".stripMargin
}
