package scala.build.preprocessing

import dotty.tools.dotc.ast.{Trees, untpd}
import dotty.tools.dotc.core.Contexts.{Context, ContextBase}
import dotty.tools.dotc.parsing.JavaParsers.OutlineJavaParser
import dotty.tools.dotc.util.SourceFile
import dotty.tools.io.VirtualFile
import dotty.tools.dotc.ast.untpd.{ModuleDef, PackageDef, Tree, TypeDef}
import dotty.tools.dotc.core.Symbols.ClassSymbol
import dotty.tools.dotc.core.{SymbolLoaders, Flags}
import dotty.tools.dotc.ast.untpd.Modifiers
import scala.io.Codec

object JavaParser {
  private def parseOutline(byteContent: Array[Byte]): untpd.Tree = {
    given Context = ContextBase().initialCtx.fresh
    val virtualFile = VirtualFile("placeholder.java", byteContent)
    val sourceFile = SourceFile(virtualFile, Codec.UTF8)
    val outlineParser = OutlineJavaParser(sourceFile)
    outlineParser.parse()
  }

  extension (mdef: untpd.DefTree) {
    def nonPackagePrivate: Boolean = mdef.mods.privateWithin.toTermName.toString != "<empty>"
    def isPrivate: Boolean = mdef.mods.flags.is(Flags.Private)
    def isProtected: Boolean = mdef.mods.flags.is(Flags.Protected)
  }

  def parseRootPublicClassName(byteContent: Array[Byte]): Option[String] = {
    Option(parseOutline(byteContent))
      .flatMap {
        case pd: Trees.PackageDef[_] => Some(pd.stats)
        case _ => None
      }
      .flatMap(_.collectFirst {
        case mdef: ModuleDef if mdef.nonPackagePrivate && !mdef.isPrivate && !mdef.isProtected =>
          mdef.name.toString
      })
  }
}
