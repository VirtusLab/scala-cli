package scala.cli.graal

import java.nio.file.Path

sealed trait ClassPathEntry {
  def nioPath: Path = path.toNIO
  def path: os.Path
  def modified = true
}

case class Unmodified(path: os.Path) extends ClassPathEntry {
  override def modified: Boolean = false
}
case class Processed(path: os.Path, original: os.Path, cache: JarCache) extends ClassPathEntry
case class CreatedEntry(path: os.Path)                                  extends ClassPathEntry

case class PathingJar(jar: ClassPathEntry, entries: Seq[ClassPathEntry]) extends ClassPathEntry {
  override def path: os.Path = jar.path
}
