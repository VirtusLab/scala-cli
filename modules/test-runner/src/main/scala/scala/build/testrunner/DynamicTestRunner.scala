package scala.build.testrunner

import sbt.testing.{Logger => _, _}

import java.lang.annotation.Annotation
import java.lang.reflect.Modifier
import java.nio.file.{Files, Path}
import java.util.ServiceLoader
import java.util.regex.Pattern

import scala.annotation.tailrec
import scala.build.testrunner.FrameworkUtils._
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
      .toSeq
  }

  /** Based on junit-interface [GlobFilter.
    * compileGlobPattern](https://github.com/sbt/junit-interface/blob/f8c6372ed01ce86f15393b890323d96afbe6d594/src/main/java/com/novocode/junit/GlobFilter.java#L37)
    *
    * @return
    *   Pattern allows to regex input which contains only *, for example `*foo*` match to
    *   `MyTests.foo`
    */
  private def globPattern(expr: String): Pattern = {
    val a = expr.split("\\*", -1)
    val b = new StringBuilder()
    for (i <- 0 until a.length) {
      if (i != 0) b.append(".*")
      if (a(i).nonEmpty) b.append(Pattern.quote(a(i).replaceAll("\n", "\\n")))
    }
    Pattern.compile(b.toString)
  }

  def main(args: Array[String]): Unit = {

    val (testFrameworkOpt, requireTests, verbosity, testOnly, args0) = {
      @tailrec
      def parse(
        testFrameworkOpt: Option[String],
        reverseTestArgs: List[String],
        requireTests: Boolean,
        verbosity: Int,
        testOnly: Option[String],
        args: List[String]
      ): (Option[String], Boolean, Int, Option[String], List[String]) =
        args match {
          case Nil => (testFrameworkOpt, requireTests, verbosity, testOnly, reverseTestArgs.reverse)
          case "--" :: t =>
            (testFrameworkOpt, requireTests, verbosity, testOnly, reverseTestArgs.reverse ::: t)
          case h :: t if h.startsWith("--test-framework=") =>
            parse(
              Some(h.stripPrefix("--test-framework=")),
              reverseTestArgs,
              requireTests,
              verbosity,
              testOnly,
              t
            )
          case h :: t if h.startsWith("--test-only=") =>
            parse(
              testFrameworkOpt,
              reverseTestArgs,
              requireTests,
              verbosity,
              Some(h.stripPrefix("--test-only=")),
              t
            )
          case h :: t if h.startsWith("--verbosity=") =>
            parse(
              testFrameworkOpt,
              reverseTestArgs,
              requireTests,
              h.stripPrefix("--verbosity=").toInt,
              testOnly,
              t
            )
          case "--require-tests" :: t =>
            parse(testFrameworkOpt, reverseTestArgs, true, verbosity, testOnly, t)
          case h :: t =>
            parse(testFrameworkOpt, h :: reverseTestArgs, requireTests, verbosity, testOnly, t)
        }

      parse(None, Nil, false, 0, None, args.toList)
    }

    val logger = Logger(verbosity)

    val classLoader = Thread.currentThread().getContextClassLoader
    val classPath0  = TestRunner.classPath(classLoader)
    val frameworks = testFrameworkOpt
      .map(loadFramework(classLoader, _))
      .map(Seq(_))
      .getOrElse {
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

        val foundFrameworkServices = findFrameworkServices(classLoader)
        if (foundFrameworkServices.nonEmpty)
          logger.debug(
            s"""Found test framework services:
               |  - ${foundFrameworkServices.map(_.description).mkString("\n  - ")}
               |""".stripMargin
          )

        val foundFrameworks =
          findFrameworks(classPath0, classLoader, TestRunner.commonTestFrameworks)
        if (foundFrameworks.nonEmpty)
          logger.debug(
            s"""Found test frameworks:
               |  - ${foundFrameworks.map(_.description).mkString("\n  - ")}
               |""".stripMargin
          )

        val distinctFrameworks = distinctBy(foundFrameworkServices ++ foundFrameworks)(_.name())
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

        finalFrameworks match {
          case f if f.nonEmpty     => f
          case _ if verbosity >= 2 => sys.error("No test framework found")
          case _ =>
            System.err.println("No test framework found")
            sys.exit(1)
        }
      }
    def classes = {
      val keepJars = false // look into dependencies, much slower
      listClasses(classPath0, keepJars).map(name => classLoader.loadClass(name))
    }
    val out = System.out

    val exitCodes =
      frameworks
        .map { framework =>
          logger.log(s"Running test framework: ${framework.name}")
          val fingerprints = framework.fingerprints()
          val runner       = framework.runner(args0.toArray, Array(), classLoader)

          def clsFingerprints = classes.flatMap { cls =>
            matchFingerprints(classLoader, cls, fingerprints)
              .map((cls, _))
              .iterator
          }

          val taskDefs = clsFingerprints
            .filter {
              case (cls, _) =>
                testOnly.forall(pattern =>
                  globPattern(pattern).matcher(cls.getName.stripSuffix("$")).matches()
                )
            }
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
          if (doneMsg.nonEmpty) out.println(doneMsg)
          if (requireTests && events.isEmpty) {
            logger.error(s"Error: no tests were run for ${framework.name()}.")
            1
          }
          else if (failed) {
            logger.error(s"Error: ${framework.name()} tests failed.")
            1
          }
          else {
            logger.log(s"${framework.name()} tests ran successfully.")
            0
          }
        }
    if (exitCodes.contains(1)) sys.exit(1)
    else sys.exit(0)
  }
}

abstract class DynamicTestRunner
