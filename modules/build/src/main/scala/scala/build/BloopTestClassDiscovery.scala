package scala.build

import java.nio.file.Path
import java.util.regex.Pattern

import scala.build.testrunner.FrameworkUtils.listClasses
import scala.build.testrunner.Logger as TestRunnerLogger

object BloopTestClassDiscovery {

  /** Glob pattern matching only `*` wildcards, same semantics as the test-runner. */
  def globPattern(expr: String): Pattern = {
    val parts = expr.split("\\*", -1)
    val b     = new StringBuilder()
    for (i <- parts.indices) {
      if (i != 0) b.append(".*")
      if (parts(i).nonEmpty) b.append(Pattern.quote(parts(i).replaceAll("\n", "\\n")))
    }
    Pattern.compile(b.toString)
  }

  def matchingTestClasses(
    classPath: Seq[Path],
    testOnlyGlob: String,
    logger: Logger
  ): Seq[String] = {
    val pattern = globPattern(testOnlyGlob)
    listClasses(classPath, keepJars = false, TestRunnerLogger(logger.verbosity))
      .filter(pattern.matcher(_).matches)
      .toVector
      .sorted
  }
}
