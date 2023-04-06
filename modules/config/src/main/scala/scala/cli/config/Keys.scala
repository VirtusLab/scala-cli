package scala.cli.config

import com.github.plokhotnyuk.jsoniter_scala.core.{Key => _}

import scala.cli.commands.SpecificationLevel

object Keys {
  val userName = new Key.StringEntry(
    prefix = Seq("publish", "user"),
    name = "name",
    specificationLevel = SpecificationLevel.EXPERIMENTAL,
    description = "The 'name' user detail, used for publishing."
  )
  val userEmail = new Key.StringEntry(
    prefix = Seq("publish", "user"),
    name = "email",
    specificationLevel = SpecificationLevel.EXPERIMENTAL,
    description = "The 'email' user detail, used for publishing."
  )
  val userUrl = new Key.StringEntry(
    prefix = Seq("publish", "user"),
    name = "url",
    specificationLevel = SpecificationLevel.EXPERIMENTAL,
    description = "The 'url' user detail, used for publishing."
  )

  val ghToken = new Key.PasswordEntry(
    prefix = Seq("github"),
    name = "token",
    specificationLevel = SpecificationLevel.EXPERIMENTAL,
    description = "GitHub token."
  )

  val pgpSecretKey = new Key.PasswordEntry(
    prefix = Seq("pgp"),
    name = "secret-key",
    specificationLevel = SpecificationLevel.EXPERIMENTAL,
    description = "The PGP secret key, used for signing."
  )
  val pgpSecretKeyPassword = new Key.PasswordEntry(
    prefix = Seq("pgp"),
    name = "secret-key-password",
    specificationLevel = SpecificationLevel.EXPERIMENTAL,
    description = "The PGP secret key password, used for signing."
  )
  val pgpPublicKey = new Key.PasswordEntry(
    prefix = Seq("pgp"),
    name = "public-key",
    specificationLevel = SpecificationLevel.EXPERIMENTAL,
    description = "The PGP public key, used for signing.",
    hidden = true
  )

  val actions = new Key.BooleanEntry(
    prefix = Seq.empty,
    name = "actions",
    specificationLevel = SpecificationLevel.IMPLEMENTATION,
    description = "Globally enables actionable diagnostics. Enabled by default."
  )
  val interactive = new Key.BooleanEntry(
    prefix = Seq.empty,
    name = "interactive",
    specificationLevel = SpecificationLevel.IMPLEMENTATION,
    description = "Globally enables interactive mode (the '--interactive' flag)."
  )
  val power = new Key.BooleanEntry(
    prefix = Seq.empty,
    name = "power",
    specificationLevel = SpecificationLevel.MUST,
    description = "Globally enables power mode (the '--power' launcher flag)."
  )

  val suppressDirectivesInMultipleFilesWarning =
    new Key.BooleanEntry(
      prefix = Seq("suppress-warning"),
      name = "directives-in-multiple-files",
      specificationLevel = SpecificationLevel.IMPLEMENTATION,
      description =
        "Globally suppresses warnings about directives declared in multiple source files."
    )
  val suppressOutdatedDependenciessWarning =
    new Key.BooleanEntry(
      prefix = Seq("suppress-warning"),
      name = "outdated-dependencies-files",
      specificationLevel = SpecificationLevel.IMPLEMENTATION,
      description = "Globally suppresses warnings about outdated dependencies."
    )
  val suppressExperimentalFeatureWarning =
    new Key.BooleanEntry(
      prefix = Seq("suppress-warning"),
      name = "experimental-features",
      specificationLevel = SpecificationLevel.IMPLEMENTATION,
      description = "Globally suppresses warnings about experimental features."
    )

  val proxyAddress = new Key.StringEntry(
    prefix = Seq("httpProxy"),
    name = "address",
    specificationLevel = SpecificationLevel.RESTRICTED,
    description = "HTTP proxy address."
  )
  val proxyUser = new Key.PasswordEntry(
    prefix = Seq("httpProxy"),
    name = "user",
    specificationLevel = SpecificationLevel.RESTRICTED,
    description = "HTTP proxy user (used for authentication)."
  )
  val proxyPassword = new Key.PasswordEntry(
    prefix = Seq("httpProxy"),
    name = "password",
    specificationLevel = SpecificationLevel.RESTRICTED,
    description = "HTTP proxy password (used for authentication)."
  )

  val repositoryMirrors = new Key.StringListEntry(
    prefix = Seq("repositories"),
    name = "mirrors",
    description =
      s"Repository mirrors, syntax: repositories.mirrors maven:*=https://repository.company.com/maven",
    specificationLevel = SpecificationLevel.RESTRICTED
  )
  val defaultRepositories = new Key.StringListEntry(
    prefix = Seq("repositories"),
    name = "default",
    description =
      "Default repository, syntax: https://first-repo.company.com https://second-repo.company.com",
    specificationLevel = SpecificationLevel.RESTRICTED
  )

  // Kept for binary compatibility
  val repositoriesMirrors = repositoryMirrors

  // setting indicating if the global interactive mode was suggested
  val globalInteractiveWasSuggested = new Key.BooleanEntry(
    prefix = Seq.empty,
    name = "interactive-was-suggested",
    specificationLevel = SpecificationLevel.IMPLEMENTATION,
    description = "Setting indicating if the global interactive mode was already suggested.",
    hidden = true
  )

  val repositoryCredentials: Key.RepositoryCredentialsEntry = new Key.RepositoryCredentialsEntry(
    prefix = Seq("repositories"),
    name = "credentials",
    specificationLevel = SpecificationLevel.RESTRICTED,
    description =
      "Repository credentials, syntax: repositoryAddress value:user value:password [realm]"
  )

  val publishCredentials: Key.PublishCredentialsEntry = new Key.PublishCredentialsEntry(
    prefix = Seq("publish"),
    name = "credentials",
    specificationLevel = SpecificationLevel.EXPERIMENTAL,
    description =
      "Publishing credentials, syntax: repositoryAddress value:user value:password [realm]"
  )

  def all: Seq[Key[_]] = Seq[Key[_]](
    actions,
    defaultRepositories,
    ghToken,
    globalInteractiveWasSuggested,
    interactive,
    suppressDirectivesInMultipleFilesWarning,
    suppressOutdatedDependenciessWarning,
    suppressExperimentalFeatureWarning,
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
}
