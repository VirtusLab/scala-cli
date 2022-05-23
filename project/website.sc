import $file.deps, deps.Scala

private def lastTableLine(path: os.Path, colCount: Int): Seq[String] = {
  val content = os.read(path)
  val lines   = content.linesIterator.toVector
  val (line, idx) = lines
    .zipWithIndex
    .filter(_._1.count(_ == '|') == colCount + 1)
    .lastOption
    .getOrElse {
      sys.error(s"No table of expected shape found in $path")
    }
  val cells = line
    .split("\\|", -1)
    .toSeq
    .map(_.trim)
    .drop(1)
    .dropRight(1)
  assert(cells.length == colCount)
  cells
}

def checkMainScalaVersions(path: os.Path): Unit = {
  val cells     = lastTableLine(path, 4)
  val lastCells = cells.drop(1)
  assert(lastCells.length == 3)
  val expectedLastCells = Seq(Scala.scala3, Scala.scala213, Scala.scala212)
  if (lastCells != expectedLastCells)
    sys.error(
      s"Unexpected last line in Scala version table in $path " +
        s"(expected ${expectedLastCells.mkString(", ")}, got ${lastCells.mkString(", ")})"
    )
}

def checkScalaJsVersions(path: os.Path): Unit = {
  val cells     = lastTableLine(path, 2)
  val lastCells = cells.drop(1)
  assert(lastCells.length == 1)
  val expectedLastCells = Seq(Scala.scalaJs)
  if (lastCells != expectedLastCells)
    sys.error(
      s"Unexpected last line in Scala.js version table in $path " +
        s"(expected ${expectedLastCells.mkString(", ")}, got ${lastCells.mkString(", ")})"
    )
}
