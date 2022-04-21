package scala.cli.signing.util

import java.nio.charset.StandardCharsets
import java.util.Base64

object Util {

  private val base64Chars = (('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') ++ Seq('+', '/'))
    .map(_.toByte)
    .toSet

  def maybeDecodeBase64(input: Array[Byte]): Array[Byte] =
    if (input.nonEmpty && input.forall(base64Chars.contains))
      Base64.getDecoder.decode(input)
    else
      input

}
