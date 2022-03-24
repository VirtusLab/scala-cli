package scala.build

import coursier.util.StringInterpolators.safeModule
import coursier.core.Module

import scala.language.experimental.macros

object CoursierUtils {
  implicit class CSafeModule(val sc: StringContext) extends AnyVal {
    def cmod(args: Any*): Module = macro safeModule
  }
}
