package scala.build.testrunner

import org.objectweb.asm
import sbt.testing._

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.ConcurrentHashMap

import scala.jdk.CollectionConverters._

object AsmTestRunner {

  class ParentInspector(classPath: Seq[Path]) {

    private val cache = new ConcurrentHashMap[String, Seq[String]]

    def parents(className: String): Seq[String] =
      Option(cache.get(className)) match {
        case Some(value) => value
        case None =>
          val byteCodeOpt = findInClassPath(classPath, className + ".class")
          val parents = byteCodeOpt match {
            case None => Nil
            case Some(b) =>
              val reader  = new asm.ClassReader(new ByteArrayInputStream(b))
              val checker = new TestClassChecker
              reader.accept(checker, 0)
              checker.implements
          }

          cache.put(className, parents)
          parents
      }

    def allParents(className: String): Stream[String] = {

      def helper(done: Set[String], todo: List[String]): Stream[String] =
        todo match {
          case Nil => Stream.empty
          case h :: t =>
            if (done(h)) helper(done, t)
            else h #:: helper(done + h, parents(h).toList ::: t)
        }

      helper(Set.empty, className :: Nil)
    }

  }

  // originally adapted from https://github.com/com-lihaoyi/mill/blob/ab4d61a50da24fb7fac97c4453dd8a770d8ac62b/scalalib/src/Lib.scala#L156-L172
  def matchFingerprints(
    className: String,
    byteCode: () => InputStream,
    fingerprints: Seq[Fingerprint],
    parentInspector: ParentInspector
  ): Option[Fingerprint] = {

    val checker          = new TestClassChecker
    var is0: InputStream = null
    try {
      is0 = byteCode()
      val reader = new asm.ClassReader(is0)
      reader.accept(checker, 0)
    }
    finally if (is0 != null) is0.close()

    val isModule              = className.endsWith("$")
    val hasPublicConstructors = checker.publicConstructorCount > 0
    val definitelyNoTests = checker.isAbstract ||
      checker.isInterface ||
      checker.publicConstructorCount > 1 ||
      isModule == hasPublicConstructors
    if (definitelyNoTests)
      None
    else
      fingerprints.find {
        case f: SubclassFingerprint =>
          f.isModule == isModule &&
          parentInspector.allParents(checker.name)
            .contains(f.superclassName().replace('.', '/'))

        case _: AnnotatedFingerprint =>
          // val annotationCls = loader.loadClass(f.annotationName())
          //   .asInstanceOf[Class[Annotation]]
          // f.isModule == isModule && (
          //   cls.isAnnotationPresent(annotationCls) ||
          //   cls.getDeclaredMethods.exists(_.isAnnotationPresent(annotationCls)) ||
          //   cls.getMethods.exists(m => m.isAnnotationPresent(annotationCls) && Modifier.isPublic(m.getModifiers()))
          // )
          ???
      }
  }

  def listClassesByteCode(
    classPathEntry: Path,
    keepJars: Boolean
  ): Iterator[(String, () => InputStream)] =
    if (Files.isDirectory(classPathEntry)) {
      var stream: java.util.stream.Stream[Path] = null
      try {
        stream = Files.walk(classPathEntry, Int.MaxValue)
        stream
          .iterator
          .asScala
          .filter(_.getFileName.toString.endsWith(".class"))
          .map(_.toAbsolutePath)
          .map { p =>
            val clsName      = classPathEntry.relativize(p).toString.stripSuffix(".class")
            def openStream() = Files.newInputStream(p)
            (clsName, openStream _)
          }
          .toVector // fully consume stream before closing it
          .iterator
      }
      finally if (stream != null) stream.close()
    }
    else if (keepJars && Files.isRegularFile(classPathEntry)) {
      import java.util.zip._
      val buf         = Array.ofDim[Byte](16384)
      var zf: ZipFile = null
      try {
        zf = new ZipFile(classPathEntry.toFile)
        zf.entries
          .asScala
          // FIXME Check if these are files too
          .filter(_.getName.endsWith(".class"))
          .map { ent =>
            val baos            = new ByteArrayOutputStream
            var is: InputStream = null
            try {
              is = zf.getInputStream(ent)
              var read = -1
              while ({
                read = is.read(buf)
                read >= 0
              }) baos.write(buf, 0, read)
              val clsName      = ent.getName.stripSuffix(".class")
              def openStream() = new ByteArrayInputStream(baos.toByteArray)
              (clsName, openStream _)
            }
            finally if (is != null) is.close()
          }
          .toVector // fully consume ZipFile before closing it
          .iterator
      }
      finally if (zf != null) zf.close()
    }
    else Iterator.empty

  def listClassesByteCode(
    classPath: Seq[Path],
    keepJars: Boolean
  ): Iterator[(String, () => InputStream)] =
    classPath.iterator.flatMap(listClassesByteCode(_, keepJars))

  def findInClassPath(classPathEntry: Path, name: String): Option[Array[Byte]] =
    if (Files.isDirectory(classPathEntry)) {
      val p = classPathEntry.resolve(name)
      if (Files.isRegularFile(p)) Some(Files.readAllBytes(p))
      else None
    }
    else if (Files.isRegularFile(classPathEntry)) {
      import java.util.zip._
      val buf         = Array.ofDim[Byte](16384)
      var zf: ZipFile = null
      try {
        zf = new ZipFile(classPathEntry.toFile)
        Option(zf.getEntry(name)).map { ent =>
          val baos            = new ByteArrayOutputStream
          var is: InputStream = null
          try {
            is = zf.getInputStream(ent)
            var read = -1
            while ({
              read = is.read(buf)
              read >= 0
            }) baos.write(buf, 0, read)
            baos.toByteArray
          }
          finally if (is != null) is.close()
        }
      }
      finally if (zf != null) zf.close()
    }
    else None

  def findInClassPath(classPath: Seq[Path], name: String): Option[Array[Byte]] =
    classPath
      .iterator
      .flatMap(findInClassPath(_, name).iterator)
      .take(1)
      .toList
      .headOption

  def findFrameworkService(classPath: Seq[Path]): Option[String] =
    findInClassPath(classPath, "META-INF/services/sbt.testing.Framework").map { b =>
      new String(b, StandardCharsets.UTF_8)
    }

  def findFramework(
    classPath: Seq[Path],
    preferredClasses: Seq[String]
  ): Option[String] = {
    val parentInspector = new ParentInspector(classPath)
    findFramework(classPath, preferredClasses, parentInspector)
  }

  def findFramework(
    classPath: Seq[Path],
    preferredClasses: Seq[String],
    parentInspector: ParentInspector
  ): Option[String] = {
    val preferredClassesByteCode = preferredClasses
      .iterator
      .map(_.replace('.', '/'))
      .flatMap { name =>
        findInClassPath(classPath, name + ".class")
          .iterator
          .map { b =>
            def openStream() = new ByteArrayInputStream(b)
            (name, openStream _)
          }
      }
    (preferredClassesByteCode ++ listClassesByteCode(classPath, true))
      .flatMap {
        case ("module-info", _) => Iterator.empty
        case (name, is) =>
          val checker          = new TestClassChecker
          var is0: InputStream = null
          try {
            is0 = is()
            val reader = new asm.ClassReader(is0)
            reader.accept(checker, 0)
          }
          finally if (is0 != null) is0.close()
          val isFramework = parentInspector.allParents(name).contains("sbt/testing/Framework")
          if (isFramework && !checker.isAbstract && checker.publicConstructorCount == 1)
            Iterator(name)
          else
            Iterator.empty
      }
      .take(1)
      .toList
      .headOption
  }

  private class TestClassChecker extends asm.ClassVisitor(asm.Opcodes.ASM9) {
    private var nameOpt                 = Option.empty[String]
    private var publicConstructorCount0 = 0
    private var isInterfaceOpt          = Option.empty[Boolean]
    private var isAbstractOpt           = Option.empty[Boolean]
    private var implements0             = List.empty[String]
    def canBeTestSuite: Boolean = {
      val isModule = nameOpt.exists(_.endsWith("$"))
      !isAbstractOpt.contains(true) &&
      !isInterfaceOpt.contains(true) &&
      publicConstructorCount0 <= 1 &&
      isModule != (publicConstructorCount0 == 1)
    }
    def name                   = nameOpt.getOrElse(sys.error("Class not visited"))
    def publicConstructorCount = publicConstructorCount0
    def implements             = implements0
    def isAbstract             = isAbstractOpt.getOrElse(sys.error("Class not visited"))
    def isInterface            = isInterfaceOpt.getOrElse(sys.error("Class not visited"))
    override def visit(
      version: Int,
      access: Int,
      name: String,
      signature: String,
      superName: String,
      interfaces: Array[String]
    ): Unit = {
      isInterfaceOpt = Some((access & asm.Opcodes.ACC_INTERFACE) != 0)
      isAbstractOpt = Some((access & asm.Opcodes.ACC_ABSTRACT) != 0)
      nameOpt = Some(name)
      implements0 = superName :: implements0
      if (interfaces.nonEmpty)
        implements0 = interfaces.toList ::: implements0
    }
    override def visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String,
      exceptions: Array[String]
    ): asm.MethodVisitor = {
      def isPublic = (access & asm.Opcodes.ACC_PUBLIC) != 0
      if (name == "<init>" && isPublic)
        publicConstructorCount0 += 1
      null
    }
  }

  def taskDefs(
    classPath: Seq[Path],
    keepJars: Boolean,
    fingerprints: Seq[Fingerprint],
    parentInspector: ParentInspector
  ): Iterator[TaskDef] =
    listClassesByteCode(classPath, keepJars = keepJars)
      .flatMap {
        case (name, is) =>
          matchFingerprints(name, is, fingerprints, parentInspector)
            .map((name.stripSuffix("$"), _))
            .iterator
      }
      .map {
        case (clsName, fp) =>
          new TaskDef(
            clsName.replace('/', '.').replace('\\', '.'),
            fp,
            false,
            Array(new SuiteSelector)
          )
      }

  def main(args: Array[String]): Unit = {

    val classLoader = Thread.currentThread().getContextClassLoader
    val classPath   = TestRunner.classPath(classLoader)

    val parentCache = new ParentInspector(classPath)

    val frameworkClassName = findFrameworkService(classPath)
      .orElse(findFramework(classPath, TestRunner.commonTestFrameworks, parentCache))
      .getOrElse(sys.error("No test framework found"))
      .replace('/', '.')
      .replace('\\', '.')

    val framework = classLoader
      .loadClass(frameworkClassName)
      .getConstructor()
      .newInstance()
      .asInstanceOf[Framework]

    val out = System.out

    val taskDefs0 =
      taskDefs(
        classPath,
        keepJars = false,
        framework.fingerprints().toIndexedSeq,
        parentCache
      ).toArray

    val runner       = framework.runner(Array(), Array(), classLoader)
    val initialTasks = runner.tasks(taskDefs0)
    val events       = TestRunner.runTasks(initialTasks, out)

    val doneMsg = runner.done()
    if (doneMsg.nonEmpty)
      out.println(doneMsg)

    val failed = events.exists { ev =>
      ev.status == Status.Error ||
      ev.status == Status.Failure ||
      ev.status == Status.Canceled
    }
    if (failed)
      sys.exit(1)
  }
}

abstract class AsmTestRunner
