package scala.cli.graal

import org.objectweb.asm._

import java.io.{File, InputStream}
import java.nio.file.{Files, StandardOpenOption}
import java.util.jar.{Attributes, JarEntry, JarFile, JarOutputStream, Manifest}

import scala.jdk.CollectionConverters._

object BytecodeProcessor {

  def toClean(classpath: Seq[ClassPathEntry]): Seq[os.Path] = classpath.flatMap {
    case Processed(path, _, TempCache) => Seq(path)
    case PathingJar(path, entries)     => toClean(path +: entries)
    case _                             => Nil
  }

  def processPathingJar(pathingJar: os.Path, cache: JarCache): Seq[ClassPathEntry] = {
    val jarFile = new JarFile(pathingJar.toIO)
    try {
      val cp = jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.CLASS_PATH)
      if (cp != null && cp.nonEmpty) {
        // paths in pathing jars are spectated by spaces
        val entries = cp.split(" +").toSeq.map { rawEntry =>
          os.Path(rawEntry, os.pwd)
        }
        val processedCp = processClassPathEntries(entries, cache)
        val dest        = os.temp(suffix = ".jar")
        val outStream   = Files.newOutputStream(dest.toNIO, StandardOpenOption.CREATE)
        try {
          val stringCp = processedCp.map(_.path.toNIO).mkString(" ")
          val manifest = new Manifest(jarFile.getManifest())
          manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, stringCp)
          val outjar = new JarOutputStream(outStream, manifest)
          outjar.close()
          dest.toNIO.toString()
          Seq(PathingJar(Processed(dest, pathingJar, TempCache), processedCp))
        }
        finally outStream.close()
      }
      else processClassPathEntries(Seq(pathingJar), cache)
    }
    finally jarFile.close()
  }

  def processClassPath(classPath: String, cache: JarCache = TempCache): Seq[ClassPathEntry] =
    classPath.split(File.pathSeparator) match {
      case Array(maybePathingJar) if maybePathingJar.endsWith(".jar") =>
        processPathingJar(os.Path(maybePathingJar, os.pwd), cache)
      case cp =>
        val cp0 = cp.toSeq.map(os.Path(_, os.pwd))
        processClassPathEntries(cp0, cache)
    }

  def processClassPathEntries(entries: Seq[os.Path], cache: JarCache): Seq[ClassPathEntry] = {
    val cp = entries.map { path =>
      cache.cache(path) { dest =>
        if (path.ext == "jar" && os.isFile(path)) processJar(path, dest, cache)
        else if (os.isDir(path)) processDir(path, dest, cache)
        else Unmodified(dest)
      }
    }
    if (cp.exists(_.modified)) {
      // jar with runtime deps is added as a resource
      // scala3RuntimeFixes.jar is also used within
      // resource-config.json and BytecodeProcessor.scala
      val jarName      = "scala3RuntimeFixes.jar"
      val runtimeJarIs = getClass().getClassLoader.getResourceAsStream(jarName)
      if (runtimeJarIs == null) throw new NoSuchElementException(
        "Unable to find scala3RuntimeFixes.jar on classpath, did you add scala3-graal jar on classpath?"
      )
      val created = cache.put(os.RelPath(jarName), runtimeJarIs.readAllBytes())
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
}
