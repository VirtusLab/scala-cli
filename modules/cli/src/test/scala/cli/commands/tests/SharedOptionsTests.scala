package scala.cli.commands.tests

import com.eed3si9n.expecty.Expecty.assert as expect

import scala.cli.commands.shared.SharedOptions

class SharedOptionsTests extends munit.FunSuite {
  test("a class path directory contributes the jars directly in it, not nested ones") {
    val dir = os.temp.dir(prefix = "scala-cli-shared-options-tests-")
    try {
      val topJar = dir / "top.jar"
      os.write(topJar, "")
      os.write(dir / "nested" / "deep.jar", "", createFolders = true)
      val classPath = SharedOptions(extraJars = List(dir.toString)).extraJarsAndClassPath
      expect(classPath == List(dir, topJar))
    }
    finally os.remove.all(dir)
  }
}
