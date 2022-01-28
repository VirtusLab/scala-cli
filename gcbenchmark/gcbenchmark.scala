//> using lib "com.lihaoyi::os-lib:0.8.0"
//> using lib "com.lihaoyi::pprint:0.7.1"
//> using scala "2.13"

// Usage: scala-cli gcbenchmark.scala -- <path_to_scala_cli_executable>

import scala.concurrent.duration._
import scala.collection.JavaConverters._

case class Result(
  env: Map[String, String],
  maxTime: Duration,
  avgTime: Duration,
  maxMemoryFootprintMb: Int,
  idleMemoryFootprintMb: Int
)

object Main {
  val workspace =
    os.temp.dir(os.pwd, "tmp-", deleteOnExit = true) // where the temporary files are stored
  val projectSize =
    200 // Number of files in a generated project used in benchmark
  val numberOfBuilds = 10 // How many times run build for each setup
  val idleWait =
    90 // In seconds. Wait after builds are done, to measure how much memory JVM returns to OS

  val setups = Seq(
    Map("BLOOP_JAVA_OPTS" -> "-XX:+UseParallelGC"),
    Map(
      "BLOOP_JAVA_OPTS" -> "-XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahUncommitDelay=30000"
    ),
    Map.empty[String, String]
  )

  def bloopMemory(bloopPid: Int) =
    os
      .proc("ps", "-o", "rss", bloopPid)
      .call()
      .out
      .text()
      .linesIterator
      .toList(1)
      .toInt / 1024

  def bloopPid: Option[Int] = {
    val processes = os.proc("jps", "-l").call().out.text()
    "(\\d+) bloop[.]Bloop".r
      .findFirstMatchIn(processes)
      .map(_.group(1).toInt)
  }
  def scalaFile(objectName: String, rand: Int) = s"""
                                                    |object $objectName {
                                                    |def donothing(i:Int) = {}
                                                    |  donothing($rand)
                                                    |${"  donothing(1+1)\n" * 1000}
                                                    |}
                                                    |""".stripMargin

  def build(scalaCli: String, rand: Int, env: Map[String, String]): Duration = {
    val classes = (1 to projectSize).map(i => s"Bench$i")
    for { c <- classes } os.write.over(
      workspace / s"$c.scala",
      scalaFile(s"$c", 1000 + rand)
    )
    val start = System.nanoTime()
    os.proc(
      "java",
      "-jar",
      scalaCli,
      "compile",
      workspace
    ).call(cwd = workspace, env = env, stdout = os.Inherit)
    val stop    = System.nanoTime()
    val elapsed = 1.0 * (stop - start) / 1000000000
    elapsed.seconds
  }

  def main(args: Array[String]): Unit = {
    val scalaCli = pprint.log(args(0))
    os.proc("java", "-version").call(stdout = os.Inherit)
    val results = for { env <- setups } yield {
      pprint.log(env)
      bloopPid.foreach(p => os.proc("kill", p).call(stderr = os.Inherit))
      Thread.sleep(3000)
      println("=" * 80)
      build(
        scalaCli,
        0,
        env ++ System.getenv.asScala
      )
      val buildResults = (1 to numberOfBuilds).map { i =>
        val elapsed = build(scalaCli, i, env ++ System.getenv.asScala)
        val memory  = bloopMemory(bloopPid.get)
        pprint.log(f"$memory MB, $elapsed")
        (memory, elapsed)
      }
      val idleResults = for { i <- 1 to idleWait } yield {
        Thread.sleep(1000)
        val memory = bloopMemory(bloopPid.get)
        pprint.log(s"$memory MB")
        memory
      }
      val res = Result(
        env = env,
        maxTime = buildResults.map(_._2).max.toSeconds.seconds,
        avgTime = (buildResults
          .map(_._2)
          .fold(0.seconds)(_ + _) / buildResults.size).toSeconds.seconds,
        maxMemoryFootprintMb = buildResults.map(_._1).max,
        idleMemoryFootprintMb = idleResults.min
      )
      res
    }
    println("=" * 80)
    pprint.log(results)
  }
}
