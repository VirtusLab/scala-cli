package scala.cli.commands.util

import sttp.capabilities.Effect
import sttp.client3._
import sttp.monad.MonadError

import scala.build.Logger
import scala.util.Try

class ScalaCliSttpBackend(
  underlying: SttpBackend[Identity, Any],
  logger: Logger
) extends SttpBackend[Identity, Any] {

  override def send[T, R >: Any with Effect[Identity]](request: Request[T, R]): Response[T] = {
    logger.debug(s"HTTP ${request.method} ${request.uri}")
    if (logger.verbosity >= 3)
      logger.debug(s"request: '${request.show()}'")
    val resp = underlying.send[T, R](request)
    logger.debug(s"HTTP ${request.method} ${request.uri}: HTTP ${resp.code} ${resp.statusText}")
    if (logger.verbosity >= 3) {
      val logResp = request.response match {
        case ResponseAsByteArray =>
          resp.copy(
            body = Try(new String(resp.body.asInstanceOf[Array[Byte]]))
          )
        case _ =>
          resp
      }
      logger.debug(s"response: '${logResp.show()}'")
    }
    resp
  }
  override def close(): Unit =
    underlying.close()
  override def responseMonad: MonadError[Identity] =
    underlying.responseMonad
}

object ScalaCliSttpBackend {
  def httpURLConnection(logger: Logger): ScalaCliSttpBackend =
    new ScalaCliSttpBackend(
      HttpURLConnectionBackend(),
      logger
    )
}
