package scala.cli.packaging

import munit.FunSuite

import scala.build.internal.Constants

final class NativeImageOptionsTests extends FunSuite {

  private val arrayOpsOption = "--initialize-at-build-time=scala.collection.ArrayOps$"

  private val preScala39Versions =
    Seq(
      Constants.defaultScala212Version,
      Constants.defaultScala213Version,
      Constants.scala3LegacyLts
    ) ++ Constants.scala38Versions

  private val scala39AndNewerVersions =
    (Constants.scala39Versions ++ Seq(
      Constants.defaultScalaVersion,
      Constants.scala3NextRcVersion
    )).distinct

  test("scala3StdLibOptions: no options when the Scala version is unknown") {
    assertEquals(NativeImage.scala3StdLibOptions(None), Nil)
  }

  test("scala3StdLibOptions: no options for Scala versions using the 2.13 standard library") {
    for scalaVersion <- preScala39Versions
    do assertEquals(NativeImage.scala3StdLibOptions(Some(scalaVersion)), Nil, scalaVersion)
  }

  test("scala3StdLibOptions: ArrayOps$ is initialized at build time for Scala 3.9+") {
    for scalaVersion <- scala39AndNewerVersions
    do
      assertEquals(
        NativeImage.scala3StdLibOptions(Some(scalaVersion)),
        Seq(arrayOpsOption),
        scalaVersion
      )
  }
}
