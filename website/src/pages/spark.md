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

## Running Hadoop jobs

The `run` sub-command can run Hadoop jobs, by calling the `hadoop jar` command under-the-hood:

<ChainedSnippets>

```java title=WordCount.java
//> using lib "org.apache.hadoop:hadoop-client-api:3.3.3"

// from https://hadoop.apache.org/docs/r3.3.3/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCount {

  public static class TokenizerMapper
       extends Mapper<Object, Text, Text, IntWritable>{

    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();

    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());
      while (itr.hasMoreTokens()) {
        word.set(itr.nextToken());
        context.write(word, one);
      }
    }
  }

  public static class IntSumReducer
       extends Reducer<Text,IntWritable,Text,IntWritable> {
    private IntWritable result = new IntWritable();

    public void reduce(Text key, Iterable<IntWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.set(sum);
      context.write(key, result);
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "word count");
    job.setJarByClass(WordCount.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
```

```bash
scala-cli run --hadoop WordCount.java
```

</ChainedSnippets>
