package scala.build.internal

import scala.build.errors.BuildException
import scala.cli.javaclassname.JavaParser

/** A [[JavaParserProxy]] that relies on java-class-name in the class path, rather than downloading
  * it and running it as an external binary.
  *
  * Should be used from Scala CLI when it's run on the JVM.
  */
class JavaParserProxyJvm extends JavaParserProxy {
  override def className(content: Array[Byte]): Either[BuildException, Option[String]] =
    Right(JavaParser.parseRootPublicClassName(content))
}
