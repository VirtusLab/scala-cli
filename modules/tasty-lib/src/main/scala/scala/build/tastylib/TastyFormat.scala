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

// Originally adapted from https://github.com/scala/scala/blob/20ac944346a93ba747811e80f8f67a09247cb987/src/compiler/scala/tools/tasty/TastyFormat.scala

package scala.build.tastylib

object TastyFormat {

  final val header: Array[Int] = Array(0x5c, 0xa1, 0xab, 0x1f)

  def isVersionCompatible(
    fileMajor: Int,
    fileMinor: Int,
    fileExperimental: Int,
    compilerMajor: Int,
    compilerMinor: Int,
    compilerExperimental: Int
  ): Boolean =
    fileMajor == compilerMajor && {
      if (fileExperimental == compilerExperimental)
        if (compilerExperimental == 0) fileMinor <= compilerMinor
        else fileMinor == compilerMinor
      else
        fileExperimental == 0 && fileMinor < compilerMinor
    }

  object NameTags {
    final val UTF8           = 1
    final val QUALIFIED      = 2
    final val EXPANDED       = 3
    final val EXPANDPREFIX   = 4
    final val UNIQUE         = 10
    final val DEFAULTGETTER  = 11
    final val SUPERACCESSOR  = 20
    final val INLINEACCESSOR = 21
    final val BODYRETAINER   = 22
    final val OBJECTCLASS    = 23
    final val SIGNED         = 63
    final val TARGETSIGNED   = 62
  }
}
