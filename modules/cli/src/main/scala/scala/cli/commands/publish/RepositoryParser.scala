package scala.cli.commands.publish

// from coursier.internal.SharedRepositoryParser
// delete when coursier.internal.SharedRepositoryParser.repositoryOpt is available for us
import coursier.core.Repository
import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.{LocalRepositories, Repositories}

object RepositoryParser {

  def repositoryOpt(s: String): Option[MavenRepository] =
    if (s == "central")
      Some(Repositories.central)
    else if (s.startsWith("sonatype:"))
      Some(Repositories.sonatype(s.stripPrefix("sonatype:")))
    else if (s.startsWith("bintray:")) {
      val s0 = s.stripPrefix("bintray:")
      val id =
        if (s.contains("/")) s0
        else s0 + "/maven"

      Some(Repositories.bintray(id))
    }
    else if (s.startsWith("typesafe:"))
      Some(Repositories.typesafe(s.stripPrefix("typesafe:")))
    else if (s.startsWith("sbt-maven:"))
      Some(Repositories.sbtMaven(s.stripPrefix("sbt-maven:")))
    else if (s == "scala-integration" || s == "scala-nightlies")
      Some(Repositories.scalaIntegration)
    else if (s == "jitpack")
      Some(Repositories.jitpack)
    else if (s == "clojars")
      Some(Repositories.clojars)
    else if (s == "jcenter")
      Some(Repositories.jcenter)
    else if (s == "google")
      Some(Repositories.google)
    else if (s == "gcs")
      Some(Repositories.centralGcs)
    else if (s == "gcs-eu")
      Some(Repositories.centralGcsEu)
    else if (s == "gcs-asia")
      Some(Repositories.centralGcsAsia)
    else if (s.startsWith("apache:"))
      Some(Repositories.apache(s.stripPrefix("apache:")))
    else
      None
}
