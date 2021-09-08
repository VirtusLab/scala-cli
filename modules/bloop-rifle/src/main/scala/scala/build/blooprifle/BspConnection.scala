package scala.build.blooprifle

import java.net.Socket

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future

trait BspConnection {
  def address: String
  def openSocket(period: FiniteDuration, timeout: FiniteDuration): Socket
  def closed: Future[Int]
  def stop(): Unit
}
