package scala.build.internal

/** Scala version ↔ JDK compatibility ranges.
  *
  * Sourced from https://docs.scala-lang.org/overviews/jdk-compatibility/overview.html
  *
  * Non-stable versions (RC, nightly, custom suffixes) are normalised by stripping everything from
  * the first `-` onward (e.g. `3.7.4-RC1` → `3.7.4`).
  *
  * @param maxRecommendedJdk
  *   highest JDK version Scala is tested with for this line; warn when a newer JDK is used. For
  *   Scala 3.8+, this tracks the latest released JDK and should be bumped when new JDKs ship.
  */
final case class ScalaJdkCompat(minJdk: Int, maxRecommendedJdk: Int)

object ScalaJdkCompat {

  def normalizeScalaVersion(scalaVersion: String): String =
    val dash = scalaVersion.indexOf('-')
    if dash < 0 then scalaVersion
    else scalaVersion.substring(0, dash)

  def forScalaVersion(scalaVersion: String): Option[ScalaJdkCompat] =
    parseVersion(normalizeScalaVersion(scalaVersion)).flatMap(compatFor.tupled)

  private def parseVersion(version: String): Option[(Int, Int, Int)] =
    val parts = version.split('.')
    if parts.length < 2 then None
    else
      for
        major <- parts(0).toIntOption
        minor <- parts(1).toIntOption
        patch = if parts.length >= 3 then parts(2).takeWhile(_.isDigit).toIntOption.getOrElse(0)
        else 0
      yield (major, minor, patch)

  private def patchTable(table: Seq[(Int, Int)])(patch: Int): Int =
    table.reverseIterator.collectFirst { case (threshold, jdk) if patch >= threshold => jdk }
      .getOrElse(table.head._2)

  private val table_2_12 = Seq(0 -> 8, 4 -> 11, 15 -> 17, 18 -> 21, 21 -> 26)
  private val table_2_13 = Seq(0 -> 11, 6 -> 17, 11 -> 21, 17 -> 25, 18 -> 26)
  private val table_3_3  = Seq(0 -> 17, 1 -> 21, 6 -> 25, 8 -> 26)

  private def compatFor(major: Int, minor: Int, patch: Int): Option[ScalaJdkCompat] =
    (major, minor) match
      case (2, 12)          => Some(ScalaJdkCompat(8, patchTable(table_2_12)(patch)))
      case (2, 13)          => Some(ScalaJdkCompat(8, patchTable(table_2_13)(patch)))
      case (3, m) if m >= 8 => Some(ScalaJdkCompat(17, 26))
      case (3, 7)           => Some(ScalaJdkCompat(8, if patch >= 1 then 25 else 21))
      case (3, m) if m >= 4 => Some(ScalaJdkCompat(8, 21))
      case (3, 3)           => Some(ScalaJdkCompat(8, patchTable(table_3_3)(patch)))
      case (3, _)           => Some(ScalaJdkCompat(8, 17))
      case _                => None
}
