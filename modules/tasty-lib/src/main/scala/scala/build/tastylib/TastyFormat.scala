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

  final val MajorVersion: Int = 28

  object NameTags {
    final val UTF8 = 1
  }
}
