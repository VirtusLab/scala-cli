package scala.build.preprocessing.directives

import scala.cli.commands.SharedOptions
import scala.build.options.ScalaOptions
import scala.cli.commands.ScalacOptions
import scala.build.options.BuildOptions
import scala.build.options.ShadowingSeq
import scala.build.options.ScalacOpt
import scala.build.Positioned
import scala.tools.nsc.plugins.Plugins

object ScalacDirectiveHandlers
    extends PrefixedDirectiveGroup[ScalaOptions]("", "TODO", SharedOptions.help) {
  def mkBuildOptions(v: ScalaOptions): BuildOptions = BuildOptions(scalaOptions = v)

  def group = DirectiveHandlerGroup("Scala compiler", Seq(Options, Version, Plugins))

  object Options extends BaseStringListSetting {
    override def keys = Seq("option", "options")
    def exampleValues = Seq(Seq("-Xasync"), Seq("-Xasync", "-Xfatal-warnings"))
    def usageValue    = "option"

    def processOption(opts: ::[Positioned[String]])(using Ctx) =
      val opsSeq = ShadowingSeq.from(opts.map(_.map(ScalacOpt.apply)))
      Right(ScalaOptions(scalacOptions = opsSeq))
  }

  object Version extends BaseStringListSetting {
    def processOption(opts: ::[Positioned[String]])(using Ctx) =
      val main :: rest = opts.map(_.value)
      Right(ScalaOptions(scalaVersion = Some(main), extraScalaVersions = rest.toSet))

    override def keys = Seq("scala")
    def exampleValues = Seq(
      Seq("3.0.2"),
      Seq("3"),
      Seq("3.1"),
      Seq("2.13"),
      Seq("2"),
      Seq("2.13.6", "2.12.15")
    )
    def usageValue = "option"
  }

  object Plugins extends BaseStringListSetting {
    def processOption(opts: ::[Positioned[String]])(using Ctx) =
      ClassPathDirectiveHandlers.Libs.parseDependencies(opts).map(plugins =>
        ScalaOptions(compilerPlugins = plugins)
      )

    override def keys = Seq("plugin", "plugins")
    def exampleValues = Seq(
      Seq("org.typelevel:::kind-projector:0.13.2"),
      Seq("<org>:::<name>:<version>", "<org2>:::<name2>:<version2>")
    )
    def usageValue = "plugin_dependency"
  }
}
