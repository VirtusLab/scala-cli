package scala.cli.config

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

final case class PublishCredentials(
  host: String = "",
  user: Option[PasswordOption] = None,
  password: Option[PasswordOption] = None,
  realm: Option[String] = None,
  httpsOnly: Option[Boolean] = None
) extends CredentialsValue {
  override def asString: String = {
    val prefix = httpsOnly match {
      case Some(true)  => "https://"
      case Some(false) => "http://"
      case None        => "//"
    }
    // FIXME We're getting secrets and putting them in a non-Secret guarded string here
    val credentialsPart = {
      val realmPart    = realm.map("(" + _ + ")").getOrElse("")
      val userPart     = user.map(_.get().value).getOrElse("")
      val passwordPart = password.map(":" + _.get().value).getOrElse("")
      if (realmPart.nonEmpty || userPart.nonEmpty || passwordPart.nonEmpty)
        realmPart + userPart + passwordPart + "@"
      else
        ""
    }
    prefix + credentialsPart + host
  }
}

final case class PublishCredentialsAsJson(
  host: String,
  user: Option[String] = None,
  password: Option[String] = None,
  realm: Option[String] = None,
  httpsOnly: Option[Boolean] = None
) extends CredentialsAsJson[PublishCredentials] {
  def credentialsType: String = "publish"
  def toCredentialsValue(
    userOpt: Option[PasswordOption],
    passwordOpt: Option[PasswordOption]
  ): PublishCredentials =
    PublishCredentials(
      host = host,
      user = userOpt,
      password = passwordOpt,
      realm = realm,
      httpsOnly = httpsOnly
    )
}

object PublishCredentialsAsJson {
  implicit lazy val listJsonCodec: JsonValueCodec[List[PublishCredentialsAsJson]] =
    JsonCodecMaker.make
}
