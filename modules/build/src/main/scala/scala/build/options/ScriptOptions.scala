package scala.build.options

import scala.build.internal.CodeWrapper

final case class ScriptOptions(
  codeWrapper: Option[CodeWrapper] = None,
) {
  def orElse(other: ScriptOptions): ScriptOptions =
    ScriptOptions(
      codeWrapper = codeWrapper.orElse(other.codeWrapper)
    )
}

object ScriptOptions {
  implicit val hasHashData: HasHashData[ScriptOptions] = HasHashData.derive
}
