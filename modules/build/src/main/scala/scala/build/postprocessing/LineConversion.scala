package scala.build.postprocessing

object LineConversion {
  def scalaLineToScLine(lineScala: Int, startOffsetInScala: Int): Option[Int] = {
    val lineSc = lineScala - startOffsetInScala
    if (lineSc >= 0) Some(lineSc) else None
  }

  def scalaLineToScLineShift(startOffsetInScala: Int): Int = -startOffsetInScala
}
