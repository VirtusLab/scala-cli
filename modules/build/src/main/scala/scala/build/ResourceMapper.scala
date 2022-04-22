package scala.build

import scala.collection.Map
import scala.jdk.CollectionConverters._


/* Maps resources from their original path, to destination path,
 * also caching that mapping. 
 * 
 * Remembering the mapping this way allows for the resource to be removed
 * if the original file is renamed/deleted. 
 */
object ResourceMapper {

  private def readMappingIfExists(mappingFile: os.Path): Option[Map[os.Path, os.Path]] = {
    if (os.exists(mappingFile)) {
      val mappingContent = os.read(mappingFile)
      Some(mappingContent.split('\n').map { pair =>
        val sep = pair.split(',')
        (os.Path(sep(0)), os.Path(sep(1)))
      }.toMap)
    } else None
  }

  private def writeMapping(mappingPath: os.Path, mapping: Map[os.Path, os.Path]) = {
    val mappingContent = mapping.map{ case (inputPath, outputPath) =>
      s"$inputPath,$outputPath"
    }.mkString("\n")
    os.write.over(mappingPath, mappingContent)
  }

  def scalaNativeCFileMapping(build: Build.Successful): Map[os.Path, os.Path] = {
    build.inputs.flattened().collect {
      case cfile: Inputs.CFile =>
        val inputPath = cfile.path
        val destPath = build.output / "scala-native" / cfile.subPath
        (inputPath, destPath)
    }.toMap
  }

  private def resolveProjectCFileMappingPath(nativeWorkDir: os.Path) = nativeWorkDir / ".project_native_mapping"

  def copyCFilesToScalaNativeDir(build: Build.Successful, nativeWorkDir: os.Path): Unit = {
    def isInDirectory(output: os.Path, filePath: os.Path) = {
      val outputFullPath = output.toNIO.iterator().asScala.toSeq
      val fileFullPath = output.toNIO.iterator().asScala.toSeq
      fileFullPath.startsWith(outputFullPath)
    }

    val mappingPath = resolveProjectCFileMappingPath(nativeWorkDir)
    val oldMapping = readMappingIfExists(mappingPath).getOrElse(Map.empty)
    val newMapping = scalaNativeCFileMapping(build)

    val removedFiles = oldMapping.values.toSet -- newMapping.values.toSet
    removedFiles.foreach { outputPath =>
      // Delete only if in outpath, to not cause any harm
      if (isInDirectory(build.output, outputPath))
        os.remove(outputPath)
    }

    newMapping.toList.foreach { case (inputPath, outputPath) =>
      os.copy(
        inputPath,
        outputPath,
        replaceExisting = true,
        createFolders = true
      )
    }
    writeMapping(mappingPath, newMapping)
  }
}