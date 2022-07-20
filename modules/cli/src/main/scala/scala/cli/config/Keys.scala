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

  val actionableDiagnostics = new Key.BooleanEntry(Seq.empty, "actionable")

  def all = Seq[Key[_]](
    actionableDiagnostics,
    userName,
    userEmail,
    userUrl,
    ghToken,
    pgpSecretKey,
    pgpSecretKeyPassword,
    pgpPublicKey,
    sonatypeUser,
    sonatypePassword
  )

  lazy val map = all.map(e => e.fullName -> e).toMap

}
