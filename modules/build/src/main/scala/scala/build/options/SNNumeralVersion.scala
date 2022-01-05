package scala.build.options

case class SNNumeralVersion(major: Int, minor: Int, patch: Int) {

  override def equals(other: Any): Boolean =
    other match {
      case that: SNNumeralVersion =>
        this.major == that.major &&
          this.minor == that.minor &&
          this.patch == that.patch
      case _ => false
    }

  def <(that: SNNumeralVersion): Boolean =
    if (this.major == that.major)
      if (this.minor == that.minor) this.patch < that.patch
      else this.minor < that.minor
    else this.major < that.major

  def <=(that: SNNumeralVersion) = this < that || this == that

  def >(that: SNNumeralVersion) = !(this <= that)

  def >=(that: SNNumeralVersion) = !(this < that)
}

object SNNumeralVersion {
  private val VersionPattern = raw"(\d+)\.(\d+)\.(\d+)(\-.*)?".r

  // tags/suffixes are included or not compared since they usually
  // should offer no feature compatibility improvements
  def parse(v: String): Option[SNNumeralVersion] = v match {
    case VersionPattern(major, minor, patch, _) =>
      Some(SNNumeralVersion(major.toInt, minor.toInt, patch.toInt))
    case _ =>
      None
  }
}
