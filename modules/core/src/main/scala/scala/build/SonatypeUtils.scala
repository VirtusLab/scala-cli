package scala.build

import coursier.maven.MavenRepository

object SonatypeUtils {
  lazy val snapshotsRepositoryUrl = "https://central.sonatype.com/repository/maven-snapshots"
  lazy val snapshotsRepository    = MavenRepository(snapshotsRepositoryUrl)
}
