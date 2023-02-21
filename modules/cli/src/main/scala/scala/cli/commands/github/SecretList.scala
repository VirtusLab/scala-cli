package scala.cli.commands.github

import caseapp.core.RemainingArgs
import caseapp.core.help.HelpFormat
import com.github.plokhotnyuk.jsoniter_scala.core.*
import sttp.client3.*

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.cli.commands.ScalaCommand
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.util.ScalaCliSttpBackend
import scala.cli.config.Secret
import scala.cli.errors.GitHubApiError
import scala.cli.util.ArgHelpers.*

object SecretList extends ScalaCommand[SecretListOptions] {

  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED

  override def helpFormat: HelpFormat = super.helpFormat.withPrimaryGroup("Secret")
  override def names = List(
    List("github", "secret", "list"),
    List("gh", "secret", "list")
  )

  def list(
    repoOrg: String,
    repoName: String,
    token: Secret[String],
    backend: SttpBackend[Identity, Any],
    logger: Logger
  ): Either[GitHubApiError, GitHubApi.SecretList] = either {

    // https://docs.github.com/en/rest/reference/actions#list-repository-secrets
    val r = basicRequest
      .get(uri"https://api.github.com/repos/$repoOrg/$repoName/actions/secrets")
      .header("Authorization", s"token ${token.value}")
      .header("Accept", "application/vnd.github.v3+json")
      .send(backend)

    if (r.code.code != 200)
      value(Left(new GitHubApiError(
        s"Unexpected status code ${r.code.code} in response when listing secrets of $repoOrg/$repoName"
      )))

    // FIXME Paging

    val body = r.body match {
      case Left(_) =>
        // should not happen if response code is 200?
        value(Left(new GitHubApiError(
          s"Unexpected missing body in response when listing secrets of $repoOrg/$repoName"
        )))
      case Right(value) => value
    }
    readFromString(body)(GitHubApi.secretListCodec)
  }

  override def runCommand(
    options: SecretListOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
    val backend = ScalaCliSttpBackend.httpURLConnection(logger)

    val list0 = list(
      options.shared.repoOrg,
      options.shared.repoName,
      options.shared.token.get().toConfig,
      backend,
      logger
    ).orExit(logger)

    // FIXME Paging

    System.err.println(s"Found ${list0.total_count} secret(s)")
    for (s <- list0.secrets)
      println(s.name)
  }
}
