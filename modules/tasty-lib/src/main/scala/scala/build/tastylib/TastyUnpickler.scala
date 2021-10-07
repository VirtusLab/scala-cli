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
import scala.build.tastylib.TastyFormat.NameTags._
import scala.build.tastylib.TastyName._
import scala.build.tastylib.TastyReader.Bytes
import scala.collection.mutable

object TastyUnpickler {

  final class NameTable {
    private[this] val names = new mutable.ArrayBuffer[(TastyName, Bytes)]
    def add(name: TastyName, bytes: Bytes): mutable.ArrayBuffer[(TastyName, Bytes)] =
      names += (name -> bytes)
    def apply(ref: NameRef): TastyName = names(ref.index)._1
    def size: Int                      = names.size
    def toSeq: Seq[(TastyName, Bytes)] = names.toArray.toSeq
  }
}

import TastyUnpickler._

private class TastyUnpickler(reader: TastyReader) { self =>
  import reader._

  private[this] val nameTable = new NameTable

  def nameAtRef: NameTable = nameTable

  private def readName(): TastyName = nameTable(readNameRef())

  private def readParamSig(): Signature.ParamSig[ErasedTypeRef] = {
    val ref = readInt()
    if (ref < 0)
      Left(ref.abs)
    else
      Right(ErasedTypeRef(nameTable(NameRef(ref))))
  }

  private def readNameContents(): (TastyName, Bytes) = {
    val initialPos = pos
    val tag        = readByte()
    val length     = readNat()
    val start      = currentAddr
    val end        = start + length
    def debugName(name: TastyName): name.type =
      name
    def readSignedRest(original: TastyName, target: TastyName): TastyName = {
      val result    = ErasedTypeRef(readName())
      val paramsSig = until(end)(readParamSig())
      val sig       = Signature(paramsSig, result)
      debugName(SignedName(original, sig, target))
    }
    val result = tag match {
      case UTF8 =>
        goto(end)
        debugName(SimpleName(new String(bytes.slice(start.index, start.index + length), "UTF-8")))
      case tag @ (QUALIFIED | EXPANDED | EXPANDPREFIX) =>
        val sep = tag match {
          case QUALIFIED    => PathSep
          case EXPANDED     => ExpandedSep
          case EXPANDPREFIX => ExpandPrefixSep
        }
        debugName(QualifiedName(readName(), sep, readName().asSimpleName))
      case UNIQUE =>
        val separator = readName().asSimpleName
        val num       = readNat()
        val originals = until(end)(readName())
        val original  = if (originals.isEmpty) TastyName.Empty else originals.head
        debugName(UniqueName(original, separator, num))
      case DEFAULTGETTER =>
        debugName(DefaultName(readName(), readNat()))
      case TARGETSIGNED =>
        val original = readName()
        val target   = readName()
        readSignedRest(original, target)
      case SIGNED =>
        val original = readName()
        readSignedRest(original, original)
      case OBJECTCLASS =>
        debugName(ObjectName(readName()))
      case BODYRETAINER =>
        debugName(SuffixName(readName(), BodyRetainerSuffix))
      case INLINEACCESSOR | SUPERACCESSOR =>
        val prefix = tag match {
          case INLINEACCESSOR => InlinePrefix
          case SUPERACCESSOR  => SuperPrefix
        }
        debugName(PrefixName(prefix, readName()))
      case _ =>
        val qual = readName()
        sys.error(
          s"at NameRef(${nameTable.size}): name `${qual.debug}` is qualified by unknown tag $tag"
        )
    }
    assert(currentAddr == end, s"bad name ${result.debug} $start $currentAddr $end")
    (result, new Bytes(bytes, initialPos, index(end)))
  }

  def readHeader(): Unit = new TastyHeaderUnpickler(reader).readHeader()

  def readNames(): Bytes = {
    val preambleStart = pos
    val endAddr       = readEnd()
    val preambleEnd   = pos
    val preambleBytes = Bytes(bytes, preambleStart, preambleEnd)
    doUntil(endAddr) {
      val (n, b) = readNameContents()
      nameTable.add(n, b)
    }
    preambleBytes
  }
}
