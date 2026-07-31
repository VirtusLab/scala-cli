package scala.build.internal

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.jar.{Attributes as JarAttributes, Manifest as JarManifest}

import scala.jdk.CollectionConverters.*

/** Helpers for building JAR manifests that honour a user-supplied `META-INF/MANIFEST.MF` (typically
  * copied into the class directory via a resource dir) while still letting Scala CLI overlay its
  * own attributes (e.g. `Main-Class`).
  */
object JarManifests:

  val manifestEntryName: String = "META-INF/MANIFEST.MF"

  def isManifestEntry(name: String): Boolean =
    name == manifestEntryName || name == "META-INF\\MANIFEST.MF"

  /** Reads a user-supplied `META-INF/MANIFEST.MF` from a class/resource directory, if present. */
  def userManifestIn(dir: os.Path): Option[JarManifest] =
    val path = dir / "META-INF" / "MANIFEST.MF"
    Option.when(os.isFile(path)):
      JarManifest(ByteArrayInputStream(os.read.bytes(path)))

  /** Builds a manifest using `base` (typically a user-supplied one) and overlays Scala CLI
    * attributes. Scala CLI wins on conflicts. Always sets `Manifest-Version: 1.0`.
    */
  def merged(
    base: Option[JarManifest],
    attributes: Seq[(JarAttributes.Name, String)]
  ): JarManifest =
    val manifest = base match
      case Some(m) =>
        val copy = JarManifest()
        copy.getMainAttributes.putAll(m.getMainAttributes)
        for (entryName, attrs) <- m.getEntries.asScala do
          val attrsCopy = JarAttributes()
          attrsCopy.putAll(attrs)
          copy.getEntries.put(entryName, attrsCopy)
        copy
      case None =>
        JarManifest()
    manifest.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")
    for (key, value) <- attributes do
      manifest.getMainAttributes.put(key, value)
    manifest

  def bytes(manifest: JarManifest): Array[Byte] =
    val baos = ByteArrayOutputStream()
    manifest.write(baos)
    baos.toByteArray
