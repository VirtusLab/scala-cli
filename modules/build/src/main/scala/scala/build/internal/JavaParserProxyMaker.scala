package scala.build.internal

import java.util.function.Supplier

import scala.build.Logger

/** On the JVM, provides [[JavaParserProxyJvm]] as [[JavaParserProxy]] instance.
  *
  * From native launchers, [[JavaParserProxyMakerSubst]] takes over this, and gives
  * [[JavaParserProxyBinary]] instead.
  *
  * That way, no reference to [[JavaParserProxyJvm]] remains in the native call graph, and that
  * class and those it pulls (the java-class-name classes, which includes parts of the dotty parser)
  * are not embedded the native launcher.
  *
  * Note that this is a class and not an object, to make it easier to write substitutions for that
  * in Java.
  */
class JavaParserProxyMaker {
  def get(
    archiveCache: Object, // Actually a ArchiveCache[Task], but having issues with the higher-kind type param from Java…
    javaClassNameVersionOpt: Option[String],
    logger: Logger,
    javaCommand: Supplier[String]
  ): JavaParserProxy =
    new JavaParserProxyJvm
}
