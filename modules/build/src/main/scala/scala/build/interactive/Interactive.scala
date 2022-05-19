package scala.build.interactive

import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.io.StdIn.readLine

case class Interactive(options: BuildOptions) {

  private val isInteractive = options.internal.interactive.getOrElse(false)

  def chooseOne[E <: BuildException](
    msg: String,
    options: List[String],
    fallbackError: E
  ): Either[E, String] =
    if (isInteractive && coursier.paths.Util.useAnsiOutput()) {
      System.err.println(msg)
      options.zipWithIndex.foreach {
        case (option, index) => System.err.println(s"[$index] $option")
      }
      val response      = readLine()
      val inputIndexOpt = parseIndexInput(response, options.length)
      inputIndexOpt.map(options(_)).toRight(fallbackError)
    }
    else Left(fallbackError)

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
