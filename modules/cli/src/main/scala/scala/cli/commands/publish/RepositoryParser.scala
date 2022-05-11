package scala.cli.commands.publish

// from coursier.internal.SharedRepositoryParser
// delete when coursier.internal.SharedRepositoryParser.repositoryOpt is available for us

import coursier.Repositories
import coursier.core.Repository
import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository

object RepositoryParser {

  def repositoryOpt(s: String): Option[Repository] =
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
    else if (s.startsWith("bintray-ivy:"))
      Some(Repositories.bintrayIvy(s.stripPrefix("bintray-ivy:")))
    else if (s.startsWith("typesafe:ivy-"))
      Some(Repositories.typesafeIvy(s.stripPrefix("typesafe:ivy-")))
    else if (s.startsWith("typesafe:"))
      Some(Repositories.typesafe(s.stripPrefix("typesafe:")))
    else if (s.startsWith("sbt-maven:"))
      Some(Repositories.sbtMaven(s.stripPrefix("sbt-maven:")))
    else if (s.startsWith("sbt-plugin:"))
      Some(Repositories.sbtPlugin(s.stripPrefix("sbt-plugin:")))
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

  def repository(s: String): Either[String, Repository] =
    repositoryOpt(s) match {
      case Some(repo) => Right(repo)
      case None =>
        if (s.startsWith("ivy:")) {
          val s0     = s.stripPrefix("ivy:")
          val sepIdx = s0.indexOf('|')
          if (sepIdx < 0)
            IvyRepository.parse(s0)
          else {
            val mainPart     = s0.substring(0, sepIdx)
            val metadataPart = s0.substring(sepIdx + 1)
            IvyRepository.parse(mainPart, Some(metadataPart))
          }
        }
        else
          Right(MavenRepository(s))
    }

}
