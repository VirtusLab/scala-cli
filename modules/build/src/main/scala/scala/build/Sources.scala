package scala.build

import scala.build.internal.CodeWrapper
import scala.build.options.BuildOptions
import scala.build.preprocessing._

final case class Sources(
  paths: Seq[(os.Path, os.RelPath)],
  inMemory: Seq[(Either[String, os.Path], os.RelPath, String, Int)],
  mainClass: Option[String],
  resourceDirs: Seq[os.Path],
  buildOptions: BuildOptions
) {

  def generateSources(generatedSrcRoot: os.Path): Seq[GeneratedSource] = {
    val generated =
      for ((reportingPath, relPath, code, topWrapperLen) <- inMemory) yield {
        os.write.over(generatedSrcRoot / relPath, code.getBytes("UTF-8"), createFolders = true)
        (reportingPath, relPath, topWrapperLen)
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

  def defaultPreprocessors(codeWrapper: CodeWrapper): Seq[Preprocessor] =
    Seq(
      ScriptPreprocessor(codeWrapper),
      JavaPreprocessor,
      ScalaPreprocessor,
      DataPreprocessor
    )
}
