package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File
import java.util.Locale

import scala.jdk.CollectionConverters._
import scala.util.Properties

class SparkTests212 extends SparkTestDefinitions with Test212 {
  import SparkTestDefinitions.*

  private val spark30 = new Spark(
    "3.0.3",
    // The spark distribution actually ships with Scala 2.12.10, but we run into #1092 if we use it here
    "2.12.15"
  )

  private val spark24 = new Spark(
    "2.4.2",
    // The spark distribution actually ships with Scala 2.12.8, but we run into #1092 if we use it here
    "2.12.15"
  )

  override def afterAll(): Unit = {
    spark30.cleanUp()
    spark24.cleanUp()
  }

  def simplePackageSparkJobTest(spark: Spark): Unit =
    simpleJobInputs(spark, withTestScope = false).fromRoot { root =>
      val dest = os.rel / "SparkJob.jar"
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        "--spark",
        "--jvm",
        "8",
        ".",
        "-o",
        dest
      )
        .call(cwd = root)

      val java8Home =
        os.Path(os.proc(TestUtil.cs, "java-home", "--jvm", "zulu:8").call().out.trim(), os.pwd)

      val ext = if (Properties.isWin) ".cmd" else ""
      val res =
        os.proc(
          spark.sparkHome / "bin" / s"spark-submit$ext",
          "--master",
          defaultMaster,
          dest
        ).call(
          cwd = root,
          env = Map(
            "JAVA_HOME" -> java8Home.toString,
            "PATH" -> ((java8Home / "bin").toString + File.pathSeparator + System.getenv("PATH"))
          )
        )

      val expectedOutput = "Result: 55"

      expect(res.out.trim() == expectedOutput)
    }

  private def addToPath(dir: os.Path): Map[String, String] = {
    // On Windows, trying to preserve the case of the PATH entry
    def default = "PATH" -> Option(System.getenv("PATH")).getOrElse("")
    val (key, currentValue) =
      if (Properties.isWin)
        System.getenv().asScala.find(_._1.toLowerCase(Locale.ROOT) == "path").getOrElse(default)
      else
        default

    Map(key -> s"$dir${File.pathSeparator}$currentValue")
  }

  def simpleRunSparkJobTest(
    spark: Spark,
    usePath: Boolean = false,
    withTestScope: Boolean = false
  ): Unit =
    simpleJobInputs(spark, withTestScope).fromRoot { root =>
      val env =
        if (usePath) addToPath(spark.sparkHome / "bin")
        else Map("SPARK_HOME" -> spark.sparkHome.toString)
      val scopeOptions = if (withTestScope) Seq("--test") else Nil
      val res = os.proc(
        TestUtil.cli,
        "--power",
        "run",
        extraOptions,
        "--spark",
        "--jvm",
        "8",
        ".",
        scopeOptions
      )
        .call(cwd = root, env = env)

      val expectedOutput = "Result: 55"

      val output = res.out.trim().linesIterator.toVector

      expect(output.contains(expectedOutput))
    }

  test("package spark 2.4") {
    simplePackageSparkJobTest(spark24)
  }

  test("package spark 3.0") {
    simplePackageSparkJobTest(spark30)
  }

  for {
    withTestScope <- Seq(true, false)
    scopeDescription = if (withTestScope) "test scope" else "main scope"
  } {
    test(s"run spark 2.4 ($scopeDescription)") {
      simpleRunSparkJobTest(spark24, withTestScope = withTestScope)
    }

    test(s"run spark 2.4 standalone ($scopeDescription)") {
      simpleRunStandaloneSparkJobTest(
        spark24.scalaVersion,
        spark24.sparkVersion,
        withTestScope = withTestScope
      )
    }

    test(s"run spark 3.0 standalone ($scopeDescription)") {
      simpleRunStandaloneSparkJobTest(
        spark30.scalaVersion,
        spark30.sparkVersion,
        withTestScope = withTestScope
      )
    }

    for {
      usePath <- Seq(false, true)
      pathDescription = if (usePath) "via PATH " else ""
    }
      test(s"run spark 3.0 $pathDescription($scopeDescription)") {
        simpleRunSparkJobTest(spark30, usePath = usePath, withTestScope = withTestScope)
      }
  }

}
