//> using lib "org.ow2.asm:asm:9.2"
//> using lib "com.lihaoyi:os-lib_2.13:0.8.0"
//> using lib "org.scala-sbt::io:1.6.0"
//> using scala "3.1.1"

import java.io.File
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.MethodVisitor
import scala.reflect.NameTransformer.OpCodes
import sbt.io.IO
import org.objectweb.asm.ClassWriter
import os.write

def processJar(jarPath: String) =
  val path = os.pwd / jarPath

class StaticInitVistor(parent: MethodVisitor) extends MethodVisitor(Opcodes.ASM9, parent):
  override def visitMethodInsn(opcode: Int, owner: String, name: String, descr: String, isInterface: Boolean): Unit =
    if owner == "scala/runtime/LazyVals$" && name == "getOffset" then
      parent.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false)
      parent.visitMethodInsn(Opcodes.INVOKESTATIC, "runtime/BetterLazyVal","getOffset", "(Ljava/lang/Object;Ljava/lang/reflect/Field;)J", false)
    else 
      parent.visitMethodInsn(opcode, owner, name, descr, isInterface)

class LazyValVisitor(writer: ClassWriter) extends ClassVisitor(Opcodes.ASM9, writer):

  var changed = false
  override def visitMethod(access: Int, name: String, desc: String, sig: String, exceptions: Array[String]): MethodVisitor =
    if name == "<clinit>" then 
      changed = true
      new StaticInitVistor(writer.visitMethod(access, name, desc, sig, exceptions))
    else 
      writer.visitMethod(access, name, desc, sig, exceptions)

def processClassFile(bytes: Array[Byte], path: os.RelPath): Option[Array[Byte]] =
  val reader = new ClassReader(bytes)
  val writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
  val visitor = new LazyValVisitor(writer)
  val res =
    util.Try(reader.accept(visitor, 0))
  if visitor.changed && res.isSuccess then Some(writer.toByteArray) else None


def processAndCreateJar(path: os.Path, dest: os.Path, original: os.Path): Boolean = 
  var processed = Seq.newBuilder[String]
  val manifestField = path / "META-INF" / "MANIFEST.MF"
  val containsSHA = os.exists(manifestField) && os.read(manifestField).contains("SHA-256-Digest:")
  if containsSHA then println(s"Ignoring path: $original")
  val mappings = os.walk(path).map { file =>
    val relPath = file.relativeTo(path).toString
    if file.last.endsWith(".class") && !containsSHA then 
      processClassFile(os.read.bytes(file), file.relativeTo(path))
        .foreach{ newCode => 
          processed += relPath
          // println("Processing: " + path + " with " + file)
          os.write.over(file, newCode)
    }
    (file.toIO, relPath)
  }
  IO.zip(mappings, dest.toIO, None)
  val aList = processed.result
  if aList.nonEmpty then 
    os.write(dest / os.up / (dest.last + ".processed.txt"), aList.mkString("\n"))
  aList.nonEmpty

def toPath(str: String) = os.FilePath(str) match 
  case p: os.Path => p
  case r: os.RelPath => os.pwd / r
  case s: os.SubPath => os.pwd / s
  

@main def process(classpath: String, out: String) =
  val outFile = toPath(out)
  os.remove.all(outFile)
  assert(runtime.BetterLazyVal != null)
  val statuses = classpath.split(File.pathSeparator).zipWithIndex.map {
    case (entry, index) => 
      val from = toPath(entry)
      if os.exists(from) then
        val dest = os.temp.dir()
        try 
          if (entry.endsWith(".jar")) IO.unzip(from.toIO, dest.toIO)
          else 
            val copied = dest / "copied"
            os.copy.over(from, dest, createFolders = true)
          processAndCreateJar(dest, outFile / s"cp-${1000 + index}.jar", from)
        finally os.remove.all(dest)
      else 
        println(s"Skipping $entry")
        false
  }
