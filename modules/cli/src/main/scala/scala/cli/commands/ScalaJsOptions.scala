package scala.cli.commands

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.build.internal.FetchExternalBinary
import scala.build.{Os, options}

// format: off
final case class ScalaJsOptions(

  @Group("Scala")
  @HelpMessage("Enable Scala.JS. To show more options for Scala.Js pass `--help-js`")
    js: Boolean = false,

  @Group("Scala.JS")
  @HelpMessage("The Scala JS version")
    jsVersion: Option[String] = None,
  @Group("Scala.JS")
  @HelpMessage("The Scala JS mode, either `dev` or `release`")
    jsMode: Option[String] = None,
  @HelpMessage("The Scala JS module kind: commonjs/common, esmodule/es, nomodule/none")
  @Group("Scala.JS")
    jsModuleKind: Option[String] = None,

  @Group("Scala.JS")
  jsCheckIr: Option[Boolean] = None,

  @Group("Scala.JS")
  @HelpMessage("Emit source maps")
    jsEmitSourceMaps: Boolean = false,
  @Group("Scala.JS")
  @HelpMessage("Set the destination path of source maps")
   jsSourceMapsPath: Option[String] = None,
  @Group("Scala.JS")
  @HelpMessage("Enable jsdom")
    jsDom: Option[Boolean] = None,
  @Group("Scala.JS")
  @HelpMessage("A header that will be added at the top of generated .js files")
    jsHeader: Option[String] = None,
  @Group("Scala.JS")
  @HelpMessage("Primitive Longs *may* be compiled as primitive JavaScript bigints")
    jsAllowBigIntsForLongs: Option[Boolean] = None,
  @Group("Scala.JS")
  @HelpMessage("Avoid class'es when using functions and prototypes has the same observable semantics.")
    jsAvoidClasses: Option[Boolean] = None,
  @Group("Scala.JS")
  @HelpMessage("Avoid lets and consts when using vars has the same observable semantics.")
    jsAvoidLetsAndConsts: Option[Boolean] = None,
  @Group("Scala.JS")
  @HelpMessage("The Scala JS module split style: fewestmodules, smallestmodules")
    jsModuleSplitStyle: Option[String] = None,
  @Group("Scala.JS")
  @HelpMessage("The Scala JS ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021")
    jsEsVersion: Option[String] = None,

  @Group("Scala.JS")
  @HelpMessage("Path to the Scala.JS linker")
  @ValueDescription("path")
  @Hidden
    jsLinkerPath: Option[String] = None,
  @Group("Scala.JS")
  @HelpMessage("Scala.JS CLI version to use for linking")
  @ValueDescription("version")
  @Hidden
    jsCliVersion: Option[String] = None,
  @Group("Scala.JS")
  @HelpMessage("Scala.JS CLI Java options")
  @ValueDescription("option")
  @Hidden
    jsCliJavaArg: List[String] = Nil,
  @Group("Scala.JS")
  @HelpMessage("Whether to run the Scala.JS CLI on the JVM or using a native executable")
  @Hidden
    jsCliOnJvm: Option[Boolean] = None
) {
  // format: on

  def scalaJsOptions: options.ScalaJsOptions =
    options.ScalaJsOptions(
      version = jsVersion,
      mode = jsMode,
      moduleKindStr = jsModuleKind,
      checkIr = jsCheckIr,
      emitSourceMaps = jsEmitSourceMaps,
      sourceMapsDest = jsSourceMapsPath.filter(_.trim.nonEmpty).map(os.Path(_, Os.pwd)),
      dom = jsDom,
      header = jsHeader,
      allowBigIntsForLongs = jsAllowBigIntsForLongs,
      avoidClasses = jsAvoidClasses,
      avoidLetsAndConsts = jsAvoidLetsAndConsts,
      moduleSplitStyleStr = jsModuleSplitStyle,
      esVersionStr = jsEsVersion
    )
  def linkerOptions: options.scalajs.ScalaJsLinkerOptions =
    options.scalajs.ScalaJsLinkerOptions(
      linkerPath = jsLinkerPath
        .filter(_.trim.nonEmpty)
        .map(os.Path(_, Os.pwd)),
      scalaJsCliVersion = jsCliVersion.map(_.trim).filter(_.nonEmpty),
      javaArgs = jsCliJavaArg,
      useJvm = jsCliOnJvm.map {
        case false => Left(FetchExternalBinary.platformSuffix())
        case true  => Right(())
      }
    )
}

object ScalaJsOptions {
  lazy val parser: Parser[ScalaJsOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[ScalaJsOptions, parser.D] = parser
  implicit lazy val help: Help[ScalaJsOptions]                      = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[ScalaJsOptions]       = JsonCodecMaker.make
}
