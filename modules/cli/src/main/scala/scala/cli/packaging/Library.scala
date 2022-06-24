package scala.cli.packaging

import java.io.{ByteArrayOutputStream, OutputStream}
import java.nio.file.attribute.FileTime
import java.util.jar.{Attributes => JarAttributes, JarOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.build.Build

object Library {

  def withLibraryJar[T](
    build: Build.Successful,
    fileName: String = "library",
    scratchDirOpt: Option[os.Path] = None
  )(f: os.Path => T): T = {
    val mainJarContent = libraryJar(build)
    scratchDirOpt.foreach(os.makeDir.all(_))
    val mainJar = os.temp(
      mainJarContent,
      dir = scratchDirOpt.orNull,
      prefix = fileName.stripSuffix(".jar"),
      suffix = ".jar",
      deleteOnExit = scratchDirOpt.isEmpty
    )
    try f(mainJar)
    finally if (scratchDirOpt.isEmpty) os.remove(mainJar)
  }

  def libraryJar(
    build: Build.Successful,
    mainClassOpt: Option[String] = None,
    hasActualManifest: Boolean = true,
    contentDirOverride: Option[os.Path] = None
  ): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    writeLibraryJarTo(baos, build, mainClassOpt, hasActualManifest, contentDirOverride)
    baos.toByteArray
  }

  def writeLibraryJarTo(
    outputStream: OutputStream,
    build: Build.Successful,
    mainClassOpt: Option[String] = None,
    hasActualManifest: Boolean = true,
    contentDirOverride: Option[os.Path] = None
  ): Unit = {

    val manifest = new java.util.jar.Manifest
    manifest.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")

    if (hasActualManifest)
      for (mainClass <- mainClassOpt.orElse(build.sources.defaultMainClass) if mainClass.nonEmpty)
        manifest.getMainAttributes.put(JarAttributes.Name.MAIN_CLASS, mainClass)

    var zos: ZipOutputStream = null
    val contentDir           = contentDirOverride.getOrElse(build.output)

    try {
      zos = new JarOutputStream(outputStream, manifest)
      for (path <- os.walk(contentDir) if os.isFile(path)) {
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

}
