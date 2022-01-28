/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

// Originally adapted from https://github.com/scala/scala/blob/20ac944346a93ba747811e80f8f67a09247cb987/src/compiler/scala/tools/tasty/TastyHeaderUnpickler.scala

package scala.build.tastylib

import java.util.UUID

class TastyHeaderUnpickler(reader: TastyReader) {

  def this(bytes: Array[Byte]) = this(new TastyReader(bytes))

  /** reads and verifies the TASTy version, extracting the UUID */
  def readHeader(): Either[UnpickleException, UUID] = {

    val isTasty = TastyFormat.header.forall { b =>
      val read = reader.readByte()
      read == b
    }

    if (isTasty) {
      val fileMajor = reader.readNat()
      if (fileMajor <= 27) { // old behavior before `tasty-core` 3.0.0-RC1
        reader.readNat()     // fileMinor
        val signature = TastyHeaderUnpickler.signatureString(fileMajor)
        Left(new UnpickleException(signature + TastyHeaderUnpickler.backIncompatAddendum))
      }
      else {
        reader.readNat() // fileMinor
        reader.readNat() // fileExperimental
        val toolingLength = reader.readNat()
        val toolingStart = {
          val start = reader.currentAddr
          val end   = start + toolingLength
          reader.goto(end)
          start
        }

        val validVersion = fileMajor == TastyFormat.MajorVersion

        if (validVersion)
          Right(new UUID(reader.readUncompressedLong(), reader.readUncompressedLong()))
        else {
          val signature      = TastyHeaderUnpickler.signatureString(fileMajor)
          val toolingVersion = new String(reader.bytes, toolingStart.index, toolingLength)
          val producedByAddendum =
            s"\nThe TASTy file was produced by $toolingVersion."
          val msg =
            if (fileMajor < TastyFormat.MajorVersion) TastyHeaderUnpickler.backIncompatAddendum
            else TastyHeaderUnpickler.forwardIncompatAddendum
          Left(new UnpickleException(signature + msg + producedByAddendum))
        }
      }
    }
    else
      Left(new UnpickleException("not a TASTy file"))
  }
}

object TastyHeaderUnpickler {

  private def signatureString(fileMajor: Int) =
    s"""TASTy signature has wrong version.
       | expected: {majorVersion: ${TastyFormat.MajorVersion}}
       | found   : {majorVersion: $fileMajor}
       |
       |""".stripMargin

  private def backIncompatAddendum =
    """This TASTy file was produced by an earlier release that is not supported anymore.
      |Please recompile this TASTy with a later version.""".stripMargin

  private def forwardIncompatAddendum =
    """This TASTy file was produced by a more recent, forwards incompatible release.
      |To read this TASTy file, please upgrade your tooling.""".stripMargin
}
