package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions}

case object UsingRepositoryDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Repository"
  def description      = "Add a repository for dependency resolution"
  def usage            = "//> using repository _repository_"
  override def usageMd = "`//> using repository `_repository_"
  override def examples = Seq(
    "//> using repository \"jitpack\"",
    "//> using repository \"sonatype:snapshots\"",
    "//> using repository \"https://maven-central.storage-download.googleapis.com/maven2\""
  )

  def keys                  = Seq("repository", "repositories")
  override def isRestricted = true
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValues =>
      val exraRepositories = groupedValues.scopedStringValues.map(_.positioned.value)
      val options = BuildOptions(
        classPathOptions = ClassPathOptions(
          extraRepositories = exraRepositories
        )
      )
      ProcessedDirective(Some(options), Seq.empty)
    }

}
