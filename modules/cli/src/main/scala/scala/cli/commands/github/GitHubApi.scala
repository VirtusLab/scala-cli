package scala.cli.commands.github

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import java.util.Base64

object GitHubApi {

  final case class Secret(
    name: String,
    created_at: String,
    updated_at: String
  )

  final case class SecretList(
    total_count: Int,
    secrets: List[Secret]
  )

  implicit val secretListCodec: JsonValueCodec[SecretList] =
    JsonCodecMaker.make

  final case class PublicKey(
    key_id: String,
    key: String
  ) {
    def decodedKey: Array[Byte] =
      Base64.getDecoder().decode(key)
  }

  implicit val publicKeyCodec: JsonValueCodec[PublicKey] =
    JsonCodecMaker.make

  final case class EncryptedSecret(
    encrypted_value: String,
    key_id: String
  )

  implicit val encryptedSecretCodec: JsonValueCodec[EncryptedSecret] =
    JsonCodecMaker.make

}
