package scala.build.bloop

object VersionOps {
  implicit class VersionOps_(lhs: String) {
    def toInt(s: String): Option[Int] =
      try Some(Integer.parseInt(s))
      catch {
        case _: NumberFormatException => None
      }

    def isNewerThan(rhs: String) : Boolean = {
      val lhsInt = lhs.split("[-.]").map(toInt).map(_.getOrElse(-1))
      val rhsInt = rhs.split("[-.]").map(toInt).map(_.getOrElse(-1))
      (0 until lhsInt.size).exists(i => lhsInt(i) > rhsInt(i))
    }
  }
}
