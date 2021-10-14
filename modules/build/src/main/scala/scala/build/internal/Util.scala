package scala.build.internal

import java.io.{File, PrintStream}
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import scala.build.Os

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
      val counter        = new AtomicInteger
      def threadNumber() = counter.incrementAndGet()
      def newThread(r: Runnable) =
        new Thread(r, s"$prefix-thread-${threadNumber()}") {
          setDaemon(true)
          setPriority(Thread.NORM_PRIORITY)
        }
    }

  private implicit class DependencyOps(private val dep: dependency.Dependency) extends AnyVal {
    def toCs: coursier.Dependency = {
      val mod = coursier.Module(
        coursier.Organization(dep.organization),
        coursier.ModuleName(dep.name),
        dep.attributes
      )
      var dep0 = coursier.Dependency(mod, dep.version)
      if (dep.exclude.nonEmpty)
        dep0 = dep0.withExclusions {
          dep.exclude.toSet[dependency.Module].map { mod =>
            (coursier.Organization(mod.organization), coursier.ModuleName(mod.name))
          }
        }
      for (clOpt <- dep.userParams.get("classifier"); cl <- clOpt)
        dep0 = dep0.withConfiguration(coursier.core.Configuration(cl))
      for (_ <- dep.userParams.get("intransitive"))
        dep0 = dep0.withTransitive(false)
      // FIXME
      // for (tpeOpt <- dep.userParams.get("type"); tpe <- tpeOpt)
      //   dep0 = dep0.withType(tpe)
      dep0
    }
  }
  implicit class ScalaDependencyOps(private val dep: dependency.AnyDependency) extends AnyVal {
    def toCs(params: dependency.ScalaParameters): coursier.Dependency =
      dep.applyParams(params).toCs
  }

  def isFullScalaVersion(sv: String): Boolean =
    sv.count(_ == '.') >= 2 && !sv.endsWith(".")

  def printablePath(p: os.Path): String =
    printablePath(p, Os.pwd, File.separator)
  def printablePath(p: os.Path, cwd: os.Path, sep: String): String =
    if (p.startsWith(cwd)) (Iterator(".") ++ p.relativeTo(cwd).segments.iterator).mkString(sep)
    else p.toString

  def printablePath(p: Either[String, os.Path]): String =
    p.fold(identity, printablePath(_))
}
