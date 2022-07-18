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
