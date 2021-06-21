package scala.build.options

import scala.build.internal.CodeWrapper

final case class ScriptOptions(
  codeWrapper: Option[CodeWrapper] = None,
) {
  def orElse(other: ScriptOptions): ScriptOptions =
    ScriptOptions(
      codeWrapper = codeWrapper.orElse(other.codeWrapper)
    )
  def addHashData(update: String => Unit): Unit = {
    for (wrapper <- codeWrapper)
      // kind of meh to use wrapper.toString here…
      update("codeWrapper=" + wrapper.toString + "\n")
  }
}
