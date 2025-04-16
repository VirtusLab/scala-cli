package scala.build.testrunner

import sbt.testing.{Logger => _, _}

import java.util.regex.Pattern

import scala.annotation.tailrec
import scala.build.testrunner.FrameworkUtils._

object DynamicTestRunner {

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
        getFrameworksToRun(
          frameworkServices = findFrameworkServices(classLoader),
          frameworks = findFrameworks(classPath0, classLoader, TestRunner.commonTestFrameworks)
        )(logger) match {
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
