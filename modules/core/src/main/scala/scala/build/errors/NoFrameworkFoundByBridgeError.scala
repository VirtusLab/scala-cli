package scala.build.errors

final class NoFrameworkFoundByBridgeError(
  suspiciousJvmOnlyJars: Seq[String] = Nil
) extends TestError(
      NoFrameworkFoundByBridgeError.message(suspiciousJvmOnlyJars)
    )

object NoFrameworkFoundByBridgeError {
  private def message(jars: Seq[String]): String =
    if jars.isEmpty then "No framework found by Scala.js test bridge"
    else
      s"""No framework found by Scala.js test bridge.
         |The following JVM-only Scala dependencies are on the Scala.js classpath and likely caused the failure:
         |${jars.map("  - " + _).mkString("\n")}
         |Use the platform-aware dependency syntax (extra ':' before the version, e.g. 'org::name::version' / '//> using dep org::name::version') so the Scala.js artifact is fetched instead of the JVM one.""".stripMargin
}
