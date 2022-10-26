package scala.cli.config

import com.github.plokhotnyuk.jsoniter_scala.core.{Key => _, _}
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.collection.mutable.ListBuffer

object Keys {

  val userName  = new Key.StringEntry(Seq("user"), "name")
  val userEmail = new Key.StringEntry(Seq("user"), "email")
  val userUrl   = new Key.StringEntry(Seq("user"), "url")

  val ghToken = new Key.PasswordEntry(Seq("github"), "token")

  val pgpSecretKey         = new Key.PasswordEntry(Seq("pgp"), "secret-key")
  val pgpSecretKeyPassword = new Key.PasswordEntry(Seq("pgp"), "secret-key-password")
  val pgpPublicKey         = new Key.PasswordEntry(Seq("pgp"), "public-key")

  val sonatypeUser     = new Key.PasswordEntry(Seq("sonatype"), "user")
  val sonatypePassword = new Key.PasswordEntry(Seq("sonatype"), "password")

  val actions = new Key.BooleanEntry(Seq.empty, "actions")

  val interactive = new Key.BooleanEntry(Seq.empty, "interactive")

  val proxyAddress  = new Key.StringEntry(Seq("httpProxy"), "address")
  val proxyUser     = new Key.PasswordEntry(Seq("httpProxy"), "user")
  val proxyPassword = new Key.PasswordEntry(Seq("httpProxy"), "password")

  val repositoryMirrors   = new Key.StringListEntry(Seq("repositories"), "mirrors")
  val defaultRepositories = new Key.StringListEntry(Seq("repositories"), "default")

  // Kept for binary compatibility
  val repositoriesMirrors = repositoryMirrors

  // setting indicating if the global interactive mode was suggested
  val globalInteractiveWasSuggested = new Key.BooleanEntry(Seq.empty, "interactive-was-suggested")

  def all: Seq[Key[_]] = Seq[Key[_]](
    actions,
    defaultRepositories,
    ghToken,
    globalInteractiveWasSuggested,
    interactive,
    pgpPublicKey,
    pgpSecretKey,
    pgpSecretKeyPassword,
    proxyAddress,
    proxyPassword,
    proxyUser,
    repositoryCredentials,
    repositoryMirrors,
    sonatypePassword,
    sonatypeUser,
    userEmail,
    userName,
    userUrl
  )

  lazy val map: Map[String, Key[_]] = all.map(e => e.fullName -> e).toMap

  private final case class RepositoryCredentialsAsJson(
    host: String,
    user: Option[String] = None,
    password: Option[String] = None,
    realm: Option[String] = None,
    optional: Option[Boolean] = None,
    matchHost: Option[Boolean] = None,
    httpsOnly: Option[Boolean] = None,
    passOnRedirect: Option[Boolean] = None
  ) {
    def credentials: Either[::[String], RepositoryCredentials] = {
      def malformedMessage(valueType: String) =
        s"Malformed repository credentials $valueType value (expected 'value:…', or 'file:/path', or 'env:ENV_VAR_NAME')"
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
        case (Right(userOpt), Right(passwordOpt)) =>
          Right(
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
          )
        case _ =>
          val errors =
            (maybeUser.left.toOption.toList ::: maybePassword.left.toOption.toList) match {
              case Nil    => sys.error("Cannot happen")
              case h :: t => ::(h, t)
            }
          Left(errors)
      }
    }
  }

  val repositoryCredentials: Key[List[RepositoryCredentials]] =
    new Key[List[RepositoryCredentials]] {

      private def asJson(credentials: RepositoryCredentials): RepositoryCredentialsAsJson =
        RepositoryCredentialsAsJson(
          credentials.host,
          credentials.user.map(_.asString.value),
          credentials.password.map(_.asString.value),
          credentials.realm,
          credentials.optional,
          credentials.matchHost,
          credentials.httpsOnly,
          credentials.passOnRedirect
        )
      private val codec: JsonValueCodec[List[RepositoryCredentialsAsJson]] =
        JsonCodecMaker.make

      def prefix = Seq("repositories")
      def name   = "credentials"

      def parse(json: Array[Byte]): Either[Key.EntryError, List[RepositoryCredentials]] =
        try {
          val list   = readFromArray(json)(codec).map(_.credentials)
          val errors = list.collect { case Left(errors) => errors }.flatten
          errors match {
            case Nil =>
              Right(list.collect { case Right(v) => v })
            case h :: t =>
              Left(new Key.MalformedEntry(this, ::(h, t)))
          }
        }
        catch {
          case e: JsonReaderException =>
            Left(new Key.JsonReaderError(e))
        }
      def write(value: List[RepositoryCredentials]): Array[Byte] =
        writeToArray(value.map(asJson))(codec)

      def asString(value: List[RepositoryCredentials]): Seq[String] =
        value
          .zipWithIndex
          .map {
            case (cred, idx) =>
              val prefix = s"configRepo$idx"
              val lines  = new ListBuffer[String]
              if (cred.host.nonEmpty)
                lines += s"$prefix.host=${cred.host}"
              for (u <- cred.user)
                lines += s"$prefix.username=${u.asString.value}"
              for (p <- cred.password)
                lines += s"$prefix.password=${p.asString.value}"
              for (r <- cred.realm)
                lines += s"$prefix.realm=$r"
              for (b <- cred.httpsOnly)
                lines += s"$prefix.https-only=$b"
              for (b <- cred.matchHost)
                lines += s"$prefix.auto=$b"
              for (b <- cred.passOnRedirect)
                lines += s"$prefix.pass-on-redirect=$b"
              // seems cred.optional can't be changed from properties…
              lines.map(_ + System.lineSeparator()).mkString
          }
      def fromString(values: Seq[String]): Either[Key.MalformedValue, List[RepositoryCredentials]] =
        Left(new Key.MalformedValue(
          this,
          values,
          Right("Inline credentials not accepted, please manually edit the config file")
        ))
    }
}
