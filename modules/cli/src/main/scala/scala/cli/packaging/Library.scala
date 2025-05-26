package scala.cli.packaging

import java.io.OutputStream
import java.nio.file.StandardOpenOption.{CREATE, TRUNCATE_EXISTING}
import java.nio.file.attribute.FileTime
import java.util.jar.{Attributes as JarAttributes, JarOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.build.Build
import scala.cli.internal.CachedBinary

object Library {
  def libraryJar(
    builds: Seq[Build.Successful],
    mainClassOpt: Option[String] = None
  ): os.Path = {
    val workDir = builds.head.inputs.libraryJarWorkDir
    val dest    = workDir / "library.jar"
    val cacheData =
      CachedBinary.getCacheData(
        builds,
        mainClassOpt.toList.flatMap(c => List("--main-class", c)),
        dest,
        workDir
      )

    if cacheData.changed then {
      var outputStream: OutputStream = null
      try {
        outputStream = os.write.outputStream(
          dest,
          createFolders = true,
          openOptions = Seq(CREATE, TRUNCATE_EXISTING)
        )
        writeLibraryJarTo(
          outputStream,
          builds,
          mainClassOpt
        )
      }
      finally
        if outputStream != null then outputStream.close()

      CachedBinary.updateProjectAndOutputSha(dest, workDir, cacheData.projectSha)
    }

    dest
  }

  def writeLibraryJarTo(
    outputStream: OutputStream,
    builds: Seq[Build.Successful],
    mainClassOpt: Option[String] = None,
    hasActualManifest: Boolean = true,
    contentDirOverride: Option[os.Path] = None
  ): Unit = {

    val manifest = new java.util.jar.Manifest
    manifest.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")

    if hasActualManifest then
      for {
        mainClass <- mainClassOpt.orElse(builds.flatMap(_.sources.defaultMainClass).headOption)
        if mainClass.nonEmpty
      } manifest.getMainAttributes.put(JarAttributes.Name.MAIN_CLASS, mainClass)

    var zos: ZipOutputStream = null
    val contentDirs          = builds.map(b => contentDirOverride.getOrElse(b.output)).distinct

    try {
      zos = new JarOutputStream(outputStream, manifest)
      for {
        contentDir <- contentDirs
        path       <- os.walk(contentDir) if os.isFile(path)
      } {
        val name         = path.relativeTo(contentDir).toString
        val lastModified = os.mtime(path)
        val ent          = new ZipEntry(name)
        ent.setLastModifiedTime(FileTime.fromMillis(lastModified))

        val content = os.read.bytes(path)
        ent.setSize(content.length)

        zos.putNextEntry(ent)
        zos.write(content)
        zos.closeEntry()
      }
    }
    finally if (zos != null) zos.close()
  }

  extension (build: Build.Successful) {
    private def fullClassPathAsJar: Seq[os.Path] =
      Seq(libraryJar(Seq(build))) ++ build.dependencyClassPath
    def fullClassPathMaybeAsJar(asJar: Boolean): Seq[os.Path] =
      if asJar then fullClassPathAsJar else build.fullClassPath
  }

}
