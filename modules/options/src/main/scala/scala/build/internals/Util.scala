package scala.build.internal

import dependency.NoAttributes

import java.io.{File, PrintStream}
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import scala.build.errors.NoScalaVersionProvidedError
import scala.build.{Os, Positioned}

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

  implicit class ModuleOps(private val mod: dependency.Module) extends AnyVal {
    def toCs: coursier.Module =
      coursier.Module(
        coursier.Organization(mod.organization),
        coursier.ModuleName(mod.name),
        mod.attributes
      )
  }
  implicit class ScalaModuleOps(private val mod: dependency.AnyModule) extends AnyVal {
    def toCs(params: dependency.ScalaParameters): coursier.Module =
      mod.applyParams(params).toCs
    def toCs(paramsOpt: Option[dependency.ScalaParameters])
      : Either[NoScalaVersionProvidedError, coursier.Module] =
      paramsOpt match {
        case Some(params) => Right(toCs(params))
        case None =>
          val isJavaMod = mod.nameAttributes == NoAttributes
          if (isJavaMod)
            Right(mod.asInstanceOf[dependency.Module].toCs)
          else
            Left(new NoScalaVersionProvidedError(Left(mod)))
      }
  }

  implicit class DependencyOps(private val dep: dependency.Dependency) extends AnyVal {
    def toCs: coursier.Dependency = {
      val mod  = dep.module.toCs
      var dep0 = coursier.Dependency(mod, dep.version)
      if (dep.exclude.nonEmpty)
        dep0 = dep0.withExclusions {
          dep.exclude.toSet[dependency.Module].map { mod =>
            (coursier.Organization(mod.organization), coursier.ModuleName(mod.name))
          }
        }
      for (clOpt <- dep.userParams.get("classifier"); cl <- clOpt)
        dep0 = dep0.withPublication(dep0.publication.withClassifier(coursier.core.Classifier(cl)))
      for (tpeOpt <- dep.userParams.get("type"); tpe <- tpeOpt)
        dep0 = dep0.withPublication(dep0.publication.withType(coursier.core.Type(tpe)))
      for (extOpt <- dep.userParams.get("ext"); ext <- extOpt)
        dep0 = dep0.withPublication(dep0.publication.withExt(coursier.core.Extension(ext)))
      for (_ <- dep.userParams.get("intransitive"))
        dep0 = dep0.withTransitive(false)
      dep0
    }
  }
  implicit class ScalaDependencyOps(private val dep: dependency.AnyDependency) extends AnyVal {
    def toCs(params: dependency.ScalaParameters): coursier.Dependency =
      dep.applyParams(params).toCs
    def toCs(paramsOpt: Option[dependency.ScalaParameters])
      : Either[NoScalaVersionProvidedError, coursier.Dependency] =
      paramsOpt match {
        case Some(params) => Right(toCs(params))
        case None =>
          val isJavaDep = dep.module.nameAttributes == NoAttributes && dep.exclude.forall(
            _.nameAttributes == NoAttributes
          )
          if (isJavaDep)
            Right(dep.asInstanceOf[dependency.Dependency].toCs)
          else
            Left(new NoScalaVersionProvidedError(Right(dep)))
      }
  }
  implicit class PositionedScalaDependencyOps(
    private val posDep: Positioned[dependency.AnyDependency]
  ) extends AnyVal {
    def toCs(paramsOpt: Option[dependency.ScalaParameters])
      : Either[NoScalaVersionProvidedError, Positioned[coursier.Dependency]] = {
      val res = posDep.map(_.toCs(paramsOpt))
      res.value
        .left.map(_ => new NoScalaVersionProvidedError(Right(posDep.value), posDep.positions))
        .map(Positioned(res.positions, _))
    }
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
