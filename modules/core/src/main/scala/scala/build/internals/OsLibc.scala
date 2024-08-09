package scala.build.internal

import bloop.rifle.VersionUtil.parseJavaVersion
import coursier.jvm.{JavaHome, JvmIndex}

import java.io.IOException
import java.nio.charset.Charset

import scala.build.Os
import scala.util.{Properties, Try}

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

  def baseDefaultJvm(os: String, jvmVersion: String): String = {
    def bloomMinimumJavaOrHigher = Try(jvmVersion.takeWhile(_.isDigit).toInt)
      .toOption
      .forall(_ >= Constants.minimumBloopJavaVersion)
    if (os == "linux-musl") s"liberica:$jvmVersion" // zulu could work too
    else if (bloomMinimumJavaOrHigher) s"temurin:$jvmVersion"
    else if (Os.isArmArchitecture) s"zulu:$jvmVersion" // adopt doesn't support Java 8 on macOS arm
    else s"temurin:$jvmVersion"
  }

  def defaultJvm(os: String): String =
    baseDefaultJvm(os, Constants.defaultJavaVersion.toString)

  def javaVersion(javaCmd: String): Int = {
    val javaVersionOutput = os.proc(javaCmd, "-version").call(
      cwd = os.pwd,
      stdout = os.Pipe,
      stderr = os.Pipe,
      mergeErrIntoOut = true
    ).out.trim()
    parseJavaVersion(javaVersionOutput).getOrElse {
      throw new Exception(s"Could not parse java version from output: $javaVersionOutput")
    }
  }

  def javaHomeVersion(javaHome: os.Path): (Int, String) = {
    val ext     = if (Properties.isWin) ".exe" else ""
    val javaCmd = (javaHome / "bin" / s"java$ext").toString

    (javaVersion(javaCmd), javaCmd)
  }

}
