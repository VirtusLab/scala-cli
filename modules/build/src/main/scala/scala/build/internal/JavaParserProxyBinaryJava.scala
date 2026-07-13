package scala.build.internal

import coursier.cache.ArchiveCache
import coursier.util.Task

import java.util.function.Supplier

import scala.build.Logger

object JavaParserProxyBinaryJava {

  /** For internal use only
    *
    * Passing archiveCache as an Object, to work around issues with higher-kind type params from
    * Java code.
    */
  def apply(
    archiveCache: Object,
    logger: Logger,
    javaClassNameVersionOpt: Option[String],
    javaCommand: Supplier[String]
  ): JavaParserProxy =
    new JavaParserProxyBinary(
      archiveCache.asInstanceOf[ArchiveCache[Task]],
      javaClassNameVersionOpt,
      logger,
      () => javaCommand.get()
    )
}
