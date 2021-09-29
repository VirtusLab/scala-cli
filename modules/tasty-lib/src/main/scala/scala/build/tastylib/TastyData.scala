package scala.build.tastylib

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

import scala.build.tastylib.TastyFormat.NameTags._
import scala.build.tastylib.TastyReader.Bytes

final case class TastyData(
  header: TastyData.Header,
  names: TastyData.Names,
  sections: TastyData.Sections
) {
  def mapNames(f: String => String): TastyData =
    copy(
      names = names.mapNames(f)
    )
}

object TastyData {
  final case class Header(
    id: UUID,
    bytes: Bytes
  )
  final case class Names(
    preambleBytes: Bytes,
    nameAtRef: Seq[(TastyName, Bytes)]
  ) {
    def simpleNames: Seq[String] =
      nameAtRef.collect {
        case (s: TastyName.SimpleName, _) =>
          s.raw
      }
    def mapNames(f: String => String): Names = {
      val updatedNameAtRef = nameAtRef.map {
        case elem @ (s: TastyName.SimpleName, _) =>
          val updatedName = f(s.raw)
          if (updatedName == s.raw) elem
          else {
            val buf = new TastyBuffer(1 + 1 + updatedName.length)
            buf.writeByte(UTF8)
            val strBytes = updatedName.getBytes(StandardCharsets.UTF_8)
            buf.writeNat(strBytes.length)
            buf.writeBytes(strBytes, strBytes.length)
            (TastyName.SimpleName(updatedName), new Bytes(buf.bytes, 0, buf.length))
          }
        case other => other
      }
      val totalLength = updatedNameAtRef.iterator.map(_._2.length).sum
      val preambleBuf = new TastyBuffer(1)
      preambleBuf.writeNat(totalLength)
      Names(Bytes(preambleBuf.bytes, 0, preambleBuf.length), updatedNameAtRef)
    }
  }
  final case class Sections(
    bytes: Bytes
  )

  def read(bytes: Array[Byte]): TastyData = {

    val headerReader = new TastyReader(bytes)
    val id           = new TastyHeaderUnpickler(headerReader).readHeader()
    val header       = Header(id, headerReader.read)

    val nameReader         = headerReader.readerFromCurrentPos
    val pickler            = new TastyUnpickler(nameReader)
    val namesPreambleBytes = pickler.readNames()
    val names              = Names(namesPreambleBytes, pickler.nameAtRef.toSeq)

    val sections = Sections(nameReader.toRead)

    TastyData(header, names, sections)
  }

  def write(data: TastyData): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    data.header.bytes.writeTo(baos)
    data.names.preambleBytes.writeTo(baos)
    for ((_, b) <- data.names.nameAtRef)
      b.writeTo(baos)
    data.sections.bytes.writeTo(baos)
    baos.toByteArray
  }
}
