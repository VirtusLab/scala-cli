package scala.build.interactive

import scala.io.StdIn

sealed abstract class Interactive extends Product with Serializable {
  def confirmOperation(msg: String): Option[Boolean]                = None
  def chooseOne(msg: String, options: List[String]): Option[String] = None
}

object Interactive {

  private var interactiveInputsOpt =
    Option(System.getenv("SCALA_CLI_INTERACTIVE_INPUTS"))
      .map(_.linesIterator.toList)

  private def readLine(): String =
    interactiveInputsOpt match {
      case None =>
        StdIn.readLine()
      case Some(interactiveInputs) =>
        synchronized {
          interactiveInputs match {
            case Nil => ""
            case h :: t =>
              interactiveInputsOpt = Some(t)
              h
          }
        }
    }

  case object InteractiveNop extends Interactive

  case object InteractiveAsk extends Interactive {

    private sealed abstract class Action[V] extends Product with Serializable {
      def msg: String
      def action: Option[V]
      final def run: Option[V] =
        if (interactiveInputsOpt.nonEmpty || coursier.paths.Util.useAnsiOutput())
          action
        else None
    }

    private case class ConfirmOperation(msg: String) extends Action[Boolean] {
      override def action: Option[Boolean] = {
        System.err.println(s"$msg [Y/n]")
        val response = readLine()
        if (response.toLowerCase == "y")
          Some(true)
        else {
          System.err.println("Abort")
          Some(false)
        }
      }
    }

    private case class ChooseOne(msg: String, options: List[String])
        extends Action[String] {
      override def action: Option[String] = {
        System.err.println(msg)
        options.zipWithIndex.foreach {
          case (option, index) => System.err.println(s"[$index] $option")
        }
        val response = readLine()
        parseIndexInput(response, options.length - 1)
      }

      private def parseIndexInput(input: String, range: Int): Option[String] =
        input.toIntOption match {
          case Some(index) =>
            val isInRange = index <= range && index >= 0
            if (isInRange) Some(options(index))
            else {
              System.err.println(
                s"The input index number is invalid, integer value from 0 to $range is expected."
              )
              None
            }
          case None =>
            if (options.contains(input))
              Some(input)
            else {
              System.err.println(
                s"Unable to parse input: integer value from 0 to $range is expected."
              )
              None
            }
        }
    }

    override def confirmOperation(msg: String): Option[Boolean] = ConfirmOperation(msg).run

    override def chooseOne(msg: String, options: List[String]): Option[String] =
      ChooseOne(msg, options).run

  }
}
