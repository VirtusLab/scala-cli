package scala.build.internal

import org.objectweb.asm
import org.objectweb.asm.ClassReader

import java.io.{ByteArrayInputStream, InputStream}
import java.util.zip.ZipEntry

import scala.build.Inputs.{Element, resolve}
import scala.build.internal.zip.WrappedZipInputStream

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

  def findInClass(path: os.Path): Iterator[String] =
    findInClass(os.read.inputStream(path))
  def findInClass(is: InputStream): Iterator[String] =
    try {
      val reader  = new ClassReader(is)
      val checker = new MainMethodChecker
      reader.accept(checker, 0)
      checker.mainClassOpt.iterator
    }
    finally is.close()

  def findInJar(path: os.Path): Iterator[String] = {
    val content        = os.read.bytes(path)
    val jarInputStream = WrappedZipInputStream.create(new ByteArrayInputStream(content))
    jarInputStream.entries().flatMap(ent =>
      if !ent.isDirectory && ent.getName.endsWith(".class") then {
        val content     = jarInputStream.readAllBytes()
        val inputStream = new ByteArrayInputStream(content)
        findInClass(inputStream)
      }
      else Iterator.empty
    )
  }

  def find(output: os.Path): Seq[String] =
    output match {
      case o if os.isFile(o) && o.last.endsWith(".class") =>
        findInClass(o).toVector
      case o if os.isFile(o) && o.last.endsWith(".jar") =>
        findInJar(o).toVector
      case o if os.isDir(o) =>
        os.walk(o)
          .iterator
          .filter(os.isFile)
          .flatMap {
            case classFilePath if classFilePath.last.endsWith(".class") =>
              findInClass(classFilePath)
            case _ => Iterator.empty
          }
          .toVector
      case _ => Vector.empty
    }
}
