package scala.cli.graal

import java.io.File
import org.objectweb.asm._
import java.nio.file.Path
import java.io.InputStream
import java.util.jar.JarFile
import java.util.jar.JarEntry
import org.objectweb.asm.ClassWriter
import scala.jdk.CollectionConverters._
import java.util.jar.JarOutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption

trait JarCache {
  def cache(path: os.Path)(processPath: os.Path => ClassPathEntry): ClassPathEntry
  def put(entry: os.RelPath, bytes: Array[Byte]): ClassPathEntry
}

sealed trait ClassPathEntry {
  def nioPath: Path = path.toNIO
  def path: os.Path
}
case class Unmodified(path: os.Path)                                    extends ClassPathEntry
case class Processed(path: os.Path, original: os.Path, cache: JarCache) extends ClassPathEntry
case class CreatedEntry(path: os.Path)                                  extends ClassPathEntry

case class ProcessedClasspath(entries: Seq[ClassPathEntry]) {
  def newClassPath = entries.map(_.path.toNIO).mkString(File.pathSeparator)
}

object TempCache extends JarCache {
  override def cache(path: os.Path)(processPath: os.Path => ClassPathEntry): ClassPathEntry =
    processPath(
      if (os.isDir(path)) os.temp.dir(prefix = path.last)
      else os.temp(prefix = path.baseName, suffix = path.ext)
    )

  override def put(entry: os.RelPath, content: Array[Byte]): ClassPathEntry = {
    val path = os.temp(prefix = entry.baseName, suffix = entry.ext)
    os.write(path, content, createFolders = true)
    CreatedEntry(path)
  }

}

object BytecodeProcessor {
  def processClasspath(classpath: String, cache: JarCache = TempCache) = {
    val cp = classpath.split(File.pathSeparator).map { str =>
      val path = toPath(str)
      cache.cache(path) { dest =>
        if (path.ext == "jar" && os.isFile(path)) processJar(path, dest, cache)
        else if (os.isDir(path)) processDir(path, dest, cache)
        else Unmodified(dest)
      }
    }
    if (cp.exists(_.isInstanceOf[Processed])) {
      // jar with runtime deps is added as a resource
      val runtimeJar = getClass().getClassLoader.getResourceAsStream("out.jar").readAllBytes()
      val created    = cache.put(os.RelPath("safeBytecode.jar"), runtimeJar)
      created +: cp
    }
    else cp // No need to add processed jar
  }

  def processDir(dir: os.Path, dest: os.Path, cache: JarCache): ClassPathEntry = {
    val paths = os.walk(dir).filter(os.isFile)
    val (skipped, processed) = paths.partitionMap {
      case p if p.ext != "class" =>
        Left(p)
      case clazzFile =>
        val original = os.read.bytes(clazzFile)
        processClassFile(original) match {
          case Some(content) =>
            val relPath  = clazzFile.relativeTo(dir)
            val destPath = dest / relPath
            os.makeDir.all(destPath / os.up)
            assert(content != original)
            os.write(destPath, content)
            Right(clazzFile)
          case _ =>
            Left(clazzFile)
        }
    }
    if (processed.nonEmpty) {
      skipped.foreach(file =>
        os.copy.over(file, dest / (file.relativeTo(dir)), createFolders = true)
      )
      Processed(dest, dir, cache)
    }
    else Unmodified(dir)
  }

  def processJar(path: os.Path, dest: os.Path, cache: JarCache): ClassPathEntry = {
    val jarFile = new JarFile(path.toIO)
    try {
      var processedBytecode: Option[Array[Byte]] = None
      val endMarker                              = "///" // not a valid path
      var processed: String                      = endMarker
      def processEntry(entry: JarEntry) = {
        val newBytecode = processClassFile(jarFile.getInputStream(entry))
        processed = entry.getName()
        processedBytecode = newBytecode
        newBytecode.fold(entry.getName())(_ => endMarker) // empty string is an end marker
      }

      val classFilesIterator =
        jarFile.entries().asIterator().asScala.filter(_.getName().endsWith(".class"))
      val cachedEntries = classFilesIterator.map(processEntry).takeWhile(_ != endMarker).toSet

      if (processedBytecode.isEmpty) Unmodified(path)
      else {
        os.makeDir.all(dest / os.up)
        val outStream = Files.newOutputStream(dest.toNIO, StandardOpenOption.CREATE)
        val outjar    = new JarOutputStream(outStream)
        jarFile.entries().asIterator().asScala.foreach { entry =>
          val content: Array[Byte] = jarFile.getInputStream(entry).readAllBytes()
          val name                 = entry.getName()
          val destBytes =
            if (cachedEntries.contains(name) || !name.endsWith(".class")) content
            else if (name == processed) processedBytecode.get
            else processClassFile(content).getOrElse(content)

          val newEntry = new JarEntry(entry.getName())

          outjar.putNextEntry(newEntry)
          outjar.write(destBytes)
          outjar.closeEntry()
        }
        outjar.close()
        Processed(dest, path, cache)
      }
    }
    finally jarFile.close()
  }

  def processClassReader(reader: ClassReader): Option[Array[Byte]] = {
    val writer  = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
    val visitor = new LazyValVisitor(writer)
    val res     = util.Try(reader.accept(visitor, 0))
    if (visitor.changed && res.isSuccess) Some(writer.toByteArray) else None
  }

  def processClassFile(content: => InputStream): Option[Array[Byte]] = {
    val is = content
    try processClassReader(new ClassReader(is))
    finally is.close()
  }

  def processClassFile(content: Array[Byte]): Option[Array[Byte]] =
    processClassReader(new ClassReader(content))

  class LazyValVisitor(writer: ClassWriter) extends ClassVisitor(Opcodes.ASM9, writer) {

    var changed: Boolean = false

    class StaticInitVistor(parent: MethodVisitor)
        extends MethodVisitor(Opcodes.ASM9, parent) {

      override def visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descr: String,
        isInterface: Boolean
      ): Unit =
        if (owner == "scala/runtime/LazyVals$" && name == "getOffset") {
          changed = true
          super.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/Class",
            "getDeclaredField",
            "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
            false
          )
          super.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "scala/cli/runtime/SafeLazyVals",
            "getOffset",
            "(Ljava/lang/Object;Ljava/lang/reflect/Field;)J",
            false
          )
        }
        else
          super.visitMethodInsn(opcode, owner, name, descr, isInterface)
    }

    override def visitMethod(
      access: Int,
      name: String,
      desc: String,
      sig: String,
      exceptions: Array[String]
    ): MethodVisitor =
      if (name == "<clinit>")
        new StaticInitVistor(super.visitMethod(access, name, desc, sig, exceptions))
      else
        super.visitMethod(access, name, desc, sig, exceptions)
  }

  def toPath(str: String) = os.FilePath(str) match {
    case p: os.Path    => p
    case r: os.RelPath => os.pwd / r
    case s: os.SubPath => os.pwd / s
  }
}
