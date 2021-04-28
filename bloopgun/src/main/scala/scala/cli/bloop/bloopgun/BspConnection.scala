package scala.cli.bloop.bloopgun

import java.net.Socket

import scala.concurrent.Future

trait BspConnection {
  def address: String
  def openSocket(): Socket
  def closed: Future[Int]
  def stop(): Unit
}
