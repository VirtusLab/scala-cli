package scala.cli.commands

object OptionsHelper {
  implicit class Mandatory[A](x: Option[A]) {
    def mandatory(parameter: String, group: String): A =
      x match {
        case Some(v) => v
        case None =>
          System.err.println(
            s"${parameter.toLowerCase.capitalize} parameter is mandatory for ${group.toLowerCase.capitalize}"
          )
          sys.exit(1)
      }
  }
}
