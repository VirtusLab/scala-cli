package scala.build.internal

object Regexes {
  val scala2NightlyRegex         = raw"""2\.(\d+)\.(\d+)-bin-[a-f0-9]*""".r
  val scala3NightlyNicknameRegex = raw"""3\.([0-9]*)\.nightly""".r
}
