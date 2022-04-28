package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaNativeOptions}
import scala.build.Positioned
import scala.scalanative.build.{GC => NativeGC, Mode => NativeMode}
import scala.util.Try
import java.nio.file.Paths

object ScalaNativeDirectiveHandlers extends PrefixedDirectiveGroup[ScalaNativeOptions](
      "native",
      "Scala Native",
      scala.cli.commands.ScalaNativeOptions.help
    ) {

  case object GC extends StringSetting(
        parse = value => ScalaNativeOptions(gcStr = Some(NativeGC(value).name)),
        // description =
        //   "Set Garbage Collerctor for Scala Native, for example `none`, `boehm` or `immix`.",
        exampleValues = Seq("none", "boehm"),
        usageValue = "gc"
      )

  case object Mode extends StringSetting(
        parse = value => ScalaNativeOptions(modeStr = Some(NativeMode(value).name)),
        // description =
        //   "Set Scala Native linking mode, one of debug [default], release, release-fast, and release-full.",
        exampleValues = Seq("release", "release-fast"),
        usageValue = "mode"
      )

  case object Clang extends StringSetting(
        parse = value => ScalaNativeOptions(clang = Some(Paths.get(value).toString())),
        // description =
        //   "Provide path for clang to use within Scala Native.",
        exampleValues = Seq("/usr/bin/clang", "/usr/bin/clang"),
        usageValue = "path"
      ) {
    override val name = "Location of clang"
  }

  case object ClangPP extends StringSetting(
        parse = value => ScalaNativeOptions(clangpp = Some(Paths.get(value).toString())),
        // description =
        //   "Provide path for clang++ to use within Scala Native.",
        exampleValues = Seq("/usr/bin/clang++", "/usr/bin/clang++"),
        usageValue = "path"
      ) {
    override val name              = "Location of clang++"
    override val keys: Seq[String] = Seq("nativeClangPP", "native-clang-pp")
  }

  case object Version extends StringSetting(
        parse = value => ScalaNativeOptions(version = Some(value)),
        // description =
        //   "Provide custom version of used Scala Native.",
        exampleValues = Seq("0.4.0", "0.4.1"),
        usageValue = "version"
      )

  // Native compile options
  case object CompileOptions extends StringListSetting(
        parse = value => ScalaNativeOptions(compileOptions = value.toList),
        // description =
        //   "Provide custom options Scala Native compilation.",
        exampleValues = Seq(Seq("TODO"), Seq("TODO", "TODO")),
        usageValue = "option",
      ){
        def primaryName = "compile"
      }

  case object LinkingOptions extends StringListSetting(
        parse = value => ScalaNativeOptions(linkingOptions = value.toList),
        // description =
        //   "Provide custom linking options for Scala Native.",
        exampleValues = Seq(Seq("TODO"), Seq("TODO", "TODO")),
        usageValue = "option"
      ){
        def primaryName = "linking"
      }

  def group =
    DirectiveHandlerGroup(
      "Scala Native",
      Seq(GC, Mode, Clang, ClangPP, Version, CompileOptions, LinkingOptions)
    )

  protected def mkBuildOptions(parsed: ScalaNativeOptions) =
    BuildOptions(scalaNativeOptions = parsed)
}
