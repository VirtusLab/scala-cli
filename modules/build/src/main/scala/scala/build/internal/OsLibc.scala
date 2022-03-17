package scala.build.internal

import coursier.jvm.{JavaHome, JvmIndex}

import java.io.IOException
import java.nio.charset.Charset

import scala.util.Try

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

  private def defaultJvmVersion = "17"

  def baseDefaultJvm(os: String, jvmVersion: String): String = {
    def java17OrHigher = Try(jvmVersion.takeWhile(_.isDigit).toInt)
      .toOption
      .forall(_ >= 17)
    if (os == "linux-musl") s"liberica:$jvmVersion" // zulu could work too
    else if (java17OrHigher) s"temurin:$jvmVersion"
    else s"adopt:$jvmVersion"
  }

  def defaultJvm(os: String): String = {
    val hasEmptyJavaHome = Option(System.getenv("JAVA_HOME")).exists(_.trim.isEmpty)
    val defaultJvm0      = baseDefaultJvm(os, defaultJvmVersion)
    if (hasEmptyJavaHome)
      // Not using the system JVM if JAVA_HOME is set to an empty string
      // (workaround for https://github.com/coursier/coursier/issues/2292)
      defaultJvm0
    else
      s"${JavaHome.systemId}|$defaultJvm0"
  }

}
