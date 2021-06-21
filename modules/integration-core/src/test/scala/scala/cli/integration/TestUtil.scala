package scala.cli.integration

import java.io.File

import scala.util.Properties

object TestUtil {

  val isNativeCli = System.getenv("IS_NATIVE_SCALA_CLI").contains("true")
  val isCI = System.getenv("CI") != null
  val cli = {
    val path = System.getenv("SCALA_CLI")
    if (isNativeCli)
      Seq(path)
    else
      Seq("java", "-Xmx512m", "-jar", path)
  }

  lazy val canRunJs = !isNativeCli || !Properties.isWin
  lazy val canRunNative = !Properties.isWin

  def fromPath(app: String): Option[String] = {

    val pathExt =
      if (Properties.isWin)
        Option(System.getenv("PATHEXT"))
          .toSeq
          .flatMap(_.split(File.pathSeparator).toSeq)
      else
        Seq("")
    val path = Option(System.getenv("PATH"))
      .toSeq
      .flatMap(_.split(File.pathSeparator))
      .map(new File(_))

    def candidates =
      for {
        dir <- path.iterator
        ext <- pathExt.iterator
      } yield new File(dir, app + ext)

    candidates
      .filter(_.canExecute)
      .toStream
      .headOption
      .map(_.getAbsolutePath)
  }

}
