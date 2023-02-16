package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File
import java.util.Locale

import scala.util.Properties

object SparkTestDefinitions {

  def lightweightSparkDistribVersionOpt = Option("0.0.4")

  final class Spark(val sparkVersion: String, val scalaVersion: String) {
    private def sbv         = scalaVersion.split('.').take(2).mkString(".")
    private var toDeleteOpt = Option.empty[os.Path]
    lazy val sparkHome: os.Path = {
      val url = lightweightSparkDistribVersionOpt match {
        case Some(lightweightSparkDistribVersion) =>
          s"https://github.com/scala-cli/lightweight-spark-distrib/releases/download/v$lightweightSparkDistribVersion/spark-$sparkVersion-bin-hadoop2.7-scala$sbv.tgz"
        case None =>
          // original URL (too heavyweight, often fails / times out…)
          s"https://archive.apache.org/dist/spark/spark-$sparkVersion/spark-$sparkVersion-bin-hadoop2.7.tgz"
      }
      val baseDir =
        os.Path(os.proc(TestUtil.cs, "get", "--archive", url).call().out.trim(), os.pwd)
      val home = os.list(baseDir) match {
        case Seq(dir) if os.isDir(dir) => dir
        case _                         => baseDir
      }
      if (lightweightSparkDistribVersionOpt.nonEmpty) {
        val copy = os.temp.dir(prefix = home.last) / "home"
        toDeleteOpt = Some(copy)
        System.err.println(s"Copying $home over to $copy")
        os.copy(home, copy)
        val fetchJarsScript0 = copy / "fetch-jars.sh"
        val cmd: Seq[os.Shellable] =
          if (Properties.isWin) Seq("""C:\Program Files\Git\bin\bash.EXE""", fetchJarsScript0)
          else Seq(fetchJarsScript0)

        System.err.println(s"Running $cmd")
        os.proc(cmd).call(stdin = os.Inherit, stdout = os.Inherit)
        System.err.println(s"Spark home $copy ready")
        copy
      }
      else
        home
    }
    def cleanUp(): Unit =
      toDeleteOpt.foreach(os.remove.all(_))
  }

}

abstract class SparkTestDefinitions(val scalaVersionOpt: Option[String]) extends ScalaCliSuite
    with TestScalaVersionArgs {

  import SparkTestDefinitions.*

  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

  protected def defaultMaster = "local[4]"
  protected def simpleJobInputs(spark: Spark) = TestInputs(
    os.rel / "SparkJob.scala" ->
      s"""//> using dep "org.apache.spark::spark-sql:${spark.sparkVersion}"
         |//> using dep "com.chuusai::shapeless:2.3.10"
         |//> using dep "com.lihaoyi::pprint:0.7.3"
         |
         |import org.apache.spark._
         |import org.apache.spark.sql._
         |
         |object SparkJob {
         |  def main(args: Array[String]): Unit = {
         |    val spark = SparkSession.builder()
         |      .appName("Test job")
         |      .getOrCreate()
         |    import spark.implicits._
         |    def sc    = spark.sparkContext
         |    val accum = sc.longAccumulator
         |    sc.parallelize(1 to 10).foreach { x =>
         |      import shapeless._
         |      val l = x :: HNil
         |      accum.add(l.head)
         |    }
         |    pprint.err.log(accum.value)
         |    println("Result: " + accum.value)
         |  }
         |}
         |""".stripMargin
  )

  private def maybeSetupWinutils(hadoopHome: os.Path): Unit =
    if (Properties.isWin) {
      val bin = hadoopHome / "bin"
      os.makeDir.all(bin)
      val res = os.proc(
        TestUtil.cs,
        "get",
        "--archive",
        "https://github.com/steveloughran/winutils/releases/download/tag_2017-08-29-hadoop-2.8.1-native/hadoop-2.8.1.zip"
      )
        .call(cwd = hadoopHome)
      val dataDir = os.Path(res.out.trim())
      val binStuffDir = os.list(dataDir)
        .filter(os.isDir(_))
        .filter(_.last.startsWith("hadoop-"))
        .headOption
        .getOrElse {
          sys.error(s"No hadoop-* directory found under $dataDir")
        }
      for (elem <- os.list(binStuffDir))
        os.copy.into(elem, bin)
    }

  private def maybeHadoopHomeForWinutils(hadoopHome: os.Path): Map[String, String] =
    if (Properties.isWin) {
      // FIXME Maybe Scala CLI should handle that itself, when on Windows,
      // for Spark >= 3.3.0 (maybe 3.3.0, > 3.0 for sure)
      maybeSetupWinutils(hadoopHome)
      val (pathVarName, currentPath) =
        sys.env.find(_._1.toLowerCase(Locale.ROOT) == "path").getOrElse(("PATH", ""))
      Map(
        pathVarName   -> s"${hadoopHome / "bin"}${File.pathSeparator}$currentPath",
        "HADOOP_HOME" -> hadoopHome.toString
      )
    }
    else
      Map.empty[String, String]

  def simpleRunStandaloneSparkJobTest(
    scalaVersion: String,
    sparkVersion: String,
    needsWinUtils: Boolean = false
  ): Unit =
    simpleJobInputs(new Spark(sparkVersion, scalaVersion)).fromRoot { root =>
      val extraEnv =
        if (needsWinUtils) maybeHadoopHomeForWinutils(root / "hadoop-home")
        else Map.empty[String, String]
      val res = os.proc(TestUtil.cli, "--power", "run", extraOptions, "--spark-standalone", ".")
        .call(cwd = root, env = extraEnv)

      val expectedOutput = "Result: 55"

      val output = res.out.trim().linesIterator.toVector

      expect(output.contains(expectedOutput))
    }

  test("run spark 3.3 standalone") {
    simpleRunStandaloneSparkJobTest(actualScalaVersion, "3.3.0", needsWinUtils = true)
  }

  test("run spark spark-submit args") {
    val jobName = "the test spark job"
    val inputs = TestInputs(
      os.rel / "SparkJob.scala" ->
        s"""//> using dep "org.apache.spark::spark-sql:3.3.0"
           |
           |import org.apache.spark._
           |import org.apache.spark.sql._
           |
           |object SparkJob {
           |  def main(args: Array[String]): Unit = {
           |    val spark = SparkSession.builder().getOrCreate()
           |    val name = spark.conf.get("spark.app.name")
           |    assert(name == "$jobName")
           |    import spark.implicits._
           |    def sc    = spark.sparkContext
           |    val accum = sc.longAccumulator
           |    sc.parallelize(1 to 10).foreach(x => accum.add(x))
           |    println("Result: " + accum.value)
           |  }
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val extraEnv = maybeHadoopHomeForWinutils(root / "hadoop-home")
      val res = os.proc(
        TestUtil.cli,
        "--power",
        "run",
        extraOptions,
        "--spark-standalone",
        ".",
        "--submit-arg",
        "--name",
        "--submit-arg",
        jobName
      )
        .call(cwd = root, env = extraEnv)

      val expectedOutput = "Result: 55"

      val output = res.out.trim().linesIterator.toVector

      expect(output.contains(expectedOutput))
    }
  }

}
