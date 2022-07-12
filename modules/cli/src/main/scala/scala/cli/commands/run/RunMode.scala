package scala.cli.commands.run

sealed abstract class RunMode extends Product with Serializable

object RunMode {
  case object Default     extends RunMode
  case object SparkSubmit extends RunMode
}
