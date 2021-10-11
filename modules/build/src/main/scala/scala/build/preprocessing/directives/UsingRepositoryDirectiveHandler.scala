package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ClassPathOptions}

case object UsingRepositoryDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Repository"
  def description      = "Adds a repository for dependency resolution"
  def usage            = "using repository _repository_"
  override def usageMd = "`using repository `_repository_"
  override def examples = Seq(
    "using repository jitpack",
    "using repository sonatype:snapshots",
    "using repository https://maven-central.storage-download.googleapis.com/maven2"
  )

  def handle(directive: Directive): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("repository", repo) if repo.nonEmpty =>
        val options = BuildOptions(
          classPathOptions = ClassPathOptions(
            extraRepositories = Seq(repo)
          )
        )
        Some(Right(options))
      case _ =>
        None
    }
}
