package scala.build.internal

import org.objectweb.asm
import org.objectweb.asm.ClassReader

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.file.NoSuchFileException
import java.util.jar.{Attributes, JarFile}

import scala.build.internal.zip.WrappedZipInputStream
import scala.build.{Logger, retry}

object MainClass {

  private def stringArrayDescriptor = "([Ljava/lang/String;)V"

  private class MainMethodChecker extends asm.ClassVisitor(asm.Opcodes.ASM9) {
    private var foundMainClass = false
    private var nameOpt        = Option.empty[String]
    def found: Boolean         = foundMainClass
    override def visit(
      version: Int,
      access: Int,
      name: String,
      signature: String,
      superName: String,
      interfaces: Array[String]
    ): Unit = {
      nameOpt = Some(name.replace('/', '.').replace('\\', '.'))
    }
    override def visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String,
      exceptions: Array[String]
    ): asm.MethodVisitor = {
      def isStatic = (access & asm.Opcodes.ACC_STATIC) != 0
      if (name == "main" && descriptor == stringArrayDescriptor && isStatic)
        foundMainClass = true
      null
    }
    def mainClassOpt: Option[String] =
      if (foundMainClass) nameOpt else None
  }

  private def findInClass(path: os.Path, logger: Logger): Iterator[String] =
    try {
      val is = retry()(logger)(os.read.inputStream(path))
      findInClass(is, logger)
    }
    catch {
      case e: NoSuchFileException =>
        e.getStackTrace.foreach(ste => logger.debug(ste.toString))
        logger.log(s"Class file $path not found: $e")
        logger.log("Are you trying to run too many builds at once? Trying to recover...")
        Iterator.empty
    }
  private def findInClass(is: InputStream, logger: Logger): Iterator[String] =
    try retry()(logger) {
        val reader  = new ClassReader(is)
        val checker = new MainMethodChecker
        reader.accept(checker, 0)
        checker.mainClassOpt.iterator
      }
    catch {
      case e: ArrayIndexOutOfBoundsException =>
        e.getStackTrace.foreach(ste => logger.debug(ste.toString))
        logger.log(s"Class input stream could not be created: $e")
        logger.log("Are you trying to run too many builds at once? Trying to recover...")
        Iterator.empty
    }
    finally is.close()

  private def findInJar(path: os.Path, logger: Logger): Iterator[String] =
    try retry()(logger) {
        val content        = os.read.bytes(path)
        val jarInputStream = WrappedZipInputStream.create(new ByteArrayInputStream(content))
        jarInputStream.entries().flatMap(ent =>
          if !ent.isDirectory && ent.getName.endsWith(".class") then {
            val content     = jarInputStream.readAllBytes()
            val inputStream = new ByteArrayInputStream(content)
            findInClass(inputStream, logger)
          }
          else Iterator.empty
        )
      }
    catch {
      case e: NoSuchFileException =>
        logger.debugStackTrace(e)
        logger.log(s"JAR file $path not found: $e, trying to recover...")
        logger.log("Are you trying to run too many builds at once? Trying to recover...")
        Iterator.empty
    }

  def findInDependency(jar: os.Path): Option[String] =
    jar match {
      case jar if os.isFile(jar) && jar.last.endsWith(".jar") =>
        for {
          manifest          <- Option(new JarFile(jar.toIO).getManifest)
          mainAttributes    <- Option(manifest.getMainAttributes)
          mainClass: String <- Option(mainAttributes.getValue(Attributes.Name.MAIN_CLASS))
        } yield mainClass
      case _ => None
    }

  def find(output: os.Path, logger: Logger): Seq[String] =
    output match {
      case o if os.isFile(o) && o.last.endsWith(".class") =>
        findInClass(o, logger).toVector
      case o if os.isFile(o) && o.last.endsWith(".jar") =>
        findInJar(o, logger).toVector
      case o if os.isDir(o) =>
        os.walk(o)
          .iterator
          .filter(os.isFile)
          .flatMap {
            case classFilePath if classFilePath.last.endsWith(".class") =>
              findInClass(classFilePath, logger)
            case _ => Iterator.empty
          }
          .toVector
      case _ => Vector.empty
    }
}
