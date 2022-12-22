package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions, JavaOpt, ShadowingSeq}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Repository")
@DirectiveExamples("//> using repository \"jitpack\"")
@DirectiveExamples("//> using repository \"sonatype:snapshots\"")
@DirectiveExamples(
  "//> using repository \"https://maven-central.storage-download.googleapis.com/maven2\""
)
@DirectiveUsage(
  "//> using repository _repository_",
  "`//> using repository `_repository_"
)
@DirectiveDescription("Add a repository for dependency resolution")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class Repository(
  @DirectiveName("repository")
    repositories: List[String] = Nil
) extends HasBuildOptions {
  // format: on
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
}
