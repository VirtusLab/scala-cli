package scala.build.options.publish

import scala.build.Positioned
import scala.build.errors.MalformedInputError

sealed abstract class Signer extends Product with Serializable

object Signer {
  case object Gpg          extends Signer
  case object BouncyCastle extends Signer
  case object Nop          extends Signer

  def parse(input: Positioned[String]): Either[MalformedInputError, Signer] =
    input.value match {
      case "gpg"                 => Right(Signer.Gpg)
      case "bc" | "bouncycastle" => Right(Signer.BouncyCastle)
      case "nop" | "none"        => Right(Signer.Nop)
      case _ => Left(new MalformedInputError("signer", input.value, "gpg|bc", input.positions))
    }
}
