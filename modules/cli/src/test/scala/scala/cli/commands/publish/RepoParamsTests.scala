package scala.cli.commands.publish

import com.eed3si9n.expecty.Expecty.expect

import java.util.concurrent.Executors

import scala.build.Logger
import scala.cli.commands.publish.RepoParams.SonatypeConnectionParams
import scala.publish.Hooks
import scala.publish.util.EmaRetryParams

class RepoParamsTests extends munit.FunSuite {

  private def sonatypeHooks(connection: SonatypeConnectionParams): Hooks.Sonatype = {
    val es = Executors.newSingleThreadScheduledExecutor()
    try
      RepoParams.centralRepo(
        base = "https://ossrh-staging-api.central.sonatype.com",
        useLegacySnapshots = false,
        connection = connection,
        es = es,
        logger = Logger.nop
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
}
