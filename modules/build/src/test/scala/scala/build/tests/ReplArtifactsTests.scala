package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

import scala.build.ReplArtifacts

class ReplArtifactsTests extends TestUtil.ScalaCliBuildSuite {

  test("uber-jar JLine") {
    val artifacts = Seq("/cs/org/jline/jline/3.29.0/jline-3.29.0-jdk8.jar")
    expect(
      ReplArtifacts.jlineJavaOpts(artifacts, javaVersion = 24) == Seq(
        "--module-path",
        artifacts.mkString(File.pathSeparator),
        "--add-modules",
        "ALL-MODULE-PATH",
        "--enable-native-access=org.jline"
      )
    )
  }

  test("split JLine modules") {
    val artifacts = Seq(
      "/cs/org/jline/jline-terminal/3.30.16/jline-terminal-3.30.16.jar",
      "/cs/org/jline/jline-native/3.30.16/jline-native-3.30.16.jar"
    )
    expect(
      ReplArtifacts.jlineJavaOpts(artifacts, javaVersion = 24) == Seq(
        "--module-path",
        artifacts.mkString(File.pathSeparator),
        "--add-modules",
        "ALL-MODULE-PATH",
        "--enable-native-access=org.jline.nativ"
      )
    )
  }
}
