package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class HadoopTests extends munit.FunSuite {
  protected lazy val extraOptions: Seq[String] = TestUtil.extraOptions

  for {
    withTestScope <- Seq(true, false)
    scopeDescription = if (withTestScope) "test scope" else "main scope"
    inputPath =
      if (withTestScope) os.rel / "test" / "WordCount.java" else os.rel / "main" / "WordCount.java"
    directiveKey = if (withTestScope) "test.dep" else "dep"
    scopeOptions = if (withTestScope) Seq("--test") else Nil
  }
    test(s"simple map-reduce ($scopeDescription)") {
      TestUtil.retryOnCi() {
        val inputs = TestInputs(
          inputPath ->
            s"""//> using $directiveKey org.apache.hadoop:hadoop-client-api:3.3.3
               |
               |// from https://hadoop.apache.org/docs/r3.3.3/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html
               |
               |package foo;
               |
               |import java.io.IOException;
               |import java.util.StringTokenizer;
               |
               |import org.apache.hadoop.conf.Configuration;
               |import org.apache.hadoop.fs.Path;
               |import org.apache.hadoop.io.IntWritable;
               |import org.apache.hadoop.io.Text;
               |import org.apache.hadoop.mapreduce.Job;
               |import org.apache.hadoop.mapreduce.Mapper;
               |import org.apache.hadoop.mapreduce.Reducer;
               |import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
               |import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
               |
               |public class WordCount {
               |
               |  public static class TokenizerMapper
               |       extends Mapper<Object, Text, Text, IntWritable>{
               |
               |    private final static IntWritable one = new IntWritable(1);
               |    private Text word = new Text();
               |
               |    public void map(Object key, Text value, Context context
               |                    ) throws IOException, InterruptedException {
               |      StringTokenizer itr = new StringTokenizer(value.toString());
               |      while (itr.hasMoreTokens()) {
               |        word.set(itr.nextToken());
               |        context.write(word, one);
               |      }
               |    }
               |  }
               |
               |  public static class IntSumReducer
               |       extends Reducer<Text,IntWritable,Text,IntWritable> {
               |    private IntWritable result = new IntWritable();
               |
               |    public void reduce(Text key, Iterable<IntWritable> values,
               |                       Context context
               |                       ) throws IOException, InterruptedException {
               |      int sum = 0;
               |      for (IntWritable val : values) {
               |        sum += val.get();
               |      }
               |      result.set(sum);
               |      context.write(key, result);
               |    }
               |  }
               |
               |  public static void main(String[] args) throws Exception {
               |    Configuration conf = new Configuration();
               |    Job job = Job.getInstance(conf, "word count");
               |    job.setJarByClass(WordCount.class);
               |    job.setMapperClass(TokenizerMapper.class);
               |    job.setCombinerClass(IntSumReducer.class);
               |    job.setReducerClass(IntSumReducer.class);
               |    job.setOutputKeyClass(Text.class);
               |    job.setOutputValueClass(IntWritable.class);
               |    FileInputFormat.addInputPath(job, new Path(args[0]));
               |    FileOutputFormat.setOutputPath(job, new Path(args[1]));
               |    System.exit(job.waitForCompletion(true) ? 0 : 1);
               |  }
               |}
               |""".stripMargin
        )
        inputs.fromRoot { root =>
          val res = os.proc(
            TestUtil.cli,
            "--power",
            "run",
            TestUtil.extraOptions,
            ".",
            "--hadoop",
            "--command",
            "--scratch-dir",
            "tmp",
            scopeOptions,
            "--",
            "foo"
          )
            .call(cwd = root)
          val command = res.out.lines()
          pprint.err.log(command)
          expect(command.take(2) == Seq("hadoop", "jar"))
          expect(command.takeRight(2) == Seq("foo.WordCount", "foo"))
        }
      }
    }
}
