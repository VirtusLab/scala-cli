package scala.build.internal

// adapted from https://github.com/com-lihaoyi/Ammonite/blob/9be39debc367abad5f5541ef58f4b986b2a8d045/amm/util/src/main/scala/ammonite/util/Util.scala

object AmmUtil {
  val upPathSegment = "^"
  def pathToPackageWrapper(relPath0: os.SubPath): (Seq[Name], Name) = {
    val relPath  = relPath0 / os.up
    val fileName = relPath0.last
    val pkg      = relPath.segments.map(Name(_))
    val wrapper = fileName.lastIndexOf('.') match {
      case -1 => fileName
      case i  => fileName.take(i)
    }

    (pkg, Name(wrapper))
  }

  def encodeScalaSourcePath(path: Seq[Name]) = path.map(_.backticked).mkString(".")

  def normalizeNewlines(s: String) = s.replace("\r", "").replace("\n", System.lineSeparator())
}
