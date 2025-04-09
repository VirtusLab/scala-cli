package scala.build.testrunner

import sbt.testing.Framework

object FrameworkUtils {
  implicit class TestFrameworkOps(val framework: Framework) {
    def description: String =
      s"${framework.name()} (${Option(framework.getClass.getCanonicalName).getOrElse(framework.toString)})"
  }

}
