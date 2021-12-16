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

// Adapted from https://github.com/scala/scala/blob/20ac944346a93ba747811e80f8f67a09247cb987/src/compiler/scala/tools/tasty/UnpickleException.scala

package scala.build.tastylib

class UnpickleException(msg: String) extends Exception(msg)
