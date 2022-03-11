package scala.build

import scala.build.internal.CodeWrapper
import scala.build.options.{BuildOptions, Scope}
import scala.build.preprocessing._

final case class Sources(
  paths: Seq[(os.Path, os.RelPath)],
  inMemory: Seq[Sources.InMemory],
  mainClass: Option[String],
  resourceDirs: Seq[os.Path],
  buildOptions: BuildOptions
) {

  def withVirtualDir(inputs: Inputs, scope: Scope, options: BuildOptions): Sources = {

    val srcRootPath = inputs.generatedSrcRoot(scope)
    val resourceDirs0 = options.classPathOptions.resourcesVirtualDir.map { path =>
      srcRootPath / path
    }

    copy(
      resourceDirs = resourceDirs ++ resourceDirs0
    )
  }

  def generateSources(generatedSrcRoot: os.Path): Seq[GeneratedSource] = {
    val generated =
      for (inMemSource <- inMemory) yield {
        os.write.over(
          generatedSrcRoot / inMemSource.generatedRelPath,
          inMemSource.generatedContent.getBytes("UTF-8"),
          createFolders = true
        )
        (
          inMemSource.originalPath.map(_._2),
          inMemSource.generatedRelPath,
          inMemSource.topWrapperLen
        )
      }

    val generatedSet = generated.map(_._2).toSet
    if (os.isDir(generatedSrcRoot))
      os.walk(generatedSrcRoot)
        .filter(os.isFile(_))
        .filter(p => !generatedSet(p.relativeTo(generatedSrcRoot)))
        .foreach(os.remove(_))

    generated.map {
      case (reportingPath, path, topWrapperLen) =>
        GeneratedSource(generatedSrcRoot / path, reportingPath, topWrapperLen)
    }
  }
}

object Sources {

  final case class InMemory(
    originalPath: Either[String, (os.SubPath, os.Path)],
    generatedRelPath: os.RelPath,
    generatedContent: String,
    topWrapperLen: Int
  )

  def defaultPreprocessors(codeWrapper: CodeWrapper): Seq[Preprocessor] =
    Seq(
      ScriptPreprocessor(codeWrapper),
      JavaPreprocessor,
      ScalaPreprocessor,
      DataPreprocessor
    )
}
