package scala.cli.config

trait CredentialsValue {
  def host: String
  def user: Option[PasswordOption]
  def password: Option[PasswordOption]
  def realm: Option[String]
  def httpsOnly: Option[Boolean]
  def asString: String
}

abstract class CredentialsAsJson[T <: CredentialsValue] {
  def user: Option[String]
  def password: Option[String]
  def toCredentialsValue(userOpt: Option[PasswordOption], passwordOpt: Option[PasswordOption]): T
  def credentialsType: String
  private def malformedMessage(valueType: String): String =
    s"Malformed $credentialsType credentials $valueType value (expected 'value:…', or 'file:/path', or 'env:ENV_VAR_NAME')"
  def credentials: Either[::[String], T] = {
    val maybeUser = user
      .map { u =>
        PasswordOption.parse(u) match {
          case Left(error)  => Left(s"${malformedMessage("user")}: $error")
          case Right(value) => Right(Some(value))
        }
      }
      .getOrElse(Right(None))
    val maybePassword = password
      .filter(_.nonEmpty)
      .map { p =>
        PasswordOption.parse(p) match {
          case Left(error)  => Left(s"${malformedMessage("password")}: $error")
          case Right(value) => Right(Some(value))
        }
      }
      .getOrElse(Right(None))
    (maybeUser, maybePassword) match {
      case (Right(userOpt), Right(passwordOpt)) => Right(toCredentialsValue(userOpt, passwordOpt))
      case _                                    =>
        val errors =
          maybeUser.left.toOption.toList ::: maybePassword.left.toOption.toList match {
            case Nil    => sys.error("Cannot happen")
            case h :: t => ::(h, t)
          }
        Left(errors)
    }
  }
}
