package sclicheck

object TestUtil {

  def withTmpDir[T](prefix: String)(f: os.Path => T): T = {
    val tmpDir = os.temp.dir(prefix = prefix)
    try f(tmpDir)
    finally tryRemoveAll(tmpDir)
  }

  def tryRemoveAll(f: os.Path): Unit =
    try os.remove.all(f)
    catch {
      case ex: java.nio.file.FileSystemException =>
        System.err.println(s"Could not remove $f ($ex), ignoring it.")
    }

  lazy val scalaCliPath = Option(System.getenv("SCLICHECK_SCALA_CLI")).getOrElse {
    sys.error("SCLICHECK_SCALA_CLI not set")
  }

}
