package scala.build.tastylib

// Originally adapted from https://github.com/lampepfl/dotty/blob/d09981141ea16a98c654b98ca582b9626bf0ff0b/tasty/src/dotty/tools/tasty/TastyBuffer.scala

object TastyBuffer {

  /** An address pointing to an index in a Tasty buffer's byte array */
  case class Addr(index: Int) extends AnyVal {
    def +(delta: Int): Addr = Addr(this.index + delta)
  }

  /** An address referring to a serialized name */
  case class NameRef(index: Int) extends AnyVal

  /** An array twice the size of given array, with existing elements copied over */
  private def dble(arr: Array[Byte]): Array[Byte] = {
    val arr1 = new Array[Byte](arr.length * 2)
    System.arraycopy(arr, 0, arr1, 0, arr.length)
    arr1
  }

}

/** A byte array buffer that can be filled with bytes or natural numbers in TASTY format, and that
  * supports reading and patching addresses represented as natural numbers.
  */
class TastyBuffer(initialSize: Int) {
  import TastyBuffer._

  /** The current byte array, will be expanded as needed */
  var bytes: Array[Byte] = new Array(initialSize)

  /** The number of bytes written */
  var length: Int = 0

  // -- Output routines --------------------------------------------

  /** Write a byte of data. */
  def writeByte(b: Int): Unit = {
    if (length >= bytes.length)
      bytes = dble(bytes)
    bytes(length) = b.toByte
    length += 1
  }

  /** Write the first `n` bytes of `data`. */
  def writeBytes(data: Array[Byte], n: Int): Unit = {
    while (bytes.length < length + n) bytes = dble(bytes)
    System.arraycopy(data, 0, bytes, length, n)
    length += n
  }

  /** Write a natural number in big endian format, base 128. All but the last digits have bit 0x80
    * set.
    */
  def writeNat(x: Int): Unit =
    writeLongNat(x.toLong & 0x00000000ffffffffL)

  /** Like writeNat, but for longs. Note that the binary representation of LongNat is identical to
    * Nat if the long value is in the range Int.MIN_VALUE to Int.MAX_VALUE.
    */
  def writeLongNat(x: Long): Unit = {
    def writePrefix(x: Long): Unit = {
      val y = x >>> 7
      if (y != 0L) writePrefix(y)
      writeByte((x & 0x7f).toInt)
    }
    val y = x >>> 7
    if (y != 0L) writePrefix(y)
    writeByte(((x & 0x7f) | 0x80).toInt)
  }

  // -- Address handling --------------------------------------------

  /** The byte at given address */
  def getByte(at: Addr): Int = bytes(at.index)

  /** The natural number at address `at` */
  def getNat(at: Addr): Int = getLongNat(at).toInt

  /** The long natural number at address `at` */
  def getLongNat(at: Addr): Long = {
    var b   = 0L
    var x   = 0L
    var idx = at.index
    while ({
      b = bytes(idx)
      x = (x << 7) | (b & 0x7f)
      idx += 1
      (b & 0x80) == 0
    })
      ()
    x
  }

  /** The address referring to the end of data written so far */
  def currentAddr: Addr = Addr(length)
}
