package build.project.publish
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import $ivy.`org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r`
import build.project.settings
import com.lumidion.sonatype.central.client.core.{PublishingType, SonatypeCredentials}
import settings.{PublishLocalNoFluff, workspaceDirName}
import de.tobiasroeser.mill.vcs.version._
import mill._
import mill.javalib.publish.Artifact
import scalalib._
import org.eclipse.jgit.api.Git

import java.nio.charset.Charset
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

lazy val (ghOrg: String, ghName: String) = {
  def default = ("VirtusLab", "scala-cli")
  val isCI    = System.getenv("CI") != null
  if (isCI) {
    val repos = {
      var git: Git = null
      try {
        git = Git.open(os.Path(sys.env("MILL_WORKSPACE_ROOT")).toIO)
        git.remoteList().call().asScala.toVector
      }
      finally
        if (git != null)
          git.close()
    }
    val reposByName = repos.flatMap(r =>
      r.getURIs.asScala.headOption.map(_.toASCIIString).toSeq.map((r.getName, _))
    )
    val repoUrlOpt =
      if (reposByName.length == 1)
        reposByName.headOption.map(_._2)
      else {
        val m = reposByName.toMap
        m.get("origin").orElse(m.get("upstream"))
      }
    repoUrlOpt match {
      case Some(url) if url.startsWith("https://github.com/") =>
        url.stripPrefix("https://github.com/").stripSuffix(".git").split("/", 2) match {
          case Array(org, name) => (org, name)
          case Array(_)         =>
            System.err.println(
              s"Warning: git remote URL $url doesn't look like a GitHub URL, using default GitHub org and name"
            )
            default
        }
      case Some(url) =>
        System.err.println(
          s"Warning: git remote URL $url doesn't look like a GitHub URL, using default GitHub org and name"
        )
        default
      case _ =>
        System.err.println("Warning: no git remote found, using default GitHub org and name")
        default
    }
  }
  else
    default
}

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
              Some(
                tag.take(idx + 1) +
                  (tag.drop(idx + 1).replaceAll("-RC.", "").toInt + 1).toString +
                  "-SNAPSHOT"
              )
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
      val rawVersion = os.proc("git", "describe", "--tags").call().out.trim()
        .stripPrefix("v")
        .replace("latest", "0.0.0")
        .replace("nightly", "0.0.0")
      val idx = rawVersion.indexOf("-")
      if (idx >= 0) rawVersion.take(idx) + "-" + rawVersion.drop(idx + 1) + "-SNAPSHOT"
      else rawVersion
    }
  else
    state
      .lastTag
      .getOrElse(state.format())
      .stripPrefix("v")

def finalPublishVersion: Target[String] = {
  val isCI = System.getenv("CI") != null
  if (isCI)
    Task(persistent = true) {
      val state = VcsVersion.vcsState()
      computePublishVersion(state, simple = false)
    }
  else
    Task {
      val state = VcsVersion.vcsState()
      computePublishVersion(state, simple = true)
    }
}

def organization = "org.virtuslab.scala-cli"

trait ScalaCliPublishModule extends SonatypeCentralPublishModule with PublishLocalNoFluff {
  import mill.scalalib.publish._
  def pomSettings: Target[PomSettings] = PomSettings(
    description = artifactName(),
    organization = organization,
    url = s"https://github.com/$ghOrg/$ghName",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github(ghOrg, ghName),
    developers = Seq(
      Developer("alexarchambault", "Alex Archambault", "https://github.com/alexarchambault"),
      Developer("lwronski", "Łukasz Wroński", "https://github.com/lwronski"),
      Developer("romanowski", "Krzysztof Romanowski", "https://github.com/romanowski"),
      Developer("Gedochao", "Piotr Chabelski", "https://github.com/Gedochao"),
      Developer("MaciejG604", "Maciej Gajek", "https://github.com/MaciejG604")
    )
  )
  def publishVersion: Target[String]      = finalPublishVersion()
  override def sourceJar: Target[PathRef] = Task {
    import mill.util.Jvm.createJar
    val allSources0 = allSources().map(_.path).filter(os.exists).toSet
    createJar(
      allSources0 ++ resources().map(_.path).filter(os.exists),
      manifest(),
      (input, relPath) =>
        !allSources0(input) ||
        (!relPath.segments.contains(".scala") && !relPath.segments.contains(workspaceDirName))
    )
  }
}

def publishSonatype(
  data: Seq[PublishModule.PublishData],
  log: mill.api.Logger,
  workspace: os.Path,
  env: Map[String, String],
  bundleName: String
): Unit = {
  val credentials = SonatypeCredentials(
    username = sys.env("SONATYPE_USERNAME"),
    password = sys.env("SONATYPE_PASSWORD")
  )
  val pgpPassword = sys.env("PGP_PASSWORD")
  val timeout     = 10.minutes

  System.err.println("Actual artifacts included in the bundle:")
  val artifacts: Seq[(Seq[(os.Path, String)], Artifact)] = data.map {
    case PublishModule.PublishData(a, s) =>
      System.err.println(s"  ${a.group}:${a.id}:${a.version}")
      (s.map { case (p, f) => (p.path, f) }, a)
  }

  val isRelease: Boolean = {
    val versions: Set[String] = artifacts.map(_._2.version).toSet
    val set: Set[Boolean]     = versions.map(!_.endsWith("-SNAPSHOT"))
    assert(
      set.size == 1,
      s"Found both snapshot and non-snapshot versions: ${versions.toVector.sorted.mkString(", ")}"
    )
    set.head
  }
  System.err.println(s"Is release: $isRelease")
  val publisher = new SonatypeCentralPublisher(
    credentials = credentials,
    gpgArgs = Seq(
      "--detach-sign",
      "--batch=true",
      "--yes",
      "--pinentry-mode",
      "loopback",
      "--passphrase",
      pgpPassword,
      "--armor",
      "--use-agent"
    ),
    readTimeout = timeout.toMillis.toInt,
    connectTimeout = timeout.toMillis.toInt,
    log = log,
    workspace = workspace,
    env = env,
    awaitTimeout = timeout.toMillis.toInt
  )
  val publishingType = if (isRelease) PublishingType.AUTOMATIC else PublishingType.USER_MANAGED
  System.err.println(s"Publishing type: $publishingType")
  val finalBundleName = if (bundleName.nonEmpty) Some(bundleName) else None
  System.err.println(s"Final bundle name: $finalBundleName")
  publisher.publishAll(
    publishingType = publishingType,
    singleBundleName = finalBundleName,
    artifacts = artifacts: _*
  )
}

// from https://github.com/sbt/sbt-ci-release/blob/35b3d02cc6c247e1bb6c10dd992634aa8b3fe71f/plugin/src/main/scala/com/geirsson/CiReleasePlugin.scala#L33-L39
def isTag: Boolean =
  Option(System.getenv("TRAVIS_TAG")).exists(_.nonEmpty) ||
  Option(System.getenv("CIRCLE_TAG")).exists(_.nonEmpty) ||
  Option(System.getenv("CI_COMMIT_TAG")).exists(_.nonEmpty) ||
  Option(System.getenv("BUILD_SOURCEBRANCH"))
    .orElse(Option(System.getenv("GITHUB_REF")))
    .exists(_.startsWith("refs/tags"))
def shouldPublish = Task(persistent = true) {
  val isSnapshot = finalPublishVersion().endsWith("SNAPSHOT")
  // not a snapshot, not a tag: let the tag job do the publishing
  isSnapshot || isTag
}
def setShouldPublish() = Task.Command {
  val envFile = System.getenv("GITHUB_ENV")
  if (envFile == null)
    sys.error("GITHUB_ENV not set")
  val charSet = Charset.defaultCharset()
  val nl      = System.lineSeparator()
  os.write.append(
    os.Path(envFile, Task.workspace),
    s"SHOULD_PUBLISH=${shouldPublish()}$nl".getBytes(charSet)
  )
}
