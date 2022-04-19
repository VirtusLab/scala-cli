package scala.cli.graal

import java.io.File
import java.nio.channels.FileChannel

object CoursierCacheProcessor {
  def main(args: Array[String]) = {
    val List(cacheDir, classpath) = args.toList
    val cache                     = DirCache(os.Path(cacheDir, os.pwd))

    val newCp = BytecodeProcessor.processClassPath(classpath, cache).map(_.nioPath)

    println(newCp.mkString(File.pathSeparator))
  }
}
