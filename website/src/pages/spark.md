# Experimental Spark features

import {ChainedSnippets, GiflikeVideo} from "../../src/components/MarkdownComponents.js";

## Packaging

The `package` sub-command offers to package Scala CLI projects as JARs ready to be passed
to `spark-submit`, and optimized for it.

<ChainedSnippets>

```scala title=SparkJob.scala
//> using lib "org.apache.spark::spark-sql:3.0.3"
//> using scala "2.12.15"

import org.apache.spark._
import org.apache.spark.sql._

object SparkJob {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Test job")
      .getOrCreate()
    import spark.implicits._
    def sc    = spark.sparkContext
    val accum = sc.longAccumulator
    sc.parallelize(1 to 10).foreach(x => accum.add(x))
    println("Result: " + accum.value)
  }
}
```

```bash
scala-cli package --spark SparkJob.scala -o spark-job.jar
```

```text
Compiling project (Scala 2.12.15, JVM)
Compiled project (Scala 2.12.15, JVM)
Wrote spark-job.jar
```

```bash
spark-submit spark-job.jar
```

```text
…
Result: 55
…
```

</ChainedSnippets>

## Running Spark jobs

The `run` sub-command can run Spark jobs, when passed `--spark`:

```bash
scala-cli run --spark SparkJob.scala # same example as above
```

Note that this requires either
- `spark-submit` to be in available in `PATH`
- `SPARK_HOME` to be set in the environment

## Running Spark jobs in a standalone way

The `run` sub-command can not only run Spark jobs, but it can also work without a Spark
distribution. For that to work, it downloads Spark JARs, and calls the main class of
`spark-submit` itself via these JARs:

```bash
scala-cli run --spark-standalone SparkJob.scala # same example as above
```
