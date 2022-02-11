package scala.build.tests

import com.eed3si9n.expecty.Expecty.{assert => expect}
import dependency.ScalaParameters

import scala.build.Ops._
import scala.build.internal.Constants._
import scala.build.options.{BuildOptions, BuildRequirements, ScalaOptions}
import scala.util.Random

class BuildOptionsTests extends munit.FunSuite {

  test("Empty BuildOptions is actually empty") {
    val empty = BuildOptions()
    val zero  = BuildOptions.monoid.zero
    expect(
      empty == zero,
      "Unexpected Option / Seq / Set / Boolean with a non-empty / non-false default value"
    )
  }

  test("Empty BuildRequirements is actually empty") {
    val empty = BuildRequirements()
    val zero  = BuildRequirements.monoid.zero
    expect(
      empty == zero,
      "Unexpected Option / Seq / Set / Boolean with a non-empty / non-false default value"
    )
  }

  test("-S 3.nightly option works") {
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some("3.nightly"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl =
          Some(
            Random.alphanumeric.take(10).mkString("")
          ) // invalid url, it should use defaults from Deps.sc
      )
    )
    val scalaParams = options.scalaParams.orThrow
    assert(
      scalaParams.scalaVersion.startsWith("3") && scalaParams.scalaVersion.endsWith("-NIGHTLY"),
      "-S 3.nightly argument does not lead to scala3 nightly build option"
    )
  }

  test("-S 2.nightly option works") {
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some("2.nightly"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl =
          Some(
            Random.alphanumeric.take(10).mkString("")
          ) // invalid url, it should use defaults from Deps.sc
      )
    )
    val scalaParams        = options.scalaParams.orThrow
    val scala2NightlyRegex = raw"""(\d+)\.(\d+)\.(\d+)-bin-[a-f0-9]*""".r
    assert(
      scala2NightlyRegex.unapplySeq(scalaParams.scalaVersion).isDefined,
      "-S 2.nightly argument does not lead to scala2 nightly build option"
    )
  }

  val expectedScalaVersions = Seq(
    Some("3")      -> defaultScalaVersion,
    None           -> defaultScalaVersion,
    Some("2.13")   -> defaultScala213Version,
    Some("2.12")   -> defaultScala212Version,
    Some("2")      -> defaultScala213Version,
    Some("2.13.2") -> "2.13.2",
    Some("3.0.1")  -> "3.0.1",
    Some("3.0")    -> "3.0.2"
  )

  for ((prefix, expectedScalaVersion) <- expectedScalaVersions)
    test(
      s"use expected default scala version for prefix scala version: ${prefix.getOrElse("empty")}"
    ) {
      val options = BuildOptions(
        scalaOptions = ScalaOptions(
          scalaVersion = prefix,
          scalaBinaryVersion = None,
          supportedScalaVersionsUrl =
            Some(
              Random.alphanumeric.take(10).mkString("")
            ) // invalid url, it should use defaults from Deps.sc
        )
      )
      val scalaParams = options.scalaParams.orThrow

      val expectedScalaParams = ScalaParameters(expectedScalaVersion)

      expect(scalaParams == expectedScalaParams)
    }

  val expectedScalaConfVersions = Seq(
    Some("3")      -> "3.0.1",
    Some("3.0")    -> "3.0.1",
    None           -> "3.0.1",
    Some("2.13")   -> "2.13.4",
    Some("2.12")   -> "2.12.13",
    Some("2")      -> "2.13.4",
    Some("2.13.2") -> "2.13.2"
  )

  val confFile = s"""[
                    | {
                    |  "scalaCliVersion": "$version",
                    |  "supportedScalaVersions": ["3.0.1", "2.13.4", "2.12.13"]
                    | }
                    |]""".stripMargin

  for ((prefix, expectedScalaVersion) <- expectedScalaConfVersions)
    test(s"use expected scala version from conf file, prefix: ${prefix.getOrElse("empty")}") {
      TestInputs.withTmpDir("conf-scala-versions") { dirPath =>

        val confFilePath = dirPath / "conf-file.json"
        os.write(confFilePath, confFile)

        val options = BuildOptions(
          scalaOptions = ScalaOptions(
            scalaVersion = prefix,
            scalaBinaryVersion = None,
            supportedScalaVersionsUrl = Some(s"file://${confFilePath.toString()}")
          )
        )

        val scalaParams         = options.scalaParams.orThrow
        val expectedScalaParams = ScalaParameters(expectedScalaVersion)

        expect(scalaParams == expectedScalaParams)
      }

    }

}
