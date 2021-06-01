package scala.cli.internal

import ammonite.util.Name
import ammonite.util.Util.{encodeScalaSourcePath, normalizeNewlines}

object CustomCodeWrapper extends CodeWrapper{
  private val userCodeNestingLevel = 1
  def apply(
    code: String,
    pkgName: Seq[Name],
    indexedWrapperName: Name,
    extraCode: String
  ) = {
    val packageDirective = if (pkgName.isEmpty) "" else s"package ${encodeScalaSourcePath(pkgName)}" + "\n"
    val top = normalizeNewlines(s"""$packageDirective

object ${indexedWrapperName.backticked}{\n"""
      )
      val bottom = normalizeNewlines(s"""\ndef main(args: _root_.scala.Array[String]) = {}
  $extraCode
}
""")

    (top, bottom, userCodeNestingLevel)
  }
}
