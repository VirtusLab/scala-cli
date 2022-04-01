package scala.cli.graal

import java.io.File

object CoursierCacheProcessor {
  def main(args: Array[String]) = {
    val List(coursierCache, classpath) = args.toList
    val cache                          = CoursierCache(BytecodeProcessor.toPath(coursierCache))

    val newCp = BytecodeProcessor.processClasspath(classpath, cache).map(_.nioPath)

    println(newCp.mkString(File.pathSeparator))
  }
}
