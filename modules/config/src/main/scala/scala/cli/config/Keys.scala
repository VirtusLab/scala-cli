package scala.cli.config

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

  val repositoriesMirrors = new Key.StringListEntry(Seq("repositories"), "mirrors")
  val defaultRepositories = new Key.StringListEntry(Seq("repositories"), "default")

  // setting indicating if the global interactive mode was suggested
  val globalInteractiveWasSuggested = new Key.BooleanEntry(Seq.empty, "interactive-was-suggested")

  def all: Seq[Key[_]] = Seq[Key[_]](
    actions,
    ghToken,
    globalInteractiveWasSuggested,
    interactive,
    pgpPublicKey,
    pgpSecretKey,
    pgpSecretKeyPassword,
    proxyAddress,
    proxyPassword,
    proxyUser,
    sonatypePassword,
    sonatypeUser,
    userEmail,
    userName,
    userUrl
  )

  lazy val map: Map[String, Key[_]] = all.map(e => e.fullName -> e).toMap

}
