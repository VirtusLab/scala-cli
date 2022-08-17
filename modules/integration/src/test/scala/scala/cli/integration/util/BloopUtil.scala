package scala.cli.integration.util

import coursier.core.Version

import scala.cli.integration.TestUtil
import scala.util.Properties

object BloopUtil {

  def bloopDaemonDir(directoriesCommandOutput: String): os.Path = {
    val dir = directoriesCommandOutput
      .linesIterator
      .map(_.trim)
      .filter(_.startsWith("Bloop daemon directory: "))
      .map(_.stripPrefix("Bloop daemon directory: "))
      .map(os.Path(_, os.pwd))
      .take(1)
      .toList
      .headOption
      .getOrElse {
        sys.error(
          s"Cannot get Bloop daemon directory in 'scala-cli directories' output '$directoriesCommandOutput'"
        )
      }
    if (!os.exists(dir)) {
      os.makeDir.all(dir)
      if (!Properties.isWin)
        os.perms.set(dir, "rwx------")
    }
    dir
  }

  private def bloopOrg(currentBloopVersion: String): String =
    currentBloopVersion.split("[-.]") match {
      case Array(majStr, minStr, patchStr, _*) =>
        import scala.math.Ordering.Implicits.*
        val maj   = majStr.toInt
        val min   = minStr.toInt
        val patch = patchStr.toInt
        val useBloopMainLine =
          Seq(maj, min, patch) < Seq(1, 4, 11) ||
          (Seq(maj, min, patch) == Seq(1, 4, 11) && !currentBloopVersion.endsWith("-SNAPSHOT"))
        if (useBloopMainLine)
          "ch.epfl.scala"
        else
          "io.github.alexarchambault.bleep"
      case _ =>
        "ch.epfl.scala"
    }
  def bloop(
    currentBloopVersion: String,
    bloopDaemonDir: os.Path,
    jvm: Option[String] = None
  ): Seq[String] => os.proc = {

    val bloopOrg0 = bloopOrg(currentBloopVersion)

    // no more bloopgun for the Bloop fork from version 1.5.3-sc-1
    val useScalaCliBloopCommand =
      bloopOrg0 != "ch.epfl.scala" && Version(currentBloopVersion) >= Version("1.5.3-sc-1")

    if (useScalaCliBloopCommand) {
      val jvmArgs = jvm.toList.flatMap(name => Seq("--bloop-java-opt", name))
      args =>
        os.proc(
          TestUtil.cli,
          "bloop",
          jvmArgs,
          "--bloop-version",
          currentBloopVersion.toString,
          "--bloop-daemon-dir",
          bloopDaemonDir,
          args
        )
    }
    else {
      val daemonArgs =
        if (Properties.isWin)
          Seq("--nailgun-server", "127.0.0.1", "--nailgun-port", "8212")
        else
          Seq("--daemon-dir", bloopDaemonDir.toString)
      val jvmArgs = jvm.toList.flatMap(name => Seq("--jvm", name))
      args =>
        os.proc(
          TestUtil.cs,
          "launch",
          jvmArgs,
          s"$bloopOrg0:bloopgun_2.12:$currentBloopVersion",
          "--",
          daemonArgs,
          args
        )
    }
  }
  def killBloop(): Unit = {
    val javaProcesses = os.proc("jps", "-l").call().out.text().linesIterator
    val bloopPidReg   = "(\\d+).*bloop[.]Bloop".r
    val bloopPids = javaProcesses.flatMap { l =>
      l match {
        case bloopPidReg(pid) => Some(pid.toInt)
        case _                => None
      }
    }.toList
    bloopPids.foreach(TestUtil.kill)
  }
}
