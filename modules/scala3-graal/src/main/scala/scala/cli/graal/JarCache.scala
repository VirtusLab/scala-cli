package scala.cli.graal

trait JarCache {
  def cache(path: os.Path)(processPath: os.Path => ClassPathEntry): ClassPathEntry
  def put(entry: os.RelPath, bytes: Array[Byte]): ClassPathEntry
}

case class DirCache(dir: os.Path) extends JarCache {
  private def dest(original: os.Path) =
    dir / s"${original.toNIO.toString.hashCode()}-${original.last}"
  override def cache(path: os.Path)(processPath: os.Path => ClassPathEntry): ClassPathEntry =
    processPath(dest(path))

  override def put(entry: os.RelPath, content: Array[Byte]): ClassPathEntry = {
    val path = dir / entry
    os.write.over(path, content, createFolders = true)
    CreatedEntry(path)
  }
}

object TempCache extends JarCache {

  override def cache(path: os.Path)(processPath: os.Path => ClassPathEntry): ClassPathEntry =
    processPath(
      if (os.isDir(path)) os.temp.dir(prefix = path.last)
      else os.temp(prefix = path.baseName, suffix = "." + path.ext)
    )

  override def put(entry: os.RelPath, content: Array[Byte]): ClassPathEntry = {
    val path = os.temp(prefix = entry.baseName, suffix = entry.ext)
    os.write.over(path, content, createFolders = true)
    CreatedEntry(path)
  }

}
