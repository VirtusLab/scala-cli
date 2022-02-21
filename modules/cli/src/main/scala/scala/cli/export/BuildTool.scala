package scala.cli.export

import java.nio.charset.Charset

import scala.build.options.{BuildOptions, ScalaJsOptions}
import scala.build.{Logger, Sources}

abstract class BuildTool extends Product with Serializable {
  def `export`(
    optionsMain: BuildOptions,
    optionsTest: BuildOptions,
    sourcesMain: Sources,
    sourcesTest: Sources
  ): Project
}

object BuildTool {
  def sources(sources: Sources, charSet: Charset): Seq[(os.SubPath, String, Array[Byte])] = {

    val mainSources = sources.paths.map {
      case (path, relPath) =>
        val language =
          if (path.last.endsWith(".java")) "java"
          else "scala" // FIXME Others
        // FIXME asSubPath might throw… Make it a SubPath earlier in the API?
        (relPath.asSubPath, language, os.read.bytes(path))
    }

    val extraMainSources = sources.inMemory.map {
      case (_, relPath, content, _) =>
        val language =
          if (relPath.last.endsWith(".java")) "java"
          else "scala"
        (relPath.asSubPath, language, content.getBytes(charSet))
    }

    mainSources ++ extraMainSources
  }

  def scalaJsLinkerCalls(options: ScalaJsOptions, logger: Logger): Seq[String] = {

    var calls = Seq.empty[String]

    calls = calls ++ {
      if (options.moduleKindStr.isEmpty) Nil
      else
        Seq(s""".withModuleKind(ModuleKind.${options.moduleKind(logger)})""")
    }

    for (checkIr <- options.checkIr)
      calls = calls :+ s".withCheckIR($checkIr)"

    val withOptimizer = options.mode.contains("release")
    calls = calls :+ s".withOptimizer($withOptimizer)"
    calls = calls :+ s".withClosureCompiler($withOptimizer)"

    calls = calls :+ s".withSourceMap(${options.emitSourceMaps})"

    calls
  }

}
