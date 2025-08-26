package scala.build

import coursier.maven.MavenRepository

object SonatypeUtils {
  lazy val snapshotsRepositoryUrl     = "https://central.sonatype.com/repository/maven-snapshots"
  lazy val snapshotsRepository        = MavenRepository(snapshotsRepositoryUrl)
  lazy val scala3NightlyRepositoryUrl = "https://repo.scala-lang.org/artifactory/maven-nightlies"
  lazy val scala3NightlyRepository    = MavenRepository(scala3NightlyRepositoryUrl)
}
