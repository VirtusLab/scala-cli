package scala.build.blooprifle

object VersionUtil {
  def jvmRelease(jvmVersion: String): Option[Int] = {
    val jvmReleaseRegex = "(1[.])?(\\d+)".r
    jvmReleaseRegex.findFirstMatchIn(jvmVersion).map(_.group(2)).map(_.toInt)
  }

  def parseBloopAbout(stdoutFromBloopAbout: String): Option[BloopServerRuntimeInfo] = {

    val bloopVersionRegex = "bloop v(.*)\\s".r
    val bloopJvmRegex     = "Running on Java ... v([0-9._A-Za-z]+) [(](.*)[)]".r

    for {
      bloopVersion    <- bloopVersionRegex.findFirstMatchIn(stdoutFromBloopAbout).map(_.group(1))
      bloopJvmVersion <- bloopJvmRegex.findFirstMatchIn(stdoutFromBloopAbout).map(_.group(1))
      javaHome        <- bloopJvmRegex.findFirstMatchIn(stdoutFromBloopAbout).map(_.group(2))
      jvmRelease      <- VersionUtil.jvmRelease(bloopJvmVersion)
    } yield BloopServerRuntimeInfo(
      bloopVersion = BloopVersion(bloopVersion),
      jvmVersion = jvmRelease,
      javaHome = javaHome
    )
  }
}
