package scala.cli.commands

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.Constants

// format: off
final case class ScalaJsOptions(

  @Group("Scala")
  @HelpMessage("Enable Scala.js. To show more options for Scala.js pass `--help-js`")
    js: Boolean = false,

  @Group("Scala.js")
  @HelpMessage(s"The Scala.js version (${Constants.scalaJsVersion} by default).")
    jsVersion: Option[String] = None,
  @Group("Scala.js")
  @HelpMessage("The Scala.js mode, either `dev` or `release`")
    jsMode: Option[String] = None,
  @HelpMessage("The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none")
  @Group("Scala.js")
    jsModuleKind: Option[String] = None,

  @Group("Scala.js")
  jsCheckIr: Option[Boolean] = None,

  @Group("Scala.js")
  @HelpMessage("Emit source maps")
    jsEmitSourceMaps: Boolean = false,
  @Group("Scala.js")
  @HelpMessage("Set the destination path of source maps")
   jsSourceMapsPath: Option[String] = None,
  @Group("Scala.js")
  @HelpMessage("Enable jsdom")
    jsDom: Option[Boolean] = None,
  @Group("Scala.js")
  @HelpMessage("A header that will be added at the top of generated .js files")
    jsHeader: Option[String] = None,
  @Group("Scala.js")
  @HelpMessage("Primitive Longs *may* be compiled as primitive JavaScript bigints")
    jsAllowBigIntsForLongs: Option[Boolean] = None,
  @Group("Scala.js")
  @HelpMessage("Avoid class'es when using functions and prototypes has the same observable semantics.")
    jsAvoidClasses: Option[Boolean] = None,
  @Group("Scala.js")
  @HelpMessage("Avoid lets and consts when using vars has the same observable semantics.")
    jsAvoidLetsAndConsts: Option[Boolean] = None,
  @Group("Scala.js")
  @HelpMessage("The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor")
    jsModuleSplitStyle: Option[String] = None,
  @Group("Scala.js")
  @HelpMessage("Create as many small modules as possible for the classes in the passed packages and their subpackages.")
    jsSmallModuleForPackage: List[String] = Nil,
  @Group("Scala.js")
  @HelpMessage("The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021")
    jsEsVersion: Option[String] = None,

  @Group("Scala.js")
  @HelpMessage("Path to the Scala.js linker")
  @ValueDescription("path")
  @Hidden
    jsLinkerPath: Option[String] = None,
  @Group("Scala.js")
  @HelpMessage(s"Scala.js CLI version to use for linking (${Constants.scalaJsCliVersion} by default).")
  @ValueDescription("version")
  @Hidden
    jsCliVersion: Option[String] = None,
  @Group("Scala.js")
  @HelpMessage("Scala.js CLI Java options")
  @ValueDescription("option")
  @Hidden
    jsCliJavaArg: List[String] = Nil,
  @Group("Scala.js")
  @HelpMessage("Whether to run the Scala.js CLI on the JVM or using a native executable")
  @Hidden
    jsCliOnJvm: Option[Boolean] = None
)
// format: on

object ScalaJsOptions {
  lazy val parser: Parser[ScalaJsOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[ScalaJsOptions, parser.D] = parser
  implicit lazy val help: Help[ScalaJsOptions]                      = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[ScalaJsOptions]       = JsonCodecMaker.make
}
