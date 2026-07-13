package scala.cli.commands.pgp

import coursier.cache.FileCache
import coursier.util.Task

import scala.build.errors.BuildException
import scala.build.{Logger, options as bo}
import scala.cli.commands.shared.{CoursierOptions, SharedJvmOptions}

private[pgp] trait PgpProxyCapability {
  def createKey(
    pubKey: String,
    secKey: String,
    mail: String,
    quiet: Boolean,
    passwordOpt: Option[String],
    cache: FileCache[Task],
    logger: Logger,
    jvmOptions: SharedJvmOptions,
    coursierOptions: CoursierOptions,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, Int]

  def keyId(
    key: String,
    keyPrintablePath: String,
    cache: FileCache[Task],
    logger: Logger,
    jvmOptions: SharedJvmOptions,
    coursierOptions: CoursierOptions,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, String]
}

object PgpProxySyntax {
  extension (proxy: PgpProxy)
    def createKey(
      pubKey: String,
      secKey: String,
      mail: String,
      quiet: Boolean,
      passwordOpt: Option[String],
      cache: FileCache[Task],
      logger: Logger,
      jvmOptions: SharedJvmOptions,
      coursierOptions: CoursierOptions,
      signingCliOptions: bo.ScalaSigningCliOptions
    ): Either[BuildException, Int] =
      proxy.asInstanceOf[PgpProxyCapability].createKey(
        pubKey,
        secKey,
        mail,
        quiet,
        passwordOpt,
        cache,
        logger,
        jvmOptions,
        coursierOptions,
        signingCliOptions
      )

    def keyId(
      key: String,
      keyPrintablePath: String,
      cache: FileCache[Task],
      logger: Logger,
      jvmOptions: SharedJvmOptions,
      coursierOptions: CoursierOptions,
      signingCliOptions: bo.ScalaSigningCliOptions
    ): Either[BuildException, String] =
      proxy.asInstanceOf[PgpProxyCapability].keyId(
        key,
        keyPrintablePath,
        cache,
        logger,
        jvmOptions,
        coursierOptions,
        signingCliOptions
      )
}
