package scala.build.testrunner

import sbt.testing._

import java.lang.annotation.Annotation
import java.lang.reflect.Modifier
import java.nio.file.{Files, Path}
import java.util.ServiceLoader

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

object DynamicTestRunner {

  // adapted from https://github.com/com-lihaoyi/mill/blob/ab4d61a50da24fb7fac97c4453dd8a770d8ac62b/scalalib/src/Lib.scala#L156-L172
  private def matchFingerprints(
    loader: ClassLoader,
    cls: Class[_],
    fingerprints: Array[Fingerprint]
  ): Option[Fingerprint] = {
    val isModule               = cls.getName.endsWith("$")
    val publicConstructorCount = cls.getConstructors.count(c => Modifier.isPublic(c.getModifiers))
    val noPublicConstructors   = publicConstructorCount == 0
    val definitelyNoTests = Modifier.isAbstract(cls.getModifiers) ||
      cls.isInterface ||
      publicConstructorCount > 1 ||
      isModule != noPublicConstructors
    if (definitelyNoTests)
      None
    else
      fingerprints.find {
        case f: SubclassFingerprint =>
          f.isModule == isModule &&
          loader.loadClass(f.superclassName())
            .isAssignableFrom(cls)

        case f: AnnotatedFingerprint =>
          val annotationCls = loader.loadClass(f.annotationName())
            .asInstanceOf[Class[Annotation]]
          f.isModule == isModule && (
            cls.isAnnotationPresent(annotationCls) ||
            cls.getDeclaredMethods.exists(_.isAnnotationPresent(annotationCls)) ||
            cls.getMethods.exists { m =>
              m.isAnnotationPresent(annotationCls) &&
              Modifier.isPublic(m.getModifiers())
            }
          )
      }
  }

  def listClasses(classPathEntry: Path, keepJars: Boolean): Iterator[String] =
    if (Files.isDirectory(classPathEntry)) {
      var stream: java.util.stream.Stream[Path] = null
      try {
        stream = Files.walk(classPathEntry, Int.MaxValue)
        stream
          .iterator
          .asScala
          .filter(_.getFileName.toString.endsWith(".class"))
          .map(classPathEntry.relativize(_))
          .map { p =>
            val count = p.getNameCount
            (0 until count).map(p.getName).mkString(".")
          }
          .map(_.stripSuffix(".class"))
          .toVector // fully consume stream before closing it
          .iterator
      }
      finally if (stream != null) stream.close()
    }
    else if (keepJars && Files.isRegularFile(classPathEntry)) {
      import java.util.zip._
      var zf: ZipFile = null
      try {
        zf = new ZipFile(classPathEntry.toFile)
        zf.entries
          .asScala
          // FIXME Check if these are files too
          .filter(_.getName.endsWith(".class"))
          .map(ent => ent.getName.stripSuffix(".class").replace("/", "."))
          .toVector // full consume ZipFile before closing it
          .iterator
      }
      finally if (zf != null) zf.close()
    }
    else Iterator.empty

  def listClasses(classPath: Seq[Path], keepJars: Boolean): Iterator[String] =
    classPath.iterator.flatMap(listClasses(_, keepJars))

  def findFrameworkService(loader: ClassLoader): Option[Framework] =
    ServiceLoader.load(classOf[Framework], loader)
      .iterator()
      .asScala
      .take(1)
      .toList
      .headOption

  def loadFramework(
    loader: ClassLoader,
    className: String
  ): Framework = {
    val cls         = loader.loadClass(className)
    val constructor = cls.getConstructor()
    constructor.newInstance().asInstanceOf[Framework]
  }

  def findFramework(
    classPath: Seq[Path],
    loader: ClassLoader,
    preferredClasses: Seq[String]
  ): Option[Framework] = {
    val frameworkCls = classOf[Framework]
    (preferredClasses.iterator ++ listClasses(classPath, true))
      .flatMap { name =>
        val it: Iterator[Class[_]] =
          try Iterator(loader.loadClass(name))
          catch {
            case _: ClassNotFoundException | _: UnsupportedClassVersionError | _: NoClassDefFoundError | _: IncompatibleClassChangeError =>
              Iterator.empty
          }
        it
      }
      .flatMap { cls =>
        def isAbstract = Modifier.isAbstract(cls.getModifiers)
        def publicConstructorCount =
          cls.getConstructors.count { c =>
            Modifier.isPublic(c.getModifiers) && c.getParameterCount() == 0
          }
        val it: Iterator[Class[_]] =
          if (frameworkCls.isAssignableFrom(cls) && !isAbstract && publicConstructorCount == 1)
            Iterator(cls)
          else
            Iterator.empty
        it
      }
      .flatMap { cls =>
        try {
          val constructor = cls.getConstructor()
          Iterator(constructor.newInstance().asInstanceOf[Framework])
        }
        catch {
          case _: NoSuchMethodException => Iterator.empty
        }
      }
      .take(1)
      .toList
      .headOption
  }

  def main(args: Array[String]): Unit = {

    val (testFrameworkOpt, requireTests, verbosity, args0) = {
      @tailrec
      def parse(
        testFrameworkOpt: Option[String],
        reverseTestArgs: List[String],
        requireTests: Boolean,
        verbosity: Int,
        args: List[String]
      ): (Option[String], Boolean, Int, List[String]) =
        args match {
          case Nil => (testFrameworkOpt, requireTests, verbosity, reverseTestArgs.reverse)
          case "--" :: t =>
            (testFrameworkOpt, requireTests, verbosity, reverseTestArgs.reverse ::: t)
          case h :: t if h.startsWith("--test-framework=") =>
            parse(
              Some(h.stripPrefix("--test-framework=")),
              reverseTestArgs,
              requireTests,
              verbosity,
              t
            )
          case h :: t if h.startsWith("--verbosity=") =>
            parse(
              testFrameworkOpt,
              reverseTestArgs,
              requireTests,
              h.stripPrefix("--verbosity=").toInt,
              t
            )
          case "--require-tests" :: t =>
            parse(testFrameworkOpt, reverseTestArgs, true, verbosity, t)
          case h :: t => parse(testFrameworkOpt, h :: reverseTestArgs, requireTests, verbosity, t)
        }

      parse(None, Nil, false, 0, args.toList)
    }

    val classLoader = Thread.currentThread().getContextClassLoader
    val classPath0  = TestRunner.classPath(classLoader)
    val framework = testFrameworkOpt.map(loadFramework(classLoader, _))
      .orElse(findFrameworkService(classLoader))
      .orElse(findFramework(classPath0, classLoader, TestRunner.commonTestFrameworks))
      .getOrElse {
        if (verbosity >= 2)
          sys.error("No test framework found")
        else {
          System.err.println("No test framework found")
          sys.exit(1)
        }
      }
    def classes = {
      val keepJars = false // look into dependencies, much slower
      listClasses(classPath0, keepJars).map(name => classLoader.loadClass(name))
    }
    val out = System.out

    val fingerprints = framework.fingerprints()
    val runner       = framework.runner(args0.toArray, Array(), classLoader)
    def clsFingerprints = classes.flatMap { cls =>
      matchFingerprints(classLoader, cls, fingerprints)
        .map((cls, _))
        .iterator
    }
    val taskDefs = clsFingerprints
      .map {
        case (cls, fp) =>
          new TaskDef(cls.getName.stripSuffix("$"), fp, false, Array(new SuiteSelector))
      }
      .toVector
    val initialTasks = runner.tasks(taskDefs.toArray)
    val events       = TestRunner.runTasks(initialTasks, out)
    val failed = events.exists { ev =>
      ev.status == Status.Error ||
      ev.status == Status.Failure ||
      ev.status == Status.Canceled
    }
    val doneMsg = runner.done()
    if (doneMsg.nonEmpty)
      out.println(doneMsg)
    if (requireTests && events.isEmpty) {
      System.err.println("Error: no tests were run.")
      sys.exit(1)
    }
    if (failed)
      sys.exit(1)
  }
}

abstract class DynamicTestRunner
