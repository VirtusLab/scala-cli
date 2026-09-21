package scala.build.errors

final class UnrecognizedJsEsVersionError(
  esVersion: String,
  supportedEsVersions: Seq[String]
) extends BuildException(
      s"""Unrecognized Scala.js ECMA Script version: $esVersion.
         |Available options: ${supportedEsVersions.mkString(", ")}""".stripMargin
    )
