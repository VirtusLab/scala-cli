package prclassify

/** Tiny KEY=VALUE file format used to pass classification/override results between workflow steps.
  * Lines are trimmed, blanks and `#`-comment lines are ignored.
  */
object KeyValueFile:

  def read(path: os.Path): Map[String, String] =
    if !os.exists(path) then Map.empty
    else
      os.read.lines(path).iterator.flatMap { line =>
        val trimmed = line.trim
        if trimmed.isEmpty || trimmed.startsWith("#") then None
        else
          trimmed.split("=", 2) match
            case Array(k, v) => Some(k.trim -> v.trim)
            case _           => None
      }.toMap

  def writeAll(path: os.Path, entries: Iterable[(String, String)]): Unit =
    val content = entries.map((k, v) => s"$k=$v").mkString("", "\n", "\n")
    os.write.over(path, content, createFolders = true)

  def appendAll(path: os.Path, entries: Iterable[(String, String)]): Unit =
    if entries.isEmpty then ()
    else
      val content = entries.map((k, v) => s"$k=$v").mkString("", "\n", "\n")
      os.write.append(path, content)
