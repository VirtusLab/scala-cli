package scala.build.tests

import org.objectweb.asm.{ClassWriter, Opcodes}
import sbt.testing.{AnnotatedFingerprint, Fingerprint}

import java.io.ByteArrayInputStream
import java.nio.file.Files

import scala.build.errors.NoFrameworkFoundByNativeBridgeError
import scala.build.testrunner.{AsmTestRunner, Logger as TestRunnerLogger}

class FrameworkDiscoveryTests extends TestUtil.ScalaCliBuildSuite {

  private val testAnnotation     = "my.Test"
  private val testAnnotationDesc = s"L${testAnnotation.replace('.', '/')};"

  private def annotatedFingerprint: AnnotatedFingerprint = new AnnotatedFingerprint {
    override def annotationName(): String = testAnnotation
    override def isModule(): Boolean      = false
  }

  private def matchAnnotated(
    className: String,
    bytes: Array[Byte]
  ): Option[Fingerprint] = {
    val parentInspector = new AsmTestRunner.ParentInspector(Seq.empty, TestRunnerLogger(0))
    AsmTestRunner.matchFingerprints(
      className,
      () => new ByteArrayInputStream(bytes),
      Seq(annotatedFingerprint),
      parentInspector
    )
  }

  private def writePublicInit(cw: ClassWriter): Unit = {
    val init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
    init.visitCode()
    init.visitVarInsn(Opcodes.ALOAD, 0)
    init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
    init.visitInsn(Opcodes.RETURN)
    init.visitMaxs(1, 1)
    init.visitEnd()
  }

  private def newTestClassWriter: ClassWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)

  private def classWithClassAnnotation: Array[Byte] = {
    val cw = newTestClassWriter
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "pkg/MyTest", null, "java/lang/Object", null)
    cw.visitAnnotation(testAnnotationDesc, true).visitEnd()
    writePublicInit(cw)
    cw.visitEnd()
    cw.toByteArray
  }

  private def classWithMethodAnnotation: Array[Byte] = {
    val cw = newTestClassWriter
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "pkg/MyTest", null, "java/lang/Object", null)
    writePublicInit(cw)
    val method = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "()V", null, null)
    method.visitAnnotation(testAnnotationDesc, true).visitEnd()
    method.visitCode()
    method.visitInsn(Opcodes.RETURN)
    method.visitMaxs(1, 1)
    method.visitEnd()
    cw.visitEnd()
    cw.toByteArray
  }

  private def classWithoutAnnotations: Array[Byte] = {
    val cw = newTestClassWriter
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "pkg/MyTest", null, "java/lang/Object", null)
    writePublicInit(cw)
    cw.visitEnd()
    cw.toByteArray
  }

  test("matchFingerprints matches AnnotatedFingerprint on class-level annotation") {
    val matched = matchAnnotated("pkg/MyTest", classWithClassAnnotation)
    assert(matched.collect { case f: AnnotatedFingerprint => f.annotationName() }.contains(
      testAnnotation
    ))
  }

  test("matchFingerprints matches AnnotatedFingerprint on public method-level annotation") {
    val matched = matchAnnotated("pkg/MyTest", classWithMethodAnnotation)
    assert(matched.collect { case f: AnnotatedFingerprint => f.annotationName() }.contains(
      testAnnotation
    ))
  }

  test("matchFingerprints returns None when AnnotatedFingerprint annotation is absent") {
    assertEquals(
      matchAnnotated("pkg/MyTest", classWithoutAnnotations),
      None
    )
  }

  test(
    "findFrameworkServices parses Java ServiceLoader format (trim, skip comments and empty lines)"
  ) {
    val dir = Files.createTempDirectory("scala-cli-framework-services-")
    try {
      val servicesDir = dir.resolve("META-INF").resolve("services")
      Files.createDirectories(servicesDir)
      val serviceFile = servicesDir.resolve("sbt.testing.Framework")
      // Content with newlines, comments, and surrounding whitespace
      val content =
        """munit.Framework
          |# comment line
          |
          |  munit.native.Framework  
          |
          |""".stripMargin
      Files.writeString(serviceFile, content)

      val found = AsmTestRunner.findFrameworkServices(Seq(dir), TestRunnerLogger(0))
      assertEquals(
        found.sorted,
        Seq("munit.Framework", "munit.native.Framework"),
        clue = "Service file lines should be trimmed; comments and empty lines skipped"
      )
    }
    finally {
      def deleteRecursively(p: java.nio.file.Path): Unit = {
        if Files.isDirectory(p) then Files.list(p).forEach(deleteRecursively)
        Files.deleteIfExists(p)
      }
      deleteRecursively(dir)
    }
  }

  test("NoFrameworkFoundByNativeBridgeError has Native-specific message (not Scala.js)") {
    val err = new NoFrameworkFoundByNativeBridgeError
    assert(err.getMessage.contains("Scala Native"), clue = "Message should mention Scala Native")
    assert(!err.getMessage.contains("Scala.js"), clue = "Message should not mention Scala.js")
  }
}
