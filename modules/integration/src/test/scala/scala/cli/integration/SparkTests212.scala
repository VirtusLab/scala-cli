package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

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

  def simplePackageSparkJobTest(spark: Spark): Unit = {
    val master = "local[4]"
    val inputs = TestInputs(
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
    inputs.fromRoot { root =>
      val dest = os.rel / "SparkJob.jar"
      os.proc(TestUtil.cli, "package", extraOptions, "--spark", "--jvm", "8", ".", "-o", dest)
        .call(cwd = root)

      val java8Home =
        os.Path(os.proc(TestUtil.cs, "java-home", "--jvm", "8").call().out.trim(), os.pwd)

      val ext = if (Properties.isWin) ".cmd" else ""
      val res =
        os.proc(spark.sparkHome / "bin" / s"spark-submit$ext", "--master", master, dest).call(
          cwd = root,
          env = Map(
            "JAVA_HOME" -> java8Home.toString,
            "PATH" -> ((java8Home / "bin").toString + File.pathSeparator + System.getenv("PATH"))
          )
        )

      val expectedOutput = "Result: 55"

      expect(res.out.trim() == expectedOutput)
    }
  }

  test("spark 2.4") {
    simplePackageSparkJobTest(spark24)
  }

  test("spark 3.0") {
    simplePackageSparkJobTest(spark30)
  }

}
