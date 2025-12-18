package scala.build.internal

object Regexes {
  val scala2NightlyRegex         = raw"""2\.(\d+)\.(\d+)-bin-[a-f0-9]*""".r
  val scala3NightlyNicknameRegex = raw"""3\.([0-9]*)\.nightly""".r
  val scala3RcRegex              = raw"""3\.([0-9]*\.[0-9]*-[rR][cC][0-9]+)""".r
  val scala3RcNicknameRegex      = raw"""3\.([0-9]*)\.?[rR][cC]""".r
  val scala3LtsRegex             = raw"""3\.3\.[0-9]+""".r
}
