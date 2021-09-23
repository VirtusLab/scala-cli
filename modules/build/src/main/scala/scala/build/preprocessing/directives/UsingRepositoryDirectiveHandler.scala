package scala.build.preprocessing.directives

import scala.build.options.{BuildOptions, ClassPathOptions}

case object UsingRepositoryDirectiveHandler extends UsingDirectiveHandler {
  def handle(directive: Directive): Option[Either[String, BuildOptions]] =
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
