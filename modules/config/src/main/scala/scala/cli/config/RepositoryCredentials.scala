package scala.cli.config

final case class RepositoryCredentials(
  host: String = "",
  user: Option[PasswordOption] = None,
  password: Option[PasswordOption] = None,
  realm: Option[String] = None,
  optional: Option[Boolean] = None,
  matchHost: Option[Boolean] = None,
  httpsOnly: Option[Boolean] = None,
  passOnRedirect: Option[Boolean] = None
)
