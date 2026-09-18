package scala.build.errors

final class UnrecognizedJsEsVersionError(esVersion: String)
    extends BuildException(
      s"""Unrecognized Scala.js ECMA Script version: $esVersion.
         |Expected es5_1 or esYYYY (e.g. es2022), supported by the Scala.js version in use.""".stripMargin
    )
