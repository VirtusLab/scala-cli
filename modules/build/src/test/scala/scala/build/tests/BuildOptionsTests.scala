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
import scala.build.internal.Regexes.scala2NightlyRegex
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  InternalOptions,
  MaybeScalaVersion,
  ScalaOptions,
  ShadowingSeq
}
import scala.build.{Build, BuildThreads, LocalRepo}
import scala.build.Directories
import scala.build.options.ScalacOpt
import scala.build.Positioned

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
        scalaVersion = Some(MaybeScalaVersion("3.nightly")),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    val scalaParams = options.scalaParams.orThrow.getOrElse(???)
    assert(
      scalaParams.scalaVersion.startsWith("3") && scalaParams.scalaVersion.endsWith("-NIGHTLY"),
      "-S 3.nightly argument does not lead to scala3 nightly build option"
    )
  }
  test("-S 3.1.nightly option works") {
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion("3.1.nightly"))
      )
    )
    val scalaParams = options.scalaParams.orThrow.getOrElse(???)
    expect(
      scalaParams.scalaVersion.startsWith("3.1.") && scalaParams.scalaVersion.endsWith("-NIGHTLY"),
      "-S 3.1.nightly argument does not lead to scala 3.1. nightly build option"
    )
  }

  test(s"Scala 3.${Int.MaxValue} shows Invalid Binary Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion(s"3.${Int.MaxValue}")),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.swap.exists {
        case _: InvalidBinaryScalaVersionError => true; case _ => false
      },
      s"specifying the 3.${Int.MaxValue} scala version does not lead to the Invalid Binary Scala Version Error"
    )
  }

  test("Scala 2.11.2 shows Unupported Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion("2.11.2")),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.swap.exists {
        case _: UnsupportedScalaVersionError => true; case _ => false
      },
      "specifying the 2.11.2 scala version does not lead to the Unsupported Scala Version Error"
    )
  }

  test("Scala 2.11 shows Unupported Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion("2.11")),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.swap.exists {
        case _: UnsupportedScalaVersionError => true; case _ => false
      },
      "specifying the 2.11 scala version does not lead to the Unsupported Scala Version Error"
    )
  }

  test(s"Scala 3.${Int.MaxValue}.3 shows Invalid Binary Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion(s"3.${Int.MaxValue}.3")),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.swap.exists {
        case _: InvalidBinaryScalaVersionError => true; case _ => false
      },
      "specifying the 3.2147483647.3 scala version does not lead to the Invalid Binary Scala Version Error"
    )
  }

  test("Scala 3.1.3-RC1-bin-20220213-fd97eee-NIGHTLY shows No Valid Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion("3.1.3-RC1-bin-20220213-fd97eee-NIGHTLY")),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.swap.exists {
        case _: NoValidScalaVersionFoundError => true; case _ => false
      },
      "specifying the wrong full scala 3 nightly version does not lead to the No Valid Scala Version Found Error"
    )
  }

  test("Scala 3.1.2-RC1 works") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion("3.1.2-RC1"))
      )
    )
    val scalaParams = options.scalaParams.orThrow.getOrElse(???)
    assert(
      scalaParams.scalaVersion == "3.1.2-RC1",
      "-S 3.1.2-RC1 argument does not lead to 3.1.2-RC1 build option"
    )
  }

  test("Scala 2.12.9-bin-1111111 shows No Valid Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion("2.12.9-bin-1111111"))
      )
    )
    assert(
      options.projectParams.swap.exists {
        case _: NoValidScalaVersionFoundError => true; case _ => false
      },
      "specifying the wrong full scala 2 nightly version does not lead to the No Valid Scala Version Found Error"
    )
  }

  test(s"Scala 2.${Int.MaxValue} shows Invalid Binary Scala Version Error") {

    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion(s"2.${Int.MaxValue}")),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    assert(
      options.projectParams.swap.exists {
        case _: InvalidBinaryScalaVersionError => true; case _ => false
      },
      s"specifying 2.${Int.MaxValue} as Scala version does not lead to Invalid Binary Scala Version Error"
    )
  }

  test("-S 2.nightly option works") {
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion("2.nightly")),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    val scalaParams = options.scalaParams.orThrow.getOrElse(???)
    assert(
      scala2NightlyRegex.unapplySeq(scalaParams.scalaVersion).isDefined,
      "-S 2.nightly argument does not lead to scala2 nightly build option"
    )
  }

  test("-S 2.13.nightly option works") {
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion("2.13.nightly"))
      )
    )
    val scalaParams = options.scalaParams.orThrow.getOrElse(???)
    assert(
      scala2NightlyRegex.unapplySeq(scalaParams.scalaVersion).isDefined,
      "-S 2.13.nightly argument does not lead to scala2 nightly build option"
    )
  }

  test("-S 2.12.nightly option works") {
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion("2.12.nightly"))
      )
    )
    val scalaParams = options.scalaParams.orThrow.getOrElse(???)
    assert(
      scala2NightlyRegex.unapplySeq(scalaParams.scalaVersion).isDefined,
      "-S 2.12.nightly argument does not lead to scala2 nightly build option"
    )
  }

  test("-S 2.13.9-bin-4505094 option works without repo specification") {
    val options = BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = Some(MaybeScalaVersion("2.13.9-bin-4505094")),
        scalaBinaryVersion = None,
        supportedScalaVersionsUrl = None
      )
    )
    val scalaParams = options.scalaParams.orThrow.getOrElse(???)
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
          scalaVersion = prefix.map(MaybeScalaVersion(_)),
          scalaBinaryVersion = None,
          supportedScalaVersionsUrl = None
        )
      )
      val scalaParams = options.scalaParams.orThrow.getOrElse(???)

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
            scalaVersion = prefix.map(MaybeScalaVersion(_)),
            scalaBinaryVersion = None,
            supportedScalaVersionsUrl = Some(s"file://${confFilePath.toString()}")
          )
        )

        val scalaParams         = options.scalaParams.orThrow.getOrElse(???)
        val expectedScalaParams = ScalaParameters(expectedScalaVersion)

        expect(scalaParams == expectedScalaParams)
      }

    }

  val extraRepoTmpDir = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories     = Directories.under(extraRepoTmpDir)
  val buildThreads    = BuildThreads.create()
  override def afterAll(): Unit = {
    buildThreads.shutdown()
  }

  test("User scalac options shadow internal ones") {
    val defaultOptions = BuildOptions(
      internal = InternalOptions(
        localRepository = LocalRepo.localRepo(directories.localRepoDir)
      )
    )

    val newSourceRoot = os.pwd / "out" / "foo"

    val extraScalacOpt = Seq("-sourceroot", newSourceRoot.toString)
    val options = defaultOptions.copy(
      scalaOptions = defaultOptions.scalaOptions.copy(
        scalaVersion = Some(MaybeScalaVersion("3.1.1")),
        scalacOptions = ShadowingSeq.from(
          extraScalacOpt
            .map(ScalacOpt(_))
            .map(Positioned.none)
        )
      )
    )

    val dummyInputs = TestInputs(
      os.rel / "Foo.scala" ->
        """object Foo
          |""".stripMargin
    )

    dummyInputs.withLoadedBuild(options, buildThreads, None) {
      (_, _, build) =>

        val build0 = build match {
          case s: Build.Successful => s
          case _                   => sys.error(s"Unexpected failed or cancelled build $build")
        }

        val rawOptions = build0.project.scalaCompiler.toSeq.flatMap(_.scalacOptions)
        val seq        = ShadowingSeq.from(rawOptions.map(ScalacOpt(_)))

        expect(seq.toSeq.length == rawOptions.length) // no option needs to be shadowed

        pprint.err.log(rawOptions)
        expect(rawOptions.containsSlice(extraScalacOpt))
    }
  }

}
