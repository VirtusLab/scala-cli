package scala.build.internal

case object CustomCodeWrapper extends CodeWrapper {
  private val userCodeNestingLevel = 1
  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String
  ) = {
    val packageDirective =
      if (pkgName.isEmpty) "" else s"package ${AmmUtil.encodeScalaSourcePath(pkgName)}" + "\n"

    // indentation is important in the generated code, so we don't want scalafmt to touch that
    // format: off
    val top = AmmUtil.normalizeNewlines(s"""$packageDirective

object ${indexedWrapperName.backticked}{\n"""
    )
    val bottom = AmmUtil.normalizeNewlines(s"""\ndef main(args: _root_.scala.Array[String]) = {}
  $extraCode
}
""")
    // format: on

    (top, bottom, userCodeNestingLevel)
  }
}
