package scala.cli.commands.tests

import com.eed3si9n.expecty.Expecty.assert as expect

import scala.build.CrossBuildParams
import scala.build.internal.Constants
import scala.cli.commands.doc.Doc

class DocTests extends munit.FunSuite {

  test("crossDocSubdirName: single cross group yields empty subdir") {
    val params = CrossBuildParams(Constants.defaultScala213Version, "jvm")
    expect(Doc.crossDocSubdirName(
      params,
      multipleCrossGroups = false,
      needsPlatformInSuffix = false
    ) == "")
    expect(Doc.crossDocSubdirName(
      params,
      multipleCrossGroups = false,
      needsPlatformInSuffix = true
    ) == "")
  }

  test("crossDocSubdirName: multiple groups, single platform uses only Scala version") {
    val params = CrossBuildParams(Constants.scala3Lts, "jvm")
    expect(
      Doc.crossDocSubdirName(params, multipleCrossGroups = true, needsPlatformInSuffix = false) ==
        Constants.scala3Lts
    )
  }

  test("crossDocSubdirName: multiple groups and platforms include platform in suffix") {
    val paramsJvm = CrossBuildParams(Constants.defaultScala213Version, "jvm")
    val paramsJs  = CrossBuildParams(Constants.defaultScala213Version, "js")
    val paramsNat = CrossBuildParams(Constants.scala3Lts, "native")
    expect(
      Doc.crossDocSubdirName(paramsJvm, multipleCrossGroups = true, needsPlatformInSuffix = true) ==
        s"${Constants.defaultScala213Version}_jvm"
    )
    expect(
      Doc.crossDocSubdirName(paramsJs, multipleCrossGroups = true, needsPlatformInSuffix = true) ==
        s"${Constants.defaultScala213Version}_js"
    )
    expect(
      Doc.crossDocSubdirName(paramsNat, multipleCrossGroups = true, needsPlatformInSuffix = true) ==
        s"${Constants.scala3Lts}_native"
    )
  }

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
