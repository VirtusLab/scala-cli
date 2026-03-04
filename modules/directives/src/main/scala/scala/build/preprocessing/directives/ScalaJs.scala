package scala.build.preprocessing.directives

import os.Path

import scala.build.Ops.EitherOptOps
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, ScalaJsMode, ScalaJsOptions}
import scala.cli.commands.SpecificationLevel
import scala.util.Try

@DirectiveGroupName("Scala.js options")
@DirectiveExamples(s"//> using jsVersion ${Constants.scalaJsVersion}")
@DirectiveExamples("//> using jsMode mode")
@DirectiveExamples("//> using jsNoOpt")
@DirectiveExamples("//> using jsModuleKind common")
@DirectiveExamples("//> using jsCheckIr")
@DirectiveExamples("//> using jsEmitSourceMaps")
@DirectiveExamples("//> using jsEsModuleImportMap importmap.json")
@DirectiveExamples("//> using jsSmallModuleForPackage test")
@DirectiveExamples("//> using jsDom")
@DirectiveExamples("//> using jsHeader \"#!/usr/bin/env node\n\"")
@DirectiveExamples("//> using jsAllowBigIntsForLongs")
@DirectiveExamples("//> using jsAvoidClasses")
@DirectiveExamples("//> using jsAvoidLetsAndConsts")
@DirectiveExamples("//> using jsModuleSplitStyleStr smallestmodules")
@DirectiveExamples("//> using jsEsVersionStr es2017")
@DirectiveExamples("//> using jsEmitWasm")
@DirectiveUsage(
  "//> using jsVersion|jsMode|jsModuleKind|… _value_",
  """
    |`//> using jsVersion` _value_
    |
    |`//> using jsMode` _value_
    |
    |`//> using jsNoOpt` _true|false_
    |
    |`//> using jsNoOpt`
    |
    |`//> using jsModuleKind` _value_
    |
    |`//> using jsCheckIr` _true|false_
    |
    |`//> using jsCheckIr`
    |
    |`//> using jsEmitSourceMaps` _true|false_
    |
    |`//> using jsEmitSourceMaps`
    |
    |`//> using jsEsModuleImportMap` _value_
    |
    |`//> using jsSmallModuleForPackage` _value1_ _value2_ …
    |
    |`//> using jsDom` _true|false_
    |
    |`//> using jsDom`
    |
    |`//> using jsHeader` _value_
    |
    |`//> using jsAllowBigIntsForLongs` _true|false_
    |
    |`//> using jsAllowBigIntsForLongs`
    |
    |`//> using jsAvoidClasses` _true|false_
    |
    |`//> using jsAvoidClasses`
    |
    |`//> using jsAvoidLetsAndConsts` _true|false_
    |
    |`//> using jsAvoidLetsAndConsts`
    |
    |`//> using jsModuleSplitStyleStr` _value_
    |
    |`//> using jsEsVersionStr` _value_
    |    
    |`//> using jsEmitWasm` _true|false_
    |
    |`//> using jsEmitWasm`
    |""".stripMargin
)
@DirectiveDescription("Add Scala.js options")
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class ScalaJs(
  jsVersion: Option[String] = None,
  jsMode: Option[String] = None,
  jsNoOpt: Option[Boolean] = None,
  jsModuleKind: Option[String] = None,
  jsCheckIr: Option[Boolean] = None,
  jsEmitSourceMaps: Option[Boolean] = None,
  jsEsModuleImportMap: Option[String] = None,
  jsSmallModuleForPackage: List[String] = Nil,
  jsDom: Option[Boolean] = None,
  jsHeader: Option[String] = None,
  jsAllowBigIntsForLongs: Option[Boolean] = None,
  jsAvoidClasses: Option[Boolean] = None,
  jsAvoidLetsAndConsts: Option[Boolean] = None,
  jsModuleSplitStyleStr: Option[String] = None,
  jsEsVersionStr: Option[String] = None,
  jsEmitWasm: Option[Boolean] = None
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    val scalaJsOptions = ScalaJsOptions(
      version = jsVersion,
      mode = ScalaJsMode(jsMode),
      moduleKindStr = jsModuleKind,
      checkIr = jsCheckIr,
      emitSourceMaps = jsEmitSourceMaps.getOrElse(ScalaJsOptions().emitSourceMaps),
      smallModuleForPackage = jsSmallModuleForPackage,
      dom = jsDom,
      header = jsHeader,
      allowBigIntsForLongs = jsAllowBigIntsForLongs,
      avoidClasses = jsAvoidClasses,
      avoidLetsAndConsts = jsAvoidLetsAndConsts,
      moduleSplitStyleStr = jsModuleSplitStyleStr,
      esVersionStr = jsEsVersionStr,
      noOpt = jsNoOpt,
      jsEmitWasm = jsEmitWasm.getOrElse(false)
    )

    def absFilePath(pathStr: String): Either[ImportMapNotFound, Path] =
      Try(os.Path(pathStr, os.pwd)).toEither.fold(
        ex =>
          Left(ImportMapNotFound(
            s"""Invalid path to EsImportMap. Please check your "using jsEsModuleImportMap xxxx" directive. Does this file exist $pathStr ?""",
            ex
          )),
        path =>
          os.isFile(path) && os.exists(path) match {
            case false => Left(ImportMapNotFound(
                s"""Invalid path to EsImportMap. Please check your "using jsEsModuleImportMap xxxx" directive. Does this file exist $pathStr ?""",
                null
              ))
            case true => Right(path)
          }
      )
    val jsImportMapAsPath = jsEsModuleImportMap.map(absFilePath).sequence
    jsImportMapAsPath.map(_ match
      case None            => BuildOptions(scalaJsOptions = scalaJsOptions)
      case Some(importmap) =>
        BuildOptions(
          scalaJsOptions = scalaJsOptions.copy(remapEsModuleImportMap = Some(importmap))
        ))
}

class ImportMapNotFound(message: String, cause: Throwable)
    extends BuildException(message, cause = cause)

object ScalaJs {
  val handler: DirectiveHandler[ScalaJs] = DirectiveHandler.derive
}
