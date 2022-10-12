package scala.cli.commands.github

import caseapp.core.RemainingArgs
import com.github.plokhotnyuk.jsoniter_scala.core.*
import coursier.cache.ArchiveCache
import sttp.client3.*

import java.nio.charset.StandardCharsets
import java.util.Base64

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.cli.commands.ScalaCommand
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.util.CommonOps.*
import scala.cli.commands.util.ScalaCliSttpBackend
import scala.cli.commands.{ScalaCommand, SpecificationLevel}
import scala.cli.config.{PasswordOption, Secret}
import scala.cli.errors.GitHubApiError

object SecretCreate extends ScalaCommand[SecretCreateOptions] {

  override def hidden                  = false
  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED
  override def names = List(
    List("github", "secret", "create"),
    List("gh", "secret", "create")
  )

  private def parseSecretKv(input: String): (String, Secret[String]) =
    input.split("=", 2) match {
      case Array(name, value) =>
        PasswordOption.parse(value) match {
          case Left(err)  => sys.error(s"Error parsing secret: $err")
          case Right(opt) => name -> opt.get()
        }
      case _ =>
        sys.error(
          s"Malformed secret '$input' (expected name=password, with password either file:path, command:command, or value:value)"
        )
    }

  def publicKey(
    repoOrg: String,
    repoName: String,
    token: Secret[String],
    backend: SttpBackend[Identity, Any],
    logger: Logger
  ): Either[GitHubApiError, GitHubApi.PublicKey] = either {
    // https://docs.github.com/en/rest/reference/actions#get-a-repository-public-key
    val publicKeyResp = basicRequest
      .get(uri"https://api.github.com/repos/$repoOrg/$repoName/actions/secrets/public-key")
      .header("Authorization", s"token ${token.value}")
      .header("Accept", "application/vnd.github.v3+json")
      .send(backend)

    if (publicKeyResp.code.code != 200)
      value(Left(new GitHubApiError(
        s"Error getting public key (code ${publicKeyResp.code}) for $repoOrg/$repoName"
      )))

    val publicKeyRespBody = publicKeyResp.body match {
      case Left(_) =>
        // should not happen if response code is 200?
        value(Left(new GitHubApiError(
          s"Unexpected missing body in response when listing secrets of $repoOrg/$repoName"
        )))
      case Right(value) => value
    }

    logger.debug(s"Public key: $publicKeyRespBody")

    readFromString(publicKeyRespBody)(GitHubApi.publicKeyCodec)
  }

  def createOrUpdate(
    repoOrg: String,
    repoName: String,
    token: Secret[String],
    secretName: String,
    secretValue: Secret[String],
    pubKey: GitHubApi.PublicKey,
    dummy: Boolean,
    printRequest: Boolean,
    backend: SttpBackend[Identity, Any],
    logger: Logger
  ): Either[GitHubApiError, Boolean] = either {

    val secretBytes = secretValue.value.getBytes(StandardCharsets.UTF_8)

    val encryptedValue = libsodiumjni.Sodium.seal(secretBytes, pubKey.decodedKey)

    val content = GitHubApi.EncryptedSecret(
      encrypted_value = Base64.getEncoder().encodeToString(encryptedValue),
      key_id = pubKey.key_id
    )

    // https://docs.github.com/en/rest/reference/actions#create-or-update-a-repository-secret
    val uri =
      uri"https://api.github.com/repos/$repoOrg/$repoName/actions/secrets/$secretName"
    val requestBody = writeToArray(content)(GitHubApi.encryptedSecretCodec)

    if (printRequest)
      System.out.write(requestBody)

    if (dummy) {
      logger.debug(s"Dummy mode - would have sent a request to $uri")
      logger.message(
        s"Dummy mode - NOT uploading secret $secretName to $repoOrg/$repoName"
      )
      false
    }
    else {
      val r = basicRequest
        .put(uri)
        .header("Authorization", s"token ${token.value}")
        .header("Accept", "application/vnd.github.v3+json")
        .body(requestBody)
        .send(backend)

      r.code.code match {
        case 201 =>
          logger.message(s"  created $secretName")
          true
        case 204 =>
          logger.message(s"  updated $secretName")
          false
        case code =>
          value(Left(new GitHubApiError(
            s"Unexpected status code $code in response when creating secret $secretName in $repoOrg/$repoName"
          )))
      }
    }
  }

  override def runCommand(
    options: SecretCreateOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
    val secrets = args.all.map(parseSecretKv)

    val backend = ScalaCliSttpBackend.httpURLConnection(logger)

    val pubKey = options.publicKey.filter(_.trim.nonEmpty) match {
      case Some(path) =>
        val content = os.read.bytes(os.Path(path, os.pwd))
        readFromArray(content)(GitHubApi.publicKeyCodec)
      case None =>
        publicKey(
          options.shared.repoOrg,
          options.shared.repoName,
          options.shared.token.get().toConfig,
          backend,
          logger
        ).orExit(logger)
    }

    val cache        = options.coursier.coursierCache(logger.coursierLogger(""))
    val archiveCache = ArchiveCache().withCache(cache)

    LibSodiumJni.init(cache, archiveCache, logger)

    for ((name, secretValue) <- secrets) {

      logger.debug(s"Secret name: $name")

      createOrUpdate(
        options.shared.repoOrg,
        options.shared.repoName,
        options.shared.token.get().toConfig,
        name,
        secretValue,
        pubKey,
        options.dummy,
        options.printRequest,
        backend,
        logger
      ).orExit(logger)
    }
  }
}
