package scala.build.blooprifle.internal

import scala.util.Properties

import org.scalasbt.ipcsocket.{UnixDomainSocket, Win32NamedPipeSocket}

import java.net.Socket

// mostly there for GraalVM substitutions (see UnixNamedSocketBuilder / WindowsNamedSocketBuilder)
class NamedSocketBuilder {
  def create(path: String): Socket =
    if (Properties.isWin)
      new Win32NamedPipeSocket(path, true)
    else
      new UnixDomainSocket(path, true)
}
