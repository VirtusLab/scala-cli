package scala.build.internal

import coursier.jvm.{JavaHome, JvmIndex}

import java.io.IOException
import java.nio.charset.Charset

object OsLibc {

  lazy val isMusl: Option[Boolean] = {

    def tryRun(cmd: String*): Option[os.CommandResult] =
      try {
        val res = os.proc(cmd).call(
          mergeErrIntoOut = true,
          check = false
        )
        Some(res)
      }
      catch {
        case _: IOException =>
          None
      }

    val getconfResOpt = tryRun("getconf", "GNU_LIBC_VERSION")
    if (getconfResOpt.exists(_.exitCode == 0)) Some(false)
    else {

      val lddResOpt = tryRun("ldd", "--version")

      val foundMusl = lddResOpt.exists { lddRes =>
        (lddRes.exitCode == 0 || lddRes.exitCode == 1) &&
        lddRes.out.text(Charset.defaultCharset()).contains("musl")
      }

      if (foundMusl)
        Some(true)
      else {
        val inLib = os.list(os.Path("/lib")).map(_.last)
        if (inLib.exists(_.contains("-linux-gnu"))) Some(false)
        else if (inLib.exists(name => name.contains("libc.musl-") || name.contains("ld-musl-")))
          Some(true)
        else {
          val inUsrSbin = os.list(os.Path("/usr/sbin")).map(_.last)
          if (inUsrSbin.exists(_.contains("glibc"))) Some(false)
          else None
        }
      }
    }
  }

  // FIXME These values should be the default ones in coursier-jvm

  lazy val jvmIndexOs: String = {
    val default = JvmIndex.defaultOs()
    if (default == "linux" && isMusl.getOrElse(false)) "linux-musl"
    else default
  }

  lazy val defaultJvm: String =
    if (jvmIndexOs == "linux-musl") s"${JavaHome.systemId}|liberica" // zulu could work too
    else JavaHome.defaultId

}
