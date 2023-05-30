package scala.build.postprocessing

import scala.build.internal.WrapperParams

object LineConversion {
  def scalaLineToScLine(lineScala: Int, wrapperParamsOpt: Option[WrapperParams]): Option[Int] =
    wrapperParamsOpt match {
      case Some(wrapperParams) =>
        val lineSc = lineScala - wrapperParams.topWrapperLineCount

        if (lineSc >= 0 && lineSc < wrapperParams.userCodeLineCount) Some(lineSc) else None
      case _ => None
    }

  def scalaLineToScLineShift(wrapperParamsOpt: Option[WrapperParams]): Int =
    wrapperParamsOpt match {
      case Some(wrapperParams) => wrapperParams.topWrapperLineCount * -1
      case _                   => 0
    }
}
