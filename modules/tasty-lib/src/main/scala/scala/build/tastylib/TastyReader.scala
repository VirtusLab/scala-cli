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

// Originally adapted from https://github.com/scala/scala/blob/20ac944346a93ba747811e80f8f67a09247cb987/src/compiler/scala/tools/tasty/TastyReader.scala

package scala.build.tastylib

import java.io.OutputStream

import scala.build.tastylib.TastyBuffer._
import scala.collection.mutable

class TastyReader(val bytes: Array[Byte], val start: Int, val end: Int, val base: Int = 0) {

  def this(bytes: Array[Byte]) = this(bytes, 0, bytes.length)

  private[this] var bp: Int = start

  def pos: Int                  = bp
  def read: TastyReader.Bytes   = TastyReader.Bytes(bytes, start, bp)
  def toRead: TastyReader.Bytes = TastyReader.Bytes(bytes, bp, end)

  def readerFromCurrentPos: TastyReader =
    new TastyReader(bytes, bp, end, base)

  def addr(idx: Int): Addr   = Addr(idx - base)
  def index(addr: Addr): Int = addr.index + base

  def currentAddr: Addr = addr(bp)

  def endAddr: Addr = addr(end)

  def isAtEnd: Boolean = bp == end

  def readByte(): Int = {
    val result = bytes(bp) & 0xff
    bp += 1
    result
  }

  def readNat(): Int = readLongNat().toInt
  def readInt(): Int = readLongInt().toInt

  def readLongNat(): Long = {
    var b = 0L
    var x = 0L
    while ({
      b = bytes(bp)
      x = (x << 7) | (b & 0x7f)
      bp += 1
      (b & 0x80) == 0
    }) ()
    x
  }

  def readLongInt(): Long = {
    var b       = bytes(bp)
    var x: Long = (b << 1).toByte >> 1
    bp += 1
    while ((b & 0x80) == 0) {
      b = bytes(bp)
      x = (x << 7) | (b & 0x7f)
      bp += 1
    }
    x
  }

  def readUncompressedLong(): Long = {
    var x: Long = 0
    for (_ <- 0 to 7)
      x = (x << 8) | (readByte() & 0xff)
    x
  }

  def readNameRef(): NameRef = NameRef(readNat())

  def readEnd(): Addr = addr(readNat() + bp)

  def goto(addr: Addr): Unit =
    bp = index(addr)

  def until[T](end: Addr)(op: => T): List[T] = {
    val buf = new mutable.ListBuffer[T]
    doUntil(end)(buf += op)
    buf.toList
  }

  def doUntil(end: Addr)(op: => Unit): Unit = {
    while (bp < index(end)) op
    assert(bp == index(end))
  }
}

object TastyReader {
  final class Bytes(val buf: Array[Byte], val start: Int, val end: Int) {
    def length: Int = end - start
    def writeTo(os: OutputStream): Unit =
      os.write(buf, start, end - start)
  }
  object Bytes {
    def apply(values: (Array[Byte], Int, Int)): Bytes = {
      val (buf, start, end) = values
      new Bytes(buf, start, end)
    }
  }
}
