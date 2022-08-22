package scala.cli.commands.packaging

import dependency._

object Spark {

  private def names = Seq(
    // FIXME Add more?
    // (see "cs complete-dependency org.apache.spark: | grep '_2\.12$'"
    // or `ls "$(cs get https://archive.apache.org/dist/spark/spark-2.4.2/spark-2.4.2-bin-hadoop2.7.tgz --archive)"/*/jars | grep '^spark-'`)
    "core",
    "graphx",
    "hive",
    "hive-thriftserver",
    "kubernetes",
    "mesos",
    "mllib",
    "repl",
    "sql",
    "streaming",
    "yarn"
  )

  def sparkModules: Seq[AnyModule] =
    names.map(name => mod"org.apache.spark::spark-$name")

  def hadoopModules: Seq[AnyModule] =
    Seq(
      // TODO Add more for Hadoop 2, maybe for 3 too
      mod"org.apache.hadoop:hadoop-client-api"
    )
}
