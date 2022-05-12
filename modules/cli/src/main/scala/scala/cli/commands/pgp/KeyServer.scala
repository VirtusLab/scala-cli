package scala.cli.commands.pgp

import sttp.client3._
import sttp.model.Uri

object KeyServer {

  def default =
    // wouldn't https://keyserver.ubuntu.com work as well (https > http)
    uri"http://keyserver.ubuntu.com:11371"

  def allDefaults = Seq(
    default
    // Sonatype sometimes mentions this one when the key wasn't uploaded anywhere,
    // but lookups are too slow there (and often timeout actually)
    // seems https://pgp.mit.edu might work too
    // uri"http://pgp.mit.edu:11371"
  )

  def add(
    pubKey: String,
    keyServer: Uri,
    backend: SttpBackend[Identity, Any]
  ): Either[String, String] = {

    val resp = basicRequest
      .body(Map("keytext" -> pubKey))
      .response(asString)
      .post(keyServer.addPath("pks", "add"))
      .send(backend)

    if (resp.isSuccess)
      Right(resp.body.merge)
    else
      Left(resp.body.merge)
  }

  def check(
    keyId: String,
    keyServer: Uri,
    backend: SttpBackend[Identity, Any]
  ): Either[String, Either[String, String]] = {
    val resp = basicRequest
      .get(keyServer.addPath("pks", "lookup").addParam("op", "get").addParam("search", keyId))
      .response(asString)
      .send(backend)
    if (resp.isSuccess)
      Right(Right(resp.body.merge))
    else if (resp.isClientError)
      Right(Left(resp.body.merge))
    else
      Left(resp.body.merge)
  }

}
