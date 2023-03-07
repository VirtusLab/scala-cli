package scala.cli.config

import com.github.plokhotnyuk.jsoniter_scala.core.{Key => _, _}
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.collection.mutable.ListBuffer

object Keys {

  val userName = new Key.StringEntry(
    prefix = Seq("publish", "user"),
    name = "name",
    description = "The 'name' user detail, used for publishing."
  )
  val userEmail = new Key.StringEntry(
    prefix = Seq("publish", "user"),
    name = "email",
    description = "The 'email' user detail, used for publishing."
  )
  val userUrl = new Key.StringEntry(
    prefix = Seq("publish", "user"),
    name = "url",
    description = "The 'url' user detail, used for publishing."
  )

  val ghToken = new Key.PasswordEntry(
    prefix = Seq("github"),
    name = "token",
    description = "GitHub token."
  )

  val pgpSecretKey = new Key.PasswordEntry(
    prefix = Seq("pgp"),
    name = "secret-key",
    description = "The PGP secret key, used for signing."
  )
  val pgpSecretKeyPassword = new Key.PasswordEntry(
    prefix = Seq("pgp"),
    name = "secret-key-password",
    description = "The PGP secret key password, used for signing."
  )
  val pgpPublicKey = new Key.PasswordEntry(
    prefix = Seq("pgp"),
    name = "public-key",
    description = "The PGP public key, used for signing."
  )

  val actions = new Key.BooleanEntry(
    prefix = Seq.empty,
    name = "actions",
    description = "Globally enables actionable diagnostics. Enabled by default."
  )
  val interactive = new Key.BooleanEntry(
    prefix = Seq.empty,
    name = "interactive",
    description = "Globally enables interactive mode (the '--interactive' flag)."
  )
  val power = new Key.BooleanEntry(
    prefix = Seq.empty,
    name = "power",
    description = "Globally enables power mode (the '--power' launcher flag)."
  )

  val suppressDirectivesInMultipleFilesWarning =
    new Key.BooleanEntry(
      prefix = Seq("suppress-warning"),
      name = "directives-in-multiple-files",
      description =
        "Globally suppresses warnings about directives declared in multiple source files."
    )
  val suppressOutdatedDependenciessWarning =
    new Key.BooleanEntry(
      prefix = Seq("suppress-warning"),
      name = "outdated-dependencies-files",
      description = "Globally suppresses warnings about outdated dependencies."
    )

  val proxyAddress = new Key.StringEntry(
    prefix = Seq("httpProxy"),
    name = "address",
    description = "HTTP proxy address."
  )
  val proxyUser = new Key.PasswordEntry(
    prefix = Seq("httpProxy"),
    name = "user",
    description = "HTTP proxy user (used for authentication)."
  )
  val proxyPassword = new Key.PasswordEntry(
    prefix = Seq("httpProxy"),
    name = "password",
    description = "HTTP proxy password (used for authentication)."
  )

  val repositoryMirrors = new Key.StringListEntry(
    prefix = Seq("repositories"),
    name = "mirrors",
    description =
      s"Repository mirrors, syntax: repositories.mirrors maven:*=https://repository.company.com/maven"
  )
  val defaultRepositories = new Key.StringListEntry(
    prefix = Seq("repositories"),
    name = "default",
    description =
      "Default repository, syntax: https://first-repo.company.com https://second-repo.company.com"
  )

  // Kept for binary compatibility
  val repositoriesMirrors = repositoryMirrors

  // setting indicating if the global interactive mode was suggested
  val globalInteractiveWasSuggested = new Key.BooleanEntry(
    prefix = Seq.empty,
    name = "interactive-was-suggested",
    description = "Setting indicating if the global interactive mode was already suggested."
  )

  def all: Seq[Key[_]] = Seq[Key[_]](
    actions,
    defaultRepositories,
    ghToken,
    globalInteractiveWasSuggested,
    interactive,
    suppressDirectivesInMultipleFilesWarning,
    pgpPublicKey,
    pgpSecretKey,
    pgpSecretKeyPassword,
    power,
    proxyAddress,
    proxyPassword,
    proxyUser,
    publishCredentials,
    repositoryCredentials,
    repositoryMirrors,
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
      override val description: String = "Repository credentials, syntax: value:user value:password"

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

  private final case class PublishCredentialsAsJson(
    host: String,
    user: Option[String] = None,
    password: Option[String] = None,
    realm: Option[String] = None,
    httpsOnly: Option[Boolean] = None
  ) {
    def credentials: Either[::[String], PublishCredentials] = {
      val maybeUser = user
        .map { u =>
          PasswordOption.parse(u) match {
            case Left(error) =>
              Left(
                s"Malformed publish credentials user value (expected 'value:…', or 'file:/path', or 'env:ENV_VAR_NAME'): $error"
              )
            case Right(value) => Right(Some(value))
          }
        }
        .getOrElse(Right(None))
      val maybePassword = password
        .filter(_.nonEmpty)
        .map { p =>
          PasswordOption.parse(p) match {
            case Left(error) =>
              Left(
                s"Malformed publish credentials password value (expected 'value:…', or 'file:/path', or 'env:ENV_VAR_NAME'): $error"
              )
            case Right(value) => Right(Some(value))
          }
        }
        .getOrElse(Right(None))
      (maybeUser, maybePassword) match {
        case (Right(userOpt), Right(passwordOpt)) =>
          Right(
            PublishCredentials(
              host = host,
              user = userOpt,
              password = passwordOpt,
              realm = realm,
              httpsOnly = httpsOnly
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

  val publishCredentials: Key[List[PublishCredentials]] = new Key[List[PublishCredentials]] {
    override val description: String =
      "Publishing credentials, syntax: s1.oss.sonatype.org value:user value:password"

    private def asJson(credentials: PublishCredentials): PublishCredentialsAsJson =
      PublishCredentialsAsJson(
        credentials.host,
        credentials.user.map(_.asString.value),
        credentials.password.map(_.asString.value),
        credentials.realm,
        credentials.httpsOnly
      )
    private val codec: JsonValueCodec[List[PublishCredentialsAsJson]] =
      JsonCodecMaker.make

    def prefix = Seq("publish")
    def name   = "credentials"

    def parse(json: Array[Byte]): Either[Key.EntryError, List[PublishCredentials]] =
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
    def write(value: List[PublishCredentials]): Array[Byte] =
      writeToArray(value.map(asJson))(codec)

    def asString(value: List[PublishCredentials]): Seq[String] =
      value.map { cred =>
        val prefix = cred.httpsOnly match {
          case Some(true)  => "https://"
          case Some(false) => "http://"
          case None        => "//"
        }
        // FIXME We're getting secrets and putting them in a non-Secret guarded string here
        val credentialsPart = {
          val realmPart    = cred.realm.map("(" + _ + ")").getOrElse("")
          val userPart     = cred.user.map(_.get().value).getOrElse("")
          val passwordPart = cred.password.map(":" + _.get().value).getOrElse("")
          if (realmPart.nonEmpty || userPart.nonEmpty || passwordPart.nonEmpty)
            realmPart + userPart + passwordPart + "@"
          else
            ""
        }
        prefix + credentialsPart + cred.host
      }
    def fromString(values: Seq[String]): Either[Key.MalformedValue, List[PublishCredentials]] =
      Left(new Key.MalformedValue(
        this,
        values,
        Right("Inline credentials not accepted, please manually edit the config file")
      ))
  }

}
