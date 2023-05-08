package scala.build.postprocessing

object LineConversion {
  def scalaLineToScLine(startOffsetInScala: Int): Int => Option[Int] = {
    lineScala =>
      val lineSc = lineScala - startOffsetInScala
      if (lineSc >= 0) Some(lineSc) else None
  }

}
