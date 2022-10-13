package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

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
      s"""//> using lib "org.apache.spark::spark-sql:${spark.sparkVersion}"
         |//> using lib "com.chuusai::shapeless:2.3.10"
         |//> using lib "com.lihaoyi::pprint:0.7.3"
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

  def simpleRunStandaloneSparkJobTest(scalaVersion: String, sparkVersion: String): Unit =
    simpleJobInputs(new Spark(sparkVersion, scalaVersion)).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", extraOptions, "--spark-standalone", "--jvm", "8", ".")
        .call(cwd = root)

      val expectedOutput = "Result: 55"

      val output = res.out.trim().linesIterator.toVector

      expect(output.contains(expectedOutput))
    }

  test("run spark 3.3 standalone") {
    simpleRunStandaloneSparkJobTest(actualScalaVersion, "3.3.0")
  }

}
