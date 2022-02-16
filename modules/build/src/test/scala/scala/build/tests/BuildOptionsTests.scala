package scala.build.tests

import com.eed3si9n.expecty.Expecty.{assert => expect}
import dependency.ScalaParameters

import scala.build.Ops._
import scala.build.errors.{
  InvalidBinaryScalaVersionError,
  NoValidScalaVersionFoundError,
  UnsupportedScalaVersionError
}
import scala.build.internal.Constants._
import scala.build.internal.ScalaParse.scala2NightlyRegex
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

  test("-S 3.nightly option works") {
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some("3.nightly"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    val scalaParams = options.scalaParams.orThrow
    assert(
      scalaParams.scalaVersion.startsWith("3") && scalaParams.scalaVersion.endsWith("-NIGHTLY"),
      "-S 3.nightly argument does not lead to scala3 nightly build option"
    )
  }

  test(s"Scala 3.${Int.MaxValue} shows Invalid Binary Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(s"3.${Int.MaxValue}"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.isLeft && options.projectParams.left.get.isInstanceOf[
        InvalidBinaryScalaVersionError
      ],
      s"specifying the 3.${Int.MaxValue} scala version does not lead to the Invalid Binary Scala Version Error"
    )
  }

  test("Scala 2.11.2 shows Unupported Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some("2.11.2"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.isLeft && options.projectParams.left.get.isInstanceOf[
        UnsupportedScalaVersionError
      ],
      "specifying the 2.11.2 scala version does not lead to the Unsupported Scala Version Error"
    )
  }

  test("Scala 2.11 shows Unupported Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some("2.11"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.isLeft && options.projectParams.left.get.isInstanceOf[
        UnsupportedScalaVersionError
      ],
      "specifying the 2.11 scala version does not lead to the Unsupported Scala Version Error"
    )
  }

  test(s"Scala 3.${Int.MaxValue}.3 shows Invalid Binary Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(s"3.${Int.MaxValue}.3"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.isLeft && options.projectParams.left.get.isInstanceOf[
        InvalidBinaryScalaVersionError
      ],
      "specifying the 3.2147483647.3 scala version does not lead to the Invalid Binary Scala Version Error"
    )
  }

  test("Scala 3.1.3-RC1-bin-20220213-fd97eee-NIGHTLY shows No Valid Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some("3.1.3-RC1-bin-20220213-fd97eee-NIGHTLY"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.isLeft && options.projectParams.left.get.isInstanceOf[
        NoValidScalaVersionFoundError
      ],
      "specifying the wrong full scala 3 nightly version does not lead to the No Valid Scala Version Found Error"
    )
  }

  test("Scala 2.12.9-bin-1111111 shows No Valid Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some("2.12.9-bin-1111111"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.isLeft && options.projectParams.left.get.isInstanceOf[
        NoValidScalaVersionFoundError
      ],
      "specifying the wrong full scala 2 nightly version does not lead to the No Valid Scala Version Found Error"
    )
  }

  test(s"Scala 2.${Int.MaxValue} shows Invalid Binary Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(s"2.${Int.MaxValue}"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(options.projectParams.isLeft && options.projectParams.left.get.isInstanceOf[
      InvalidBinaryScalaVersionError
    ])
  }

  test("-S 2.nightly option works") {
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some("2.nightly"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    val scalaParams = options.scalaParams.orThrow
    assert(
      scala2NightlyRegex.unapplySeq(scalaParams.scalaVersion).isDefined,
      "-S 2.nightly argument does not lead to scala2 nightly build option"
    )
  }

  test("-S 2.13.9-bin-4505094 option works without repo specification") {
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some("2.13.9-bin-4505094"),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    val scalaParams = options.scalaParams.orThrow
    assert(
      scalaParams.scalaVersion == "2.13.9-bin-4505094",
      "-S 2.13.9-bin-4505094 argument does not lead to 2.13.9-bin-4505094 scala version in build option"
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
