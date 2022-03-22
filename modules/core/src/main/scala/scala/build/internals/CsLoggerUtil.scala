package scala.build.internal

import coursier.cache.FileCache
import coursier.cache.loggers.RefreshLogger
import coursier.jvm.JavaHome
import coursier.util.Task

object CsLoggerUtil {

  // All of these methods are a bit flaky…

  implicit class CsCacheExtensions(private val cache: FileCache[Task]) extends AnyVal {
    def withMessage(message: String): FileCache[Task] =
      cache.logger match {
        case _: RefreshLogger =>
          var displayed = false
          val logger = RefreshLogger.create(
            CustomProgressBarRefreshDisplay.create(
              keepOnScreen = false,
              if (!displayed) {
                System.err.println(message)
                displayed = true
              },
              ()
            )
          )
          cache.withLogger(logger)
        case _ => cache
      }
  }
  implicit class CsJavaHomeExtensions(private val javaHome: JavaHome) extends AnyVal {
    def withMessage(message: String): JavaHome =
      javaHome.cache.map(_.archiveCache.cache) match {
        case Some(f: FileCache[Task]) =>
          val cache0 = f.withMessage(message)
          javaHome.withCache(
            javaHome.cache.map(c => c.withArchiveCache(c.archiveCache.withCache(cache0)))
          )
        case _ => javaHome
      }
  }
}
