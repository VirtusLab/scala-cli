package scala.build.internal

import java.io.File

sealed abstract class ExternalBinary extends Product with Serializable {
  def command: Seq[String]
}

object ExternalBinary {
  final case class Native(path: os.Path) extends ExternalBinary {
    def command: Seq[String] =
      Seq(path.toString)
  }
  final case class ClassPath(
    java: String,
    classPath: Seq[os.Path],
    mainClass: String
  ) extends ExternalBinary {
    def command: Seq[String] =
      Seq(java, "-cp", classPath.map(_.toString).mkString(File.pathSeparator), mainClass)
  }
}
