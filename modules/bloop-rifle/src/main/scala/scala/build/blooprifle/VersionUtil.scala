package scala.build.blooprifle

object VersionUtil {
  def jvmRelease(jvmVersion: String): Option[Int] = {
    val jvmReleaseRegex = "(1[.])?(\\d+)".r
    jvmReleaseRegex.findFirstMatchIn(jvmVersion).map(_.group(2)).map(_.toInt)
  }
}
