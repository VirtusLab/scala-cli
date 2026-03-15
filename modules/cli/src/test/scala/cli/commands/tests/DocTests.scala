package scala.cli.commands.tests

import com.eed3si9n.expecty.Expecty.assert as expect

import scala.build.internal.Constants
import scala.cli.commands.doc.Doc

class DocTests extends munit.FunSuite {

  for (javaVersion <- Constants.mainJavaVersions)
    test(s"correct external mappings for JVM $javaVersion") {
      val args        = Doc.defaultScaladocArgs(Constants.defaultScalaVersion, javaVersion)
      val mappingsArg = args.find(_.startsWith("-external-mappings:")).get
      if javaVersion >= 11 then
        expect(mappingsArg.contains(s"javase/$javaVersion/docs/api/java.base/"))
      else
        expect(mappingsArg.contains(s"javase/$javaVersion/docs/api/"))
        expect(!mappingsArg.contains("java.base/"))
      expect(mappingsArg.contains(s"scala-lang.org/api/${Constants.defaultScalaVersion}/"))
    }

  test(s"correct external mappings for Scala 3 LTS (${Constants.scala3Lts})") {
    val args        = Doc.defaultScaladocArgs(Constants.scala3Lts, Constants.defaultJavaVersion)
    val mappingsArg = args.find(_.startsWith("-external-mappings:")).get
    expect(mappingsArg.contains(s"scala-lang.org/api/${Constants.scala3Lts}/"))
    expect(
      mappingsArg.contains(s"javase/${Constants.defaultJavaVersion}/docs/api/java.base/")
    )
  }

  test(s"correct external mappings for default Scala (${Constants.defaultScalaVersion})") {
    val args =
      Doc.defaultScaladocArgs(Constants.defaultScalaVersion, Constants.defaultJavaVersion)
    val mappingsArg = args.find(_.startsWith("-external-mappings:")).get
    expect(mappingsArg.contains(s"scala-lang.org/api/${Constants.defaultScalaVersion}/"))
    expect(
      mappingsArg.contains(s"javase/${Constants.defaultJavaVersion}/docs/api/java.base/")
    )
  }
}
