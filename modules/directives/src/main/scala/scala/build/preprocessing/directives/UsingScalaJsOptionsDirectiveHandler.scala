package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaJsOptions}
import scala.meta.Mod
import scala.build.internal.ScalaJsLinkerConfig.ModuleKind
import org.checkerframework.checker.units.qual.m

case object ScalaJsDirectiveHandlers
    extends PrefixedDirectiveGroup[ScalaJsOptions](
      "js",
      "Scala.js",
      scala.cli.commands.ScalaJsOptions.help
    ) {

  case object Version extends StringSetting(
        v => ScalaJsOptions(version = Some(v)),
        Seq("3.2.1", "TODO"),
        "version"
      )

  case object Mode extends StringSetting(
        v => ScalaJsOptions(mode = Some(v)),
        Seq("TODO", "TODO"),
        "mode"
      )

  case object ModuleKind extends StringSetting(
        v => ScalaJsOptions(mode = Some(v)),
        Seq("TODO", "TODO"),
        "kind"
      )

  case object CheckIr extends BooleanDirective( // TODO - check keys
        parse = v => ScalaJsOptions(checkIr = Some(v)))

  case object EmitSourceMaps extends BooleanDirective( // TODO - check keys
        parse = v => ScalaJsOptions(emitSourceMaps = v))

  case object SmallModuleForPackage extends StringListSetting(
        parse = v => ScalaJsOptions(smallModuleForPackage = v.toList),
        Seq(Seq("pckA", "pckB"), Seq("packC"))
      )

  case object Dom extends BooleanDirective( // TODO - check keys
        parse = v => ScalaJsOptions(dom = Some(v)))

  case object Header extends StringSetting( // TODO - check keys
        parse = v => ScalaJsOptions(header = Some(v)),
        Seq("TODO", "TODO"),
        usageValue = "header"
      )

  case object AllowBigIntsForLongs extends BooleanDirective( // TODO - check keys
        parse = v => ScalaJsOptions(allowBigIntsForLongs = Some(v)))

  case object AvoidLetsAndConsts extends BooleanDirective( // TODO - check keys
        parse = v => ScalaJsOptions(avoidLetsAndConsts = Some(v)))

  case object AvoidClasses extends BooleanDirective( // TODO - check keys
        parse = v => ScalaJsOptions(avoidClasses = Some(v)))

  case object ModuleSplitStyleStr extends StringSetting( // TODO - check keys
        parse = v => ScalaJsOptions(moduleSplitStyleStr = Some(v)),
        Seq("TODO", "TODO"),
        usageValue = "splitter-style"
      )

  case object EsVersionStr extends StringSetting( // TODO - check keys
        parse = v => ScalaJsOptions(esVersionStr = Some(v)),
        Seq("TODO", "TODO"),
        usageValue = "version"
      )

  def group =
    DirectiveHandlerGroup(
      "Scala Native",
      Seq(
        Version,
        Mode,
        ModuleKind,
        CheckIr,
        EmitSourceMaps,
        SmallModuleForPackage,
        Dom,
        Header,
        AvoidClasses,
        AvoidLetsAndConsts,
        EsVersionStr
      )
    )

  protected def mkBuildOptions(parsed: ScalaJsOptions) =
    BuildOptions(scalaJsOptions = parsed)

}
