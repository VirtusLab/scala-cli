package scala.build.interactive
import scala.build.options.BuildOptions
import scala.io.StdIn.readLine

case class Interactive(options: BuildOptions) {

  private val isInteractive = options.internal.interactive.getOrElse(false)

  sealed abstract class Action[V] extends Product with Serializable {
    def msg: String
    def action: Option[V]
    final def run: Option[V] =
      if (isInteractive && coursier.paths.Util.useAnsiOutput())
        action
      else None
  }

  case class ConfirmOperation(msg: String) extends Action[Boolean] {
    override def action: Option[Boolean] = {
      System.err.println(s"$msg [Y/n]")
      val response = readLine()
      if (response.toLowerCase == "y")
        Some(true)
      else {
        System.err.println("Abort")
        None
      }
    }
  }

  case class ChooseOne(msg: String, options: List[String])
      extends Action[String] {
    override def action: Option[String] = {
      System.err.println(msg)
      options.zipWithIndex.foreach {
        case (option, index) => System.err.println(s"[$index] $option")
      }
      val response      = readLine()
      val inputIndexOpt = parseIndexInput(response, options.length - 1)
      inputIndexOpt.map(options(_))
    }

    private def parseIndexInput(input: String, range: Int): Option[Int] = {
      val indexOpt = input.toIntOption
      indexOpt match {
        case Some(index) =>
          val isInRange = index <= range && index >= 0
          if (isInRange) Some(index)
          else {
            System.err.println(
              s"The input index number is invalid, integer value from 0 to $range is expected."
            )
            None
          }
        case _ =>
          System.err.println(s"Unable to parse input: integer value from 0 to $range is expected.")
          None
      }
    }
  }

  def confirmOperation[E](
    msg: String,
    fallbackError: () => E
  ): Either[E, Boolean] = ConfirmOperation(msg).run.toRight(fallbackError())

  def chooseOne[E](
    msg: String,
    options: List[String],
    fallbackError: E
  ): Either[E, String] = ChooseOne(msg, options).run.toRight(fallbackError)

  def chooseOne(
    msg: String,
    options: List[String],
    fallbackError: () => Nothing
  ): String = ChooseOne(msg, options).run.getOrElse(fallbackError())

}
