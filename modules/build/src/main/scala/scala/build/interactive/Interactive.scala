package scala.build.interactive

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.io.StdIn.readLine

case class Interactive(options: BuildOptions, logger: Logger) {

  private val isInteractive = options.internal.interactive.getOrElse(false)

  def chooseOne[E <: BuildException](
    msg: String,
    options: List[String],
    fallbackError: E
  ): Either[E, String] =
    if (isInteractive && coursier.paths.Util.useAnsiOutput()) {
      logger.message(msg)
      options.zipWithIndex.foreach {
        case (option, index) => logger.message(s"[$index] $option")
      }
      val response      = readLine()
      val inputIndexOpt = parseIndexInput(response, options.length)
      val chosedOption = for (inputIndex <- inputIndexOpt)
        yield Right(options(inputIndex))

      chosedOption.getOrElse(Left(fallbackError))
    }
    else Left(fallbackError)

  private def parseIndexInput(input: String, range: Int): Option[Int] = {
    val indexOpt = input.toIntOption
    indexOpt match {
      case Some(index) =>
        val isInRange = index <= range && index >= 0
        if (isInRange) Some(index)
        else {
          logger.message(
            s"The input index number is invalid, integer value from 0 to $range is expected."
          )
          None
        }
      case _ =>
        logger.message(s"Unable to parse input: integer value from 0 to $range is expected.")
        None
    }
  }
}
