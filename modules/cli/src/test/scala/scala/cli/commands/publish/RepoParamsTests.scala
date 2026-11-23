package scala.cli.commands.publish

import com.eed3si9n.expecty.Expecty.expect

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.concurrent.Executors

import scala.cli.internal.CliLogger
import scala.publish.Hooks

class RepoParamsTests extends munit.FunSuite {

  private val centralPortalBase = "https://ossrh-staging-api.central.sonatype.com"
  private val legacyBase        = "https://oss.sonatype.org"
  private val s01LegacyBase     = "https://s01.oss.sonatype.org"

  private def cliLogger(progress: Option[Boolean]): CliLogger =
    new CliLogger(
      verbosity = 0,
      quiet = false,
      progress = progress,
      out = new PrintStream(new ByteArrayOutputStream)
    )

  private def centralRepo(base: String, progress: Boolean = false): RepoParams = {
    val es = Executors.newSingleThreadScheduledExecutor()
    try
      RepoParams.centralRepo(
        base = base,
        useLegacySnapshots = base != centralPortalBase,
        connectionTimeoutRetries = None,
        connectionTimeoutSeconds = None,
        stagingRepoRetries = None,
        stagingRepoWaitTimeMilis = None,
        es = es,
        logger = cliLogger(Some(progress))
      )
    finally es.shutdown()
  }

  private def sonatypeHooksBatch(progress: Boolean): Boolean =
    centralRepo(centralPortalBase, progress).hooks match {
      case s: Hooks.Sonatype => s.batch
      case other             => fail(s"Expected Sonatype hooks, got $other")
    }

  test("Sonatype hooks use the interactive logger when progress output is enabled") {
    expect(!sonatypeHooksBatch(progress = true))
  }

  test("Sonatype hooks use the batch logger when progress output is disabled") {
    expect(sonatypeHooksBatch(progress = false))
  }

  test("only the OSSRH Staging API counts as the Central Portal") {
    expect(centralRepo(centralPortalBase).isCentralPortal)
    expect(!centralRepo(legacyBase).isCentralPortal)
    expect(!centralRepo(s01LegacyBase).isCentralPortal)
    expect(!RepoParams.gitHubRepoFor("org", "name").isCentralPortal)
  }

  private def sonatypeApiError(base: String, path: String, status: Int): String =
    s"Failed to get $base/service/local/$path (http status: $status, response: )"

  private def stagingProfilesError(base: String, status: Int): String =
    sonatypeApiError(base, "staging/profiles", status)

  for {
    base   <- Seq(centralPortalBase, legacyBase, s01LegacyBase)
    status <- Seq(401, 403)
  }
    test(s"staging profiles HTTP $status is recognised for $base") {
      expect(
        RepoParams.sonatypeStagingProfilesHttpStatus(stagingProfilesError(base, status)) ==
          Some(status)
      )
    }

  test("staging profiles errors are recognised when wrapped in another message") {
    val message = s"java.lang.Exception: ${stagingProfilesError(centralPortalBase, 403)}"
    expect(RepoParams.sonatypeStagingProfilesHttpStatus(message) == Some(403))
  }

  test("staging profiles errors from non-Sonatype hosts are not recognised") {
    expect(
      RepoParams.sonatypeStagingProfilesHttpStatus(
        stagingProfilesError("https://nexus.example.com", 403)
      ).isEmpty
    )
  }

  test("other Sonatype API errors are not recognised as staging profiles errors") {
    val message = sonatypeApiError(centralPortalBase, "staging/profiles/123/start", 403)
    expect(RepoParams.sonatypeStagingProfilesHttpStatus(message).isEmpty)
    expect(RepoParams.sonatypeStagingProfilesHttpStatus(null).isEmpty)
  }

  test("default realm for the Central Portal OSSRH Staging API") {
    expect(
      RepoParams.defaultRealm(s"$centralPortalBase/service/local/staging/deploy/maven2") ==
        Some("OSSRH Staging API Service")
    )
  }

  for {
    url <- Seq(
      "https://central.sonatype.com/repository/maven-snapshots",
      s"$legacyBase/service/local/staging/deploy/maven2",
      s"$s01LegacyBase/content/repositories/snapshots"
    )
  }
    test(s"default realm for Nexus-based Sonatype repository $url") {
      expect(RepoParams.defaultRealm(url) == Some("Sonatype Nexus Repository Manager"))
    }

  test("no default realm for non-Sonatype repositories") {
    expect(RepoParams.defaultRealm("https://maven.pkg.github.com/org/name").isEmpty)
    expect(RepoParams.defaultRealm("file:///tmp/repo").isEmpty)
  }
}
