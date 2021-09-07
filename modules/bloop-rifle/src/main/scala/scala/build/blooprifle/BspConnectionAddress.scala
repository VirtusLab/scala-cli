package scala.build.blooprifle

import java.io.File

sealed abstract class BspConnectionAddress extends Product with Serializable

object BspConnectionAddress {
  final case class Tcp(port: Int)                 extends BspConnectionAddress
  final case class UnixDomainSocket(path: File)   extends BspConnectionAddress
  final case class WindowsNamedPipe(name: String) extends BspConnectionAddress
}
