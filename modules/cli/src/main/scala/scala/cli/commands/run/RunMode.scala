package scala.cli.commands.run

sealed abstract class RunMode extends Product with Serializable

object RunMode {

  sealed abstract class HasRepl extends RunMode
  sealed abstract class Spark extends RunMode {
    def submitArgs: Seq[String]
    def withSubmitArgs(args: Seq[String]): Spark
  }

  case object Default extends HasRepl
  final case class SparkSubmit(submitArgs: Seq[String]) extends Spark {
    def withSubmitArgs(args: Seq[String]): SparkSubmit =
      copy(submitArgs = args)
  }
  final case class StandaloneSparkSubmit(submitArgs: Seq[String]) extends Spark {
    def withSubmitArgs(args: Seq[String]): StandaloneSparkSubmit =
      copy(submitArgs = args)
  }
  case object HadoopJar extends RunMode
}
