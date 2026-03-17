package scala.build.tests

import java.nio.file.Files

import scala.build.errors.NoFrameworkFoundByNativeBridgeError
import scala.build.testrunner.AsmTestRunner

class FrameworkDiscoveryTests extends TestUtil.ScalaCliBuildSuite {

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

      val found = AsmTestRunner.findFrameworkServices(Seq(dir))
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
