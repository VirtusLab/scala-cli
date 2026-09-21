package scala.build

import scala.build.internal.Constants
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix

class LegacyRunnerModuleTests extends munit.FunSuite {

  private val legacyScala3   = "3.2.2"
  private val defaultScala   = Constants.defaultScalaVersion
  private val scala213       = Constants.defaultScala213Version
  private val legacyLtsScala = s"${Constants.scala3LegacyLtsPrefix}.0"

  private def defaultingWarning(moduleName: String, version: String): String =
    s"Defaulting to a legacy $moduleName module version: $version."

  for (moduleName <- Seq("runner", "test-runner")) {
    test(s"$moduleName: no fallback for the default Scala version on the default JVM") {
      val res =
        Artifacts.legacyRunnerModule(moduleName, defaultScala, Constants.defaultJavaVersion)
      assertEquals(res, None)
    }

    test(s"$moduleName: no fallback on the minimum supported JVM") {
      val res =
        Artifacts.legacyRunnerModule(moduleName, defaultScala, Constants.minimumRunnerJavaVersion)
      assertEquals(res, None)
    }

    test(s"$moduleName: no fallback on the oldest LTS Scala version") {
      val res =
        Artifacts.legacyRunnerModule(moduleName, legacyLtsScala, Constants.defaultJavaVersion)
      assertEquals(res, None)
    }

    test(s"$moduleName: JVM older than the minimum falls back to the Java 8 legacy version") {
      val res = Artifacts.legacyRunnerModule(
        moduleName,
        defaultScala,
        Constants.minimumRunnerJavaVersion - 1
      )
      val legacy = res.getOrElse(fail("expected a legacy module"))
      assertEquals(legacy.version, Constants.runnerJava8LegacyVersion)
      assertEquals(
        legacy.warnings.map(_.stripPrefix(s"$warnPrefix ")),
        Seq(
          s"Java ${Constants.minimumRunnerJavaVersion - 1} is no longer supported by the $moduleName module.",
          defaultingWarning(moduleName, Constants.runnerJava8LegacyVersion),
          s"To use the latest $moduleName, upgrade Java to at least ${Constants.minimumRunnerJavaVersion}."
        )
      )
    }

    test(s"$moduleName: pre-LTS Scala 3 falls back to the legacy Scala 3 version") {
      val res =
        Artifacts.legacyRunnerModule(moduleName, legacyScala3, Constants.defaultJavaVersion)
      val legacy = res.getOrElse(fail("expected a legacy module"))
      assertEquals(legacy.version, Constants.runnerScala30LegacyVersion)
      assertEquals(
        legacy.warnings.map(_.stripPrefix(s"$warnPrefix ")),
        Seq(
          s"Scala $legacyScala3 is no longer supported by the $moduleName module.",
          defaultingWarning(moduleName, Constants.runnerScala30LegacyVersion),
          s"To use the latest $moduleName, upgrade Scala to at least ${Constants.scala3LegacyLtsPrefix}."
        )
      )
    }

    test(s"$moduleName: Scala 2 falls back to the legacy Scala 2 version") {
      val res    = Artifacts.legacyRunnerModule(moduleName, scala213, Constants.defaultJavaVersion)
      val legacy = res.getOrElse(fail("expected a legacy module"))
      assertEquals(legacy.version, Constants.runnerScala2LegacyVersion)
    }

    test(s"$moduleName: an old Scala version takes precedence over an old JVM") {
      for {
        (scalaVersion, expectedVersion) <- Seq(
          legacyScala3 -> Constants.runnerScala30LegacyVersion,
          scala213     -> Constants.runnerScala2LegacyVersion
        )
      } {
        val res = Artifacts.legacyRunnerModule(
          moduleName,
          scalaVersion,
          Constants.minimumRunnerJavaVersion - 1
        )
        val legacy = res.getOrElse(fail("expected a legacy module"))
        assertEquals(legacy.version, expectedVersion)
        assertEquals(
          legacy.warnings.map(_.stripPrefix(s"$warnPrefix ")),
          Seq(
            s"Scala $scalaVersion is no longer supported by the $moduleName module.",
            s"Java ${Constants.minimumRunnerJavaVersion - 1} is no longer supported by the $moduleName module.",
            defaultingWarning(moduleName, expectedVersion),
            s"To use the latest $moduleName, upgrade Scala to at least ${Constants.scala3LegacyLtsPrefix}.",
            s"To use the latest $moduleName, upgrade Java to at least ${Constants.minimumRunnerJavaVersion}."
          )
        )
      }
    }

    test(s"$moduleName: the fallback is reported exactly once") {
      for {
        scalaVersion <- Seq(defaultScala, legacyScala3, scala213)
        jvmVersion   <- Seq(Constants.minimumRunnerJavaVersion - 1, Constants.defaultJavaVersion)
        legacy       <- Artifacts.legacyRunnerModule(moduleName, scalaVersion, jvmVersion)
      } assertEquals(
        legacy.warnings.count(_.contains(defaultingWarning(moduleName, legacy.version))),
        1
      )
    }
  }
}
