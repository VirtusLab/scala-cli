package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File
import java.util.Locale

import scala.jdk.CollectionConverters._
import scala.util.Properties

object SparkTests212 {

  private def lightweightSparkDistribVersionOpt = Option("0.0.4")

  private final class Spark(val sparkVersion: String, val scalaVersion: String) {
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
        os.Path(os.proc(TestUtil.cs, "get", "--archive", url).call().out.text().trim, os.pwd)
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

class SparkTests212 extends SparkTestDefinitions {

  import SparkTests212.*

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

  private def defaultMaster = "local[4]"
  private def simpleJobInputs(spark: Spark) = TestInputs(
    os.rel / "SparkJob.scala" ->
      s"""//> using lib "org.apache.spark::spark-sql:${spark.sparkVersion}"
         |//> using scala "${spark.scalaVersion}"
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
         |    sc.parallelize(1 to 10).foreach(x => accum.add(x))
         |    println("Result: " + accum.value)
         |  }
         |}
         |""".stripMargin
  )

  def simplePackageSparkJobTest(spark: Spark): Unit =
    simpleJobInputs(spark).fromRoot { root =>
      val dest = os.rel / "SparkJob.jar"
      os.proc(TestUtil.cli, "package", extraOptions, "--spark", "--jvm", "8", ".", "-o", dest)
        .call(cwd = root)

      val java8Home =
        os.Path(os.proc(TestUtil.cs, "java-home", "--jvm", "8").call().out.trim(), os.pwd)

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

  def simpleRunSparkJobTest(spark: Spark, usePath: Boolean = false): Unit =
    simpleJobInputs(spark).fromRoot { root =>
      val env =
        if (usePath) addToPath(spark.sparkHome / "bin")
        else Map("SPARK_HOME" -> spark.sparkHome.toString)
      val res = os.proc(TestUtil.cli, "run", extraOptions, "--spark", "--jvm", "8", ".")
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

  test("run spark 2.4") {
    simpleRunSparkJobTest(spark24)
  }

  test("run spark 3.0") {
    simpleRunSparkJobTest(spark30)
  }

  test("run spark 3.0 via PATH") {
    simpleRunSparkJobTest(spark30, usePath = true)
  }

}
