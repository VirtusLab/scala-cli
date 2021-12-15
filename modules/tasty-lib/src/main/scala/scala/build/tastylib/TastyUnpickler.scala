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

// Originally adapted from https://github.com/scala/scala/blob/20ac944346a93ba747811e80f8f67a09247cb987/src/compiler/scala/tools/nsc/tasty/TastyUnpickler.scala

package scala.build.tastylib

import scala.build.tastylib.TastyBuffer.NameRef
import scala.build.tastylib.TastyFormat.NameTags
import scala.build.tastylib.TastyName._
import scala.build.tastylib.TastyReader.Bytes
import scala.collection.mutable

object TastyUnpickler {

  final class NameTable {
    private[this] val names = new mutable.ArrayBuffer[(Option[TastyName], Bytes)]
    def add(
      name: Option[TastyName],
      bytes: Bytes
    ): mutable.ArrayBuffer[(Option[TastyName], Bytes)] =
      names += (name -> bytes)
    def apply(ref: NameRef): Option[TastyName] = names(ref.index)._1
    def size: Int                              = names.size
    def toSeq: Seq[(Option[TastyName], Bytes)] = names.toArray.toSeq
  }
}

import TastyUnpickler._

private class TastyUnpickler(reader: TastyReader) { self =>

  private[this] val nameTable = new NameTable

  def nameAtRef: NameTable = nameTable

  private def readNameContents(): (Option[TastyName], Bytes) = {
    val initialPos = reader.pos
    val tag        = reader.readByte()
    val length     = reader.readNat()
    val start      = reader.currentAddr
    val end        = start + length
    val result = tag match {
      case NameTags.UTF8 =>
        Some(SimpleName(new String(reader.bytes.slice(start.index, start.index + length), "UTF-8")))
      case _ =>
        None
    }
    reader.goto(end)
    (result, new Bytes(reader.bytes, initialPos, reader.index(end)))
  }

  def readHeader(): Unit = new TastyHeaderUnpickler(reader).readHeader()

  def readNames(): Bytes = {
    val preambleStart = reader.pos
    val endAddr       = reader.readEnd()
    val preambleEnd   = reader.pos
    val preambleBytes = Bytes((reader.bytes, preambleStart, preambleEnd))
    reader.doUntil(endAddr) {
      val (n, b) = readNameContents()
      nameTable.add(n, b)
    }
    preambleBytes
  }
}
