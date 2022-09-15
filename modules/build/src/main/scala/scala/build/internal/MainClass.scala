package scala.build.internal

import org.objectweb.asm
import org.objectweb.asm.ClassReader

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

  def findInClass(path: os.Path): Iterator[String] = {
    val is = os.read.inputStream(path)
    try {
      val reader  = new ClassReader(is)
      val checker = new MainMethodChecker
      reader.accept(checker, 0)
      checker.mainClassOpt.iterator
    }
    finally is.close()
  }
  def find(output: os.Path): Seq[String] =
    output match {
      case o if os.isFile(o) && o.last.endsWith(".class") =>
        findInClass(o).toVector
      case o if os.isDir(o) =>
        os.walk(o)
          .iterator
          .filter(os.isFile(_))
          .filter(_.last.endsWith(".class"))
          .flatMap(findInClass)
          .toVector
      case _ => Vector.empty
    }
}
