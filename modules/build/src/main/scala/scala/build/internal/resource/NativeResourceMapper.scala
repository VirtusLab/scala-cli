package scala.build.internal.resource

import scala.build.Build
import scala.build.input.{CFile, Inputs}

object NativeResourceMapper {

  private def scalaNativeCFileMapping(build: Build.Successful): Map[os.Path, os.RelPath] =
    build
      .inputs
      .flattened()
      .collect {
        case cfile: CFile =>
          val inputPath = cfile.path
          val destPath  = os.rel / "scala-native" / cfile.subPath
          (inputPath, destPath)
      }
      .toMap

  private def resolveProjectCFileRegistryPath(nativeWorkDir: os.Path) =
    nativeWorkDir / ".native_registry"

  /** Copies and maps c file resources from their original path to the destination path in build
    * output, also caching output paths in a file.
    *
    * Remembering the mapping this way allows for the resource to be removed if the original file is
    * renamed/deleted.
    */
  def copyCFilesToScalaNativeDir(build: Build.Successful, nativeWorkDir: os.Path): Unit = {
    val mappingFilePath = resolveProjectCFileRegistryPath(nativeWorkDir)
    ResourceMapper.copyResourcesToDirWithMapping(
      build.output,
      mappingFilePath,
      scalaNativeCFileMapping(build)
    )
  }
}
