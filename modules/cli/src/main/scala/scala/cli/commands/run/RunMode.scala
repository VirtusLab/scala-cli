package scala.cli.commands.run

sealed abstract class RunMode extends Product with Serializable

object RunMode {

  sealed abstract class HasRepl extends RunMode
  sealed abstract class Spark extends HasRepl {
    def submitArgs: Seq[String]
    def replArgs: Seq[String]
    def withSubmitArgs(args: Seq[String]): Spark
    def withReplArgs(args: Seq[String]): Spark
  }

  case object Default extends HasRepl
  final case class SparkSubmit(submitArgs: Seq[String], replArgs: Seq[String]) extends Spark {
    def withSubmitArgs(args: Seq[String]): SparkSubmit =
      copy(submitArgs = args)
    def withReplArgs(args: Seq[String]): SparkSubmit =
      copy(replArgs = args)
  }
  final case class StandaloneSparkSubmit(submitArgs: Seq[String], replArgs: Seq[String])
      extends Spark {
    def withSubmitArgs(args: Seq[String]): StandaloneSparkSubmit =
      copy(submitArgs = args)
    def withReplArgs(args: Seq[String]): StandaloneSparkSubmit =
      copy(replArgs = args)
  }
  case object HadoopJar extends RunMode
}
