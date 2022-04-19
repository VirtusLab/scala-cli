package scala.cli.graal

case class CoursierCache(root: os.Path) extends JarCache {
  val hasher: os.Path => String = p => os.size(p).toString() // replace with proper md5 based caches
  val prefix                    = "v2"
  val cache                     = root / ".graal_processor"

  override def put(entry: os.RelPath, content: Array[Byte]): ClassPathEntry = {
    // TODO better hashing
    val name = s"${entry.baseName}_${content.length}_$prefix.${entry.ext}"
    val dest = cache / entry / os.up / name
    if (!os.exists(dest)) {
      os.makeDir.all(dest / os.up)
      os.write(dest, content)
    }
    CreatedEntry(dest)
  }

  override def cache(path: os.Path)(processPath: os.Path => ClassPathEntry): ClassPathEntry = {
    def ignore = TempCache.cache(path)(processPath)
    if (!path.startsWith(root) || os.isDir(path)) ignore
    else {
      val relPath = path.relativeTo(root)
      val name    = s"${path.baseName}_${hasher(path)}_$prefix.${path.ext}"
      val dest    = cache / relPath / os.up / name
      if (os.exists(dest)) Processed(dest, path, this)
      else processPath(dest)
    }
  }
}
