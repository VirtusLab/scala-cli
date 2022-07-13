package scala.build.internal

import java.io.OutputStream

object ManifestJar {

  /** Creates a manifest JAR, in a temporary directory or in the passed scratched directory
    *
    * @param classPath
    *   Entries that should be put in the manifest class path
    * @param wrongSimplePaths
    *   Write paths slightly differently in manifest, so that tools such as native-image accept them
    *   (but the manifest JAR can't be passed to 'java -cp' any more on Windows)
    * @param scratchDirOpt
    *   an optional scratch directory to write the manifest JAR under
    */
  def create(
    classPath: Seq[os.Path],
    wrongSimplePaths: Boolean = false,
    scratchDirOpt: Option[os.Path] = None
  ): os.Path = {
    import java.util.jar._
    val manifest   = new Manifest
    val attributes = manifest.getMainAttributes
    attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    attributes.put(
      Attributes.Name.CLASS_PATH,
      if (wrongSimplePaths)
        // For tools, such as native-image, that don't correctly handle paths in manifests…
        classPath.map(_.toString).mkString(" ")
      else
        // Paths are encoded this weird way in manifest JARs. This matters on Windows in particular,
        // where paths like "C:\…" don't work fine.
        classPath.map(_.toNIO.toUri.getRawPath).mkString(" ")
    )
    val jarFile = scratchDirOpt match {
      case Some(scratchDir) =>
        os.makeDir.all(scratchDir)
        os.temp(dir = scratchDir, prefix = "classpathJar", suffix = ".jar", deleteOnExit = false)
      case None =>
        os.temp(prefix = "classpathJar", suffix = ".jar")
    }
    var os0: OutputStream    = null
    var jos: JarOutputStream = null
    try {
      os0 = os.write.outputStream(jarFile)
      jos = new JarOutputStream(os0, manifest)
    }
    finally {
      if (jos != null)
        jos.close()
      if (os0 != null)
        os0.close()
    }
    jarFile
  }

  /** Runs a block of code using a manifest JAR.
    *
    * See [[create]] for details about the parameters.
    */
  def maybeWithManifestClassPath[T](
    createManifest: Boolean,
    classPath: Seq[os.Path],
    wrongSimplePathsInManifest: Boolean = false
  )(
    f: Seq[os.Path] => T
  ): T =
    if (createManifest) {
      var toDeleteOpt = Option.empty[os.Path]

      try {
        val manifestJar = create(classPath, wrongSimplePaths = wrongSimplePathsInManifest)
        toDeleteOpt = Some(manifestJar)
        f(Seq(manifestJar))
      }
      finally
        for (toDelete <- toDeleteOpt)
          os.remove(toDelete)
    }
    else
      f(classPath)

}
