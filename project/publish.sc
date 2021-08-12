import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version_mill0.9:0.1.1`
import $file.settings, settings.PublishLocalNoFluff

import de.tobiasroeser.mill.vcs.version._
import mill._, scalalib._

import scala.concurrent.duration._

def ghOrg = "VirtuslabRnD"
def ghName = "scala-cli"

trait ScalaCliPublishModule extends PublishModule with PublishLocalNoFluff {
  import mill.scalalib.publish._
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "org.virtuslab.scala-cli",
    url = s"https://github.com/$ghOrg/$ghName",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github(ghOrg, ghName),
    developers = Seq(
      Developer("alexarchambault", "Alex Archambault","https://github.com/alexarchambault")
    )
  )
  private def computePublishVersion(state: VcsState, simple: Boolean): String = {
    if (state.commitsSinceLastTag > 0) {
      if (simple) {
        val versionOrEmpty = state.lastTag
          .filter(_ != "latest")
          .map(_.stripPrefix("v"))
          .flatMap { tag =>
            if (simple) {
              val idx = tag.lastIndexOf(".")
              if (idx >= 0) Some(tag.take(idx + 1) + (tag.drop(idx + 1).toInt + 1).toString + "-SNAPSHOT")
              else None
            } else {
              val idx = tag.indexOf("-")
              if (idx >= 0) Some(tag.take(idx) + "+" + tag.drop(idx + 1) + "-SNAPSHOT")
              else None
            }
          }
          .getOrElse("0.0.1-SNAPSHOT")
        Some(versionOrEmpty)
          .filter(_.nonEmpty)
          .getOrElse(state.format())
      } else {
        val rawVersion = os.proc("git", "describe", "--tags").call().out.text.trim
          .stripPrefix("v")
          .replace("latest", "0.0.0")
        val idx = rawVersion.indexOf("-")
        if (idx >= 0) rawVersion.take(idx) + "+" + rawVersion.drop(idx + 1) + "-SNAPSHOT"
        else rawVersion
      }
    } else
      state
        .lastTag
        .getOrElse(state.format())
        .stripPrefix("v")
  }
  def publishVersion = {
    val isCI = System.getenv("CI") != null
    if (isCI)
      T.persistent {
        val state = VcsVersion.vcsState()
        computePublishVersion(state, simple = false)
      }
    else
      T {
        val state = VcsVersion.vcsState()
        computePublishVersion(state, simple = true)
      }
  }

  def repositories = super.repositories ++ Seq(
    coursier.Repositories.sonatype("snapshots")
  )
}

def publishSonatype(
  data: Seq[PublishModule.PublishData],
  log: mill.api.Logger
): Unit = {

  val credentials = sys.env("SONATYPE_USERNAME") + ":" + sys.env("SONATYPE_PASSWORD")
  val pgpPassword = sys.env("PGP_PASSWORD")
  val timeout = 10.minutes

  val artifacts = data.map {
    case PublishModule.PublishData(a, s) =>
      (s.map { case (p, f) => (p.path, f) }, a)
  }

  val isRelease = {
    val versions = artifacts.map(_._2.version).toSet
    val set = versions.map(!_.endsWith("-SNAPSHOT"))
    assert(set.size == 1, s"Found both snapshot and non-snapshot versions: ${versions.toVector.sorted.mkString(", ")}")
    set.head
  }
  val publisher = new scalalib.publish.SonatypePublisher(
               uri = "https://oss.sonatype.org/service/local",
       snapshotUri = "https://oss.sonatype.org/content/repositories/snapshots",
       credentials = credentials,
            signed = true,
           gpgArgs = Seq("--detach-sign", "--batch=true", "--yes", "--pinentry-mode", "loopback", "--passphrase", pgpPassword, "--armor", "--use-agent"),
       readTimeout = timeout.toMillis.toInt,
    connectTimeout = timeout.toMillis.toInt,
               log = log,
      awaitTimeout = timeout.toMillis.toInt,
    stagingRelease = isRelease
  )

  publisher.publishAll(isRelease, artifacts: _*)
}
