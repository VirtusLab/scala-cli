package scala.cli.config

final case class PublishCredentials(
  host: String = "",
  user: Option[PasswordOption] = None,
  password: Option[PasswordOption] = None,
  realm: Option[String] = None,
  httpsOnly: Option[Boolean] = None
)
