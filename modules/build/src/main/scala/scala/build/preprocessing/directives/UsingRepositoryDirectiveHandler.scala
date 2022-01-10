package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions}
import scala.build.preprocessing.ScopePath

case object UsingRepositoryDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Repository"
  def description      = "Add a repository for dependency resolution"
  def usage            = "// using repository _repository_"
  override def usageMd = "`// using repository `_repository_"
  override def examples = Seq(
    "// using repository \"jitpack\"",
    "// using repository \"sonatype:snapshots\"",
    "// using repository \"https://maven-central.storage-download.googleapis.com/maven2\""
  )

  override def keys = Seq("repository", "repositories")
  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val values       = directive.values
    val repositories = DirectiveUtil.stringValues(values, path, cwd)
    val options = BuildOptions(
      classPathOptions = ClassPathOptions(
        extraRepositories = repositories.map(_._1)
      )
    )
    Right(ProcessedDirective(Some(options), Seq.empty))
  }
}
