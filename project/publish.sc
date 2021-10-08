import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version_mill0.9:0.1.1`
import $file.settings, settings.PublishLocalNoFluff

import de.tobiasroeser.mill.vcs.version._
import mill._, scalalib._

import java.nio.charset.Charset

import scala.concurrent.duration._

def ghOrg  = "Virtuslab"
def ghName = "scala-cli"

private def computePublishVersion(state: VcsState, simple: Boolean): String =
  if (state.commitsSinceLastTag > 0)
    if (simple) {
      val versionOrEmpty = state.lastTag
        .filter(_ != "latest")
        .filter(_ != "nightly")
        .map(_.stripPrefix("v"))
        .flatMap { tag =>
          if (simple) {
            val idx = tag.lastIndexOf(".")
            if (idx >= 0)
              Some(tag.take(idx + 1) + (tag.drop(idx + 1).toInt + 1).toString + "-SNAPSHOT")
            else
              None
          }
          else {
            val idx = tag.indexOf("-")
            if (idx >= 0) Some(tag.take(idx) + "+" + tag.drop(idx + 1) + "-SNAPSHOT")
            else None
          }
        }
        .getOrElse("0.0.1-SNAPSHOT")
      Some(versionOrEmpty)
        .filter(_.nonEmpty)
        .getOrElse(state.format())
    }
    else {
      val rawVersion = os.proc("git", "describe", "--tags").call().out.text().trim
        .stripPrefix("v")
        .replace("latest", "0.0.0")
        .replace("nightly", "0.0.0")
      val idx = rawVersion.indexOf("-")
      if (idx >= 0) rawVersion.take(idx) + "+" + rawVersion.drop(idx + 1) + "-SNAPSHOT"
      else rawVersion
    }
  else
    state
      .lastTag
      .getOrElse(state.format())
      .stripPrefix("v")

def finalPublishVersion = {
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

trait ScalaCliPublishModule extends PublishModule with PublishLocalNoFluff {
  import mill.scalalib.publish._
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "org.virtuslab.scala-cli",
    url = s"https://github.com/$ghOrg/$ghName",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github(ghOrg, ghName),
    developers = Seq(
      Developer("alexarchambault", "Alex Archambault", "https://github.com/alexarchambault")
    )
  )
  def publishVersion =
    finalPublishVersion()
}

def publishSonatype(
  data: Seq[PublishModule.PublishData],
  log: mill.api.Logger
): Unit = {

  val credentials = sys.env("SONATYPE_USERNAME") + ":" + sys.env("SONATYPE_PASSWORD")
  val pgpPassword = sys.env("PGP_PASSWORD")
  val timeout     = 10.minutes

  val artifacts = data.map {
    case PublishModule.PublishData(a, s) =>
      (s.map { case (p, f) => (p.path, f) }, a)
  }

  val isRelease = {
    val versions = artifacts.map(_._2.version).toSet
    val set      = versions.map(!_.endsWith("-SNAPSHOT"))
    assert(
      set.size == 1,
      s"Found both snapshot and non-snapshot versions: ${versions.toVector.sorted.mkString(", ")}"
    )
    set.head
  }
  val publisher = new scalalib.publish.SonatypePublisher(
    uri = "https://oss.sonatype.org/service/local",
    snapshotUri = "https://oss.sonatype.org/content/repositories/snapshots",
    credentials = credentials,
    signed = true,
    // format: off
    gpgArgs = Seq(
      "--detach-sign",
      "--batch=true",
      "--yes",
      "--pinentry-mode", "loopback",
      "--passphrase", pgpPassword,
      "--armor",
      "--use-agent"
    ),
    // format: on
    readTimeout = timeout.toMillis.toInt,
    connectTimeout = timeout.toMillis.toInt,
    log = log,
    awaitTimeout = timeout.toMillis.toInt,
    stagingRelease = isRelease
  )

  publisher.publishAll(isRelease, artifacts: _*)
}

// from https://github.com/sbt/sbt-ci-release/blob/35b3d02cc6c247e1bb6c10dd992634aa8b3fe71f/plugin/src/main/scala/com/geirsson/CiReleasePlugin.scala#L33-L39
def isTag: Boolean =
  Option(System.getenv("TRAVIS_TAG")).exists(_.nonEmpty) ||
  Option(System.getenv("CIRCLE_TAG")).exists(_.nonEmpty) ||
  Option(System.getenv("CI_COMMIT_TAG")).exists(_.nonEmpty) ||
  Option(System.getenv("BUILD_SOURCEBRANCH"))
    .orElse(Option(System.getenv("GITHUB_REF")))
    .exists(_.startsWith("refs/tags"))

def setShouldPublish() = T.command {
  val isSnapshot = finalPublishVersion().endsWith("SNAPSHOT")
  // not a snapshot, not a tag: let the tag job do the publishing
  val shouldPublish = isSnapshot || isTag
  val envFile       = System.getenv("GITHUB_ENV")
  if (envFile == null)
    sys.error("GITHUB_ENV not set")
  val charSet = Charset.defaultCharset()
  val nl      = System.lineSeparator()
  os.write.append(os.Path(envFile, os.pwd), s"SHOULD_PUBLISH=$shouldPublish$nl".getBytes(charSet))
}
