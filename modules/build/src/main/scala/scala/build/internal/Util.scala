package scala.build.internal

import java.io.PrintStream
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.JavaConverters._
import java.util.AbstractMap.SimpleEntry
import java.util.Map.Entry

object Util {

  def printException(t: Throwable, out: PrintStream = System.err): Unit =
    if (t != null) {
      out.println(t)
      for (l <- t.getStackTrace)
        out.println(s"  $l")
      printException(t.getCause, out)
    }

  def daemonThreadFactory(prefix: String): ThreadFactory =
    new ThreadFactory {
      val counter = new AtomicInteger
      def threadNumber() = counter.incrementAndGet()
      def newThread(r: Runnable) =
        new Thread(r, s"$prefix-thread-${threadNumber()}") {
          setDaemon(true)
          setPriority(Thread.NORM_PRIORITY)
        }
    }

  implicit class DependencyOps(private val dep: dependency.Dependency) extends AnyVal {
    def toApi: coursierapi.Dependency = {
      val apiMod = coursierapi.Module.of(dep.organization, dep.name, dep.attributes.asJava)
      val apiDep = coursierapi.Dependency.of(apiMod, dep.version)
      if (dep.exclude.nonEmpty)
        apiDep.withExclusion(dep.exclude.toSet[dependency.Module].map(mod => new SimpleEntry(mod.organization, mod.name): Entry[String, String]).asJava)
      for (clOpt <- dep.userParams.get("classifier"); cl <- clOpt)
        apiDep.withClassifier(cl)
      for (_ <- dep.userParams.get("intransitive"))
        apiDep.withTransitive(true)
      for (tpeOpt <- dep.userParams.get("type"); tpe <- tpeOpt)
        apiDep.withType(tpe)
      apiDep
    }
  }
  implicit class ScalaDependencyOps(private val dep: dependency.AnyDependency) extends AnyVal {
    def toApi(params: dependency.ScalaParameters): coursierapi.Dependency =
      dep.applyParams(params).toApi
  }

  def isFullScalaVersion(sv: String): Boolean =
    sv.count(_ == '.') >= 2 && !sv.endsWith(".")
}