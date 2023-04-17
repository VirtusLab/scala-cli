package scala.cli.config

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import scala.collection.mutable.ListBuffer

final case class RepositoryCredentials(
  host: String = "",
  user: Option[PasswordOption] = None,
  password: Option[PasswordOption] = None,
  realm: Option[String] = None,
  optional: Option[Boolean] = None,
  matchHost: Option[Boolean] = None,
  httpsOnly: Option[Boolean] = None,
  passOnRedirect: Option[Boolean] = None
) extends CredentialsValue {
  def asString: String = {
    val lines = new ListBuffer[String]
    if (host.nonEmpty)
      lines += s"host=$host"
    for (u <- user)
      lines += s"username=${u.asString.value}"
    for (p <- password)
      lines += s"password=${p.asString.value}"
    for (r <- realm)
      lines += s"realm=$r"
    for (b <- httpsOnly)
      lines += s"https-only=$b"
    for (b <- matchHost)
      lines += s"auto=$b"
    for (b <- passOnRedirect)
      lines += s"pass-on-redirect=$b"
    // seems cred.optional can't be changed from properties…
    lines.map(_ + System.lineSeparator()).mkString
  }
}

final case class RepositoryCredentialsAsJson(
  host: String,
  user: Option[String] = None,
  password: Option[String] = None,
  realm: Option[String] = None,
  optional: Option[Boolean] = None,
  matchHost: Option[Boolean] = None,
  httpsOnly: Option[Boolean] = None,
  passOnRedirect: Option[Boolean] = None
) extends CredentialsAsJson[RepositoryCredentials] {
  def credentialsType: String = "repository"
  def toCredentialsValue(
    userOpt: Option[PasswordOption],
    passwordOpt: Option[PasswordOption]
  ): RepositoryCredentials =
    RepositoryCredentials(
      host = host,
      user = userOpt,
      password = passwordOpt,
      realm = realm,
      optional = optional,
      matchHost = matchHost,
      httpsOnly = httpsOnly,
      passOnRedirect = passOnRedirect
    )
}

object RepositoryCredentialsAsJson {
  implicit lazy val listJsonCodec: JsonValueCodec[List[RepositoryCredentialsAsJson]] =
    JsonCodecMaker.make
}
