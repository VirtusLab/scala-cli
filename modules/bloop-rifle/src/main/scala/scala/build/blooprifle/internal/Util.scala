package scala.build.blooprifle.internal

import java.io.{File, IOException}
import java.net.{ServerSocket, Socket}

import scala.util.Properties

object Util {

  def withSocket[T](f: Socket => T): T = {
    var socket: Socket = null
    try {
      socket = new Socket
      f(socket)
    }
    // format: off
    finally {
      if (socket != null)
        try {
          socket.shutdownInput()
          socket.shutdownOutput()
          socket.close()
        }
        catch { case _: IOException => }
    }
    // format: on
  }

  def devNull: File =
    new File(if (Properties.isWin) "NUL" else "/dev/null")

  def randomPort(): Int = {
    val s    = new ServerSocket(0)
    val port = s.getLocalPort
    s.close()
    port
  }

}
