package scala.cli.exportCmd

import dependency.NoAttributes

import java.nio.charset.Charset
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaJsOptions}
import scala.build.{Logger, Sources}

abstract class ProjectDescriptor extends Product with Serializable {
  def `export`(
    optionsMain: BuildOptions,
    optionsTest: BuildOptions,
    sourcesMain: Sources,
    sourcesTest: Sources
  ): Either[BuildException, Project]
}

object ProjectDescriptor {
  def sources(sources: Sources): Seq[(os.SubPath, String, Array[Byte])] = {

    val mainSources = sources.paths.map {
      case (path, relPath) =>
        val language =
          if (path.last.endsWith(".java")) "java"
          else "scala" // FIXME Others
        // FIXME asSubPath might throw… Make it a SubPath earlier in the API?
        (relPath.asSubPath, language, os.read.bytes(path))
    }

    val extraMainSources = sources.inMemory.map { inMemSource =>
      val language =
        if (inMemSource.generatedRelPath.last.endsWith(".java")) "java"
        else "scala"
      (
        inMemSource.generatedRelPath.asSubPath,
        language,
        inMemSource.content
      )
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

    val withOptimizer = options.fullOpt.getOrElse(false)
    calls = calls :+ s".withOptimizer($withOptimizer)"
    calls = calls :+ s".withClosureCompiler($withOptimizer)"

    calls = calls :+ s".withSourceMap(${options.emitSourceMaps})"

    calls
  }

  def isPureJavaProject(options: BuildOptions, sources: Sources): Boolean =
    !options.scalaOptions.addScalaLibrary.contains(true) &&
    !options.scalaOptions.addScalaCompiler.contains(true) &&
    sources.paths.forall(_._1.last.endsWith(".java")) &&
    sources.inMemory.forall(_.generatedRelPath.last.endsWith(".java")) &&
    options.classPathOptions.allExtraDependencies.toSeq
      .forall(_.value.nameAttributes == NoAttributes)

}
