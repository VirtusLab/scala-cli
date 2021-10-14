package scala.build.preprocessing

final case class ScopePath(
  root: String,
  path: os.SubPath
) {
  def /(subPath: os.PathChunk): ScopePath =
    copy(path = path / subPath)
}

object ScopePath {
  def fromPath(path: os.Path): ScopePath = {
    def root(p: os.Path): os.Path =
      if (p.segmentCount > 0) root(p / os.up) else p
    val root0 = root(path)
    ScopePath(root0.toString, path.subRelativeTo(root0))
  }
}
