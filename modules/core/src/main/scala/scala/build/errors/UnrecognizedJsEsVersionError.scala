package scala.build.errors

import scala.build.Position

final class UnrecognizedJsEsVersionError(esVersion: String, positions: Seq[Position] = Nil)
    extends BuildException(
      s"""Unrecognized Scala.js ECMA Script version: $esVersion.
         |Expected es5_1 or esYYYY (e.g. es2022), supported by the Scala.js version in use.""".stripMargin,
      positions = positions
    )
