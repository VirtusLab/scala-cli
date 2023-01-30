package scala.cli.commands.run

import org.objectweb.asm
import org.objectweb.asm.ClassReader

import java.io.{ByteArrayInputStream, InputStream}
import java.util.zip.ZipEntry

import scala.build.input.Element
import scala.build.internal.zip.WrappedZipInputStream

object EntrypointDetails {

  private def stringArrayDescriptor       = "([Ljava/lang/String;)V"
  private def detailsAnnotationDescriptor = "Lcaseapp/internals/Details;"

  private class DetailsAnnotationVisitor extends asm.AnnotationVisitor(asm.Opcodes.ASM9) {
    private var detailsOpt0        = Option.empty[String]
    def detailsOpt: Option[String] = detailsOpt0
    override def visit(name: String, value: Object): Unit =
      (name, value) match {
        case ("details", strValue: String) =>
          detailsOpt0 = Some(strValue)
        case _ =>
      }
  }

  private class MainMethodVisitor extends asm.MethodVisitor(asm.Opcodes.ASM9) {
    private val detailsAnnotationVisitor = new DetailsAnnotationVisitor
    def detailsOpt: Option[String] =
      detailsAnnotationVisitor.detailsOpt
    override def visitAnnotation(descriptor: String, visible: Boolean): asm.AnnotationVisitor =
      if (descriptor == detailsAnnotationDescriptor)
        detailsAnnotationVisitor
      else
        null
  }

  private class EntrypointDetailsChecker extends asm.ClassVisitor(asm.Opcodes.ASM9) {
    private val mainMethodVisitor = new MainMethodVisitor
    override def visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String,
      exceptions: Array[String]
    ): asm.MethodVisitor = {
      def isStatic = (access & asm.Opcodes.ACC_STATIC) != 0
      if (name == "main" && descriptor == stringArrayDescriptor && isStatic)
        mainMethodVisitor
      else
        null
    }
    def detailsOpt: Option[String] =
      mainMethodVisitor.detailsOpt
  }

  def visit(is: InputStream): Option[String] =
    try {
      val reader  = new ClassReader(is)
      val checker = new EntrypointDetailsChecker
      reader.accept(checker, 0)
      checker.detailsOpt
    }
    finally is.close()

  def lookInto(elem: os.Path, mainClass: os.RelPath): Option[String] =
    if (os.isDir(elem)) {
      val classFile = elem / mainClass
      if (os.isFile(classFile))
        visit(os.read.inputStream(classFile))
      else
        None
    }
    else if (os.isFile(elem)) {
      val zis = WrappedZipInputStream.create(os.read.inputStream(elem))
      zis.entries()
        .flatMap { ent =>
          if (ent.getName == mainClass.toString) {
            val b = zis.readAllBytes()
            visit(new ByteArrayInputStream(b)).iterator
          }
          else
            Iterator.empty
        }
        .take(1)
        .toList
        .headOption
    }
    else
      None

  def find(classPath: Seq[os.Path], mainClass: String): Option[String] = {
    val mainClassPath = {
      val elems = mainClass.split('.')
      assert(elems.nonEmpty)
      os.rel / elems.init.toSeq / s"${elems.last}.class"
    }
    classPath
      .iterator
      .flatMap { elem =>
        lookInto(elem, mainClassPath).iterator
      }
      .take(1)
      .toList
      .headOption
  }

  final case class CliOption(names: ::[String], help: Option[String])

  def parse(details: String): Seq[CliOption] =
    details
      .linesIterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap { line =>
        val (strNames, help) = line.split("\\|", 2) match {
          case Array(strNames0)        => (strNames0, "")
          case Array(strNames0, help0) => (strNames0, help0)
        }

        val names = strNames.split(',').map(_.trim).filter(_.nonEmpty).toList
        names match {
          case Nil => Nil // shouldn't happen
          case h :: t =>
            List(CliOption(::(h, t), Some(help).filter(_.nonEmpty)))
        }
      }
      .toVector
}
