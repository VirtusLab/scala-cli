package scala.build.internal

// adapted from https://github.com/com-lihaoyi/Ammonite/blob/9be39debc367abad5f5541ef58f4b986b2a8d045/amm/util/src/main/scala/ammonite/util/Util.scala

object AmmUtil {
  val upPathSegment = "^"
  def pathToPackageWrapper(
    flexiblePkgName0: Seq[Name],
    relPath0: os.RelPath
  ): (Seq[Name], Name) = {
    var flexiblePkgName = flexiblePkgName0
    var relPath         = relPath0 / os.up
    val fileName        = relPath0.last
    while (
      flexiblePkgName.length > 1 &&
      flexiblePkgName.last.encoded != upPathSegment &&
      relPath.ups > 0
    ) {
      flexiblePkgName = flexiblePkgName.dropRight(1)
      relPath = os.RelPath(relPath.segments, relPath.ups - 1)
    }
    val pkg = {
      val ups  = Seq.fill(relPath.ups)(upPathSegment)
      val rest = relPath.segments
      flexiblePkgName ++ (ups ++ rest).map(Name(_))
    }
    val wrapper = fileName.lastIndexOf('.') match {
      case -1 => fileName
      case i  => fileName.take(i)
    }

    (pkg, Name(wrapper))
  }

  def encodeScalaSourcePath(path: Seq[Name]) = path.map(_.backticked).mkString(".")

  def normalizeNewlines(s: String) = s.replace("\r", "").replace("\n", System.lineSeparator())
}
