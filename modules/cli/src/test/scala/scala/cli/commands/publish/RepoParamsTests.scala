package scala.cli.commands.publish

import com.eed3si9n.expecty.Expecty.expect

import java.io.{OutputStream, PrintStream}
import java.util.concurrent.Executors

import scala.build.Logger
import scala.cli.commands.publish.RepoParams.{SonatypeConnectionParams, SonatypeServer}
import scala.cli.internal.CliLogger
import scala.publish.Hooks
import scala.publish.util.EmaRetryParams

class RepoParamsTests extends munit.FunSuite {

  private def sonatypeHooks(
    connection: SonatypeConnectionParams,
    logger: Logger = Logger.nop
  ): Hooks.Sonatype = {
    val es = Executors.newSingleThreadScheduledExecutor()
    try
      RepoParams.centralRepo(
        base = "https://ossrh-staging-api.central.sonatype.com",
        useLegacySnapshots = false,
        connection = connection,
        es = es,
        logger = logger
      ).hooks match {
        case hooks: Hooks.Sonatype => hooks
        case other                 => fail(s"Unexpected hooks: $other")
      }
    finally es.shutdown()
  }

  test("Sonatype connection defaults") {
    val hooks = sonatypeHooks(SonatypeConnectionParams())
    expect(hooks.statusPollParams == Hooks.Sonatype.defaultStatusPollParams)
    expect(hooks.api.requestRetryParams.isEmpty)
    expect(hooks.api.retryOnTimeout == 3)
    expect(hooks.api.stagingRepoRetryParams == EmaRetryParams(3, 10000L, 2.0f))
  }

  test("Sonatype connection settings are passed to the API and hooks") {
    val hooks = sonatypeHooks(SonatypeConnectionParams(
      connectionTimeoutRetries = Some(5),
      connectionRetryWaitTimeMillis = Some(200),
      stagingRepoRetries = Some(4),
      stagingRepoWaitTimeMilis = Some(300),
      stagingRepoStatusPollAttempts = Some(50),
      stagingRepoStatusPollWaitTimeMillis = Some(400)
    ))
    expect(hooks.api.retryOnTimeout == 5)
    expect(hooks.api.requestRetryParams.contains(EmaRetryParams(5, 200L, 2.0f)))
    expect(hooks.api.stagingRepoRetryParams == EmaRetryParams(4, 300L, 2.0f))
    expect(
      hooks.statusPollParams ==
        EmaRetryParams(50, 400L, Hooks.Sonatype.defaultStatusPollParams.factor)
    )
  }

  for (interactive <- Seq(true, false))
    test(s"Sonatype status logger is ${if interactive then "interactive" else "batch"}") {
      val logger = new CliLogger(
        verbosity = 0,
        quiet = false,
        progress = Some(interactive),
        out = new PrintStream(OutputStream.nullOutputStream())
      )
      expect(sonatypeHooks(SonatypeConnectionParams(), logger).batch == !interactive)
    }

  for (
    (repo, expected) <- Seq(
      "central"                         -> Some(SonatypeServer.CentralPortal),
      "central-legacy"                  -> Some(SonatypeServer.Legacy),
      "central-s01"                     -> Some(SonatypeServer.LegacyS01),
      "https://repo.example.com/maven2" -> None
    )
  )
    test(s"Sonatype server of '$repo'") {
      val es = Executors.newSingleThreadScheduledExecutor()
      try {
        val params = RepoParams(
          repo = repo,
          vcsUrlOpt = None,
          workspace = os.pwd,
          ivy2HomeOpt = None,
          isIvy2LocalLike = false,
          es = es,
          logger = Logger.nop
        ).toOption.get
        expect(params.sonatypeServerOpt == expected)
      }
      finally es.shutdown()
    }

  for (
    base <- Seq(
      "https://ossrh-staging-api.central.sonatype.com",
      "https://oss.sonatype.org",
      "https://s01.oss.sonatype.org"
    );
    status <- Seq(401, 403)
  )
    test(s"Sonatype staging profiles error status ($status, $base)") {
      val e = new java.io.FileNotFoundException(
        s"Failed to get $base/service/local/staging/profiles (http status: $status, response: nope)"
      )
      expect(PublishUtils.sonatypeProfilesErrorStatus(e).contains(status))
      expect(PublishUtils.sonatypeProfilesErrorStatus(new Exception(e)).contains(status))
    }

  test("other errors aren't Sonatype staging profiles errors") {
    expect(PublishUtils.sonatypeProfilesErrorStatus(new Exception("boom")).isEmpty)
    expect(PublishUtils.sonatypeProfilesErrorStatus(new Exception(null: String)).isEmpty)
  }
}
