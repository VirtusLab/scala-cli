package scala.cli

import ammonite.compiler.iface.CodeWrapper
import ammonite.util._
import ammonite.util.Util.{CodeSource, normalizeNewlines}

object CustomCodeWrapper extends CodeWrapper{
  private val userCodeNestingLevel = 1
  def apply(
    code: String,
    source: CodeSource,
    imports: Imports,
    printCode: String,
    indexedWrapperName: Name,
    extraCode: String
  ) = {
    val pkgName = source.pkgName.drop(2)
    val packageDirective = if (pkgName.isEmpty) "" else s"package ${Util.encodeScalaSourcePath(pkgName)}" + "\n"
    val top = normalizeNewlines(s"""$packageDirective$imports

object ${indexedWrapperName.backticked}{\n"""
      )
      val bottom = normalizeNewlines(s"""\ndef main(args: _root_.scala.Array[String]) = { $printCode }
  $extraCode
}
""")

    (top, bottom, userCodeNestingLevel)
  }
}
