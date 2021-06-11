package scala.cli.internal

object Util {
  def isFullScalaVersion(sv: String): Boolean =
    sv.count(_ == '.') >= 2 && !sv.endsWith(".")
}
