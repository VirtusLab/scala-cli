package scala.cli.commands.run

sealed abstract class RunMode extends Product with Serializable

object RunMode {

  sealed abstract class HasRepl extends RunMode
  sealed abstract class Spark   extends RunMode

  case object Default               extends HasRepl
  case object SparkSubmit           extends Spark
  case object StandaloneSparkSubmit extends Spark
  case object HadoopJar             extends RunMode
}
