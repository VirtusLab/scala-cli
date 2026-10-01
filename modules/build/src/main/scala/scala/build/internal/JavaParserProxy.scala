package scala.build.internal

import scala.build.errors.BuildException

/** Helper to get class names from Java sources
  *
  * See [[JavaParserProxyJvm]] for the implementation that runs things in memory using
  * java-class-name from the class path, and [[JavaParserProxyBinary]] for the implementation that
  * downloads and runs a java-class-name binary.
  */
trait JavaParserProxy {

  /** Extracts the class name of a Java source, using the dotty Java parser.
    *
    * @param content
    *   the Java source to extract a class name from
    * @param sourceFileName
    *   the name of the file the source is going to be written to, which names the implicit class of
    *   compact source files (JEP 512)
    * @return
    *   either some class name (if one was found) or none (if none was found), or a
    *   [[BuildException]]
    */
  def className(
    content: Array[Byte],
    sourceFileName: String
  ): Either[BuildException, Option[String]]
}
