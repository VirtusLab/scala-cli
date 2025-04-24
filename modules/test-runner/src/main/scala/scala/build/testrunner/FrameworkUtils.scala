package scala.build.testrunner

import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint}

import java.lang.annotation.Annotation
import java.lang.reflect.Modifier
import java.nio.file.{Files, Path}
import java.util.ServiceLoader

import scala.jdk.CollectionConverters._

object FrameworkUtils {
  // needed for Scala 2.12
  def distinctBy[A, B](seq: Seq[A])(f: A => B): Seq[A] = {
    @annotation.tailrec
    def loop(remaining: Seq[A], seen: Set[B], acc: Vector[A]): Vector[A] =
      if (remaining.isEmpty) acc
      else {
        val head = remaining.head
        val tail = remaining.tail
        val key  = f(head)
        if (seen(key)) loop(tail, seen, acc)
        else loop(tail, seen + key, acc :+ head)
      }

    loop(seq, Set.empty, Vector.empty)
  }

  implicit class TestFrameworkOps(val framework: Framework) {
    def description: String =
      s"${framework.name()} (${Option(framework.getClass.getCanonicalName).getOrElse(framework.toString)})"
  }

  def getFrameworksToRun(allFrameworks: Seq[Framework])(logger: Logger): Seq[Framework] = {
    val distinctFrameworks = distinctBy(allFrameworks)(_.name())
    if (distinctFrameworks.nonEmpty)
      logger.debug(
        s"""Distinct test frameworks found (by framework name):
           |  - ${distinctFrameworks.map(_.description).mkString("\n  - ")}
           |""".stripMargin
      )

    val finalFrameworks =
      distinctFrameworks
        .filter(f1 =>
          !distinctFrameworks
            .filter(_ != f1)
            .exists(f2 =>
              f1.getClass.isAssignableFrom(f2.getClass)
            )
        )
    if (finalFrameworks.nonEmpty)
      logger.log(
        s"""Final list of test frameworks found:
           |  - ${finalFrameworks.map(_.description).mkString("\n  - ")}
           |""".stripMargin
      )

    val skippedInheritedFrameworks = distinctFrameworks.diff(finalFrameworks)
    if (skippedInheritedFrameworks.nonEmpty)
      logger.log(
        s"""The following test frameworks have been filtered out, as they're being inherited from by others:
           |  - ${skippedInheritedFrameworks.map(_.description).mkString("\n  - ")}
           |""".stripMargin
      )

    finalFrameworks
  }

  def getFrameworksToRun(
    frameworkServices: Seq[Framework],
    frameworks: Seq[Framework]
  )(logger: Logger): Seq[Framework] = {
    if (frameworkServices.nonEmpty)
      logger.debug(
        s"""Found test framework services:
           |  - ${frameworkServices.map(_.description).mkString("\n  - ")}
           |""".stripMargin
      )
    if (frameworks.nonEmpty)
      logger.debug(
        s"""Found test frameworks:
           |  - ${frameworks.map(_.description).mkString("\n  - ")}
           |""".stripMargin
      )

    getFrameworksToRun(allFrameworks = frameworkServices ++ frameworks)(logger)
  }

  def listClasses(classPath: Seq[Path], keepJars: Boolean): Iterator[String] =
    classPath.iterator.flatMap(listClasses(_, keepJars))

  def listClasses(classPathEntry: Path, keepJars: Boolean): Iterator[String] =
    if (Files.isDirectory(classPathEntry)) {
      var stream: java.util.stream.Stream[Path] = null
      try {
        stream = Files.walk(classPathEntry, Int.MaxValue)
        stream
          .iterator
          .asScala
          .filter(_.getFileName.toString.endsWith(".class"))
          .map(classPathEntry.relativize)
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

  def findFrameworkServices(loader: ClassLoader): Seq[Framework] =
    ServiceLoader.load(classOf[Framework], loader)
      .iterator()
      .asScala
      .toSeq

  def loadFramework(
    loader: ClassLoader,
    className: String
  ): Framework = {
    val cls         = loader.loadClass(className)
    val constructor = cls.getConstructor()
    constructor.newInstance().asInstanceOf[Framework]
  }

  def findFrameworks(
    classPath: Seq[Path],
    loader: ClassLoader,
    preferredClasses: Seq[String]
  ): Seq[Framework] = {
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
            Modifier.isPublic(c.getModifiers) && c.getParameterCount == 0
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
      .toSeq
  }

  // adapted from https://github.com/com-lihaoyi/mill/blob/ab4d61a50da24fb7fac97c4453dd8a770d8ac62b/scalalib/src/Lib.scala#L156-L172
  def matchFingerprints(
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
              Modifier.isPublic(m.getModifiers)
            }
          )
      }
  }
}
