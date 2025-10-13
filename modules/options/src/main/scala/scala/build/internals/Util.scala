package scala.build.internal
import coursier.core.{Dependency, MinimizedExclusions, Publication, VariantPublication}
import coursier.util.Artifact
import coursier.version.VersionConstraint
import dependency.NoAttributes

import java.io.{File, PrintStream}
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import scala.build.Ops.EitherSeqOps
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  NoScalaVersionProvidedError,
  UnsupportedGradleModuleVariantError
}
import scala.build.{Os, Positioned}

object Util {

  def printException(t: Throwable, out: PrintStream = System.err): Unit =
    if (t != null) {
      out.println(t)
      for (l <- t.getStackTrace)
        out.println(s"  $l")
      printException(t.getCause, out)
    }
  def printException(t: Throwable, out: => String => Unit): Unit =
    if (t != null) {
      out(t.toString)
      for (l <- t.getStackTrace)
        out(s"  $l")
      printException(t.getCause, out)
    }

  def daemonThreadFactory(prefix: String): ThreadFactory =
    new ThreadFactory {
      val counter                = new AtomicInteger
      def threadNumber()         = counter.incrementAndGet()
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
        case None         =>
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
      var dep0 = coursier.Dependency(mod, VersionConstraint(dep.version))
      if (dep.exclude.nonEmpty)
        dep0 = dep0.withMinimizedExclusions {
          MinimizedExclusions {
            dep.exclude.toSet[dependency.Module].map { mod =>
              (coursier.Organization(mod.organization), coursier.ModuleName(mod.name))
            }
          }
        }
      for (clOpt <- dep.userParams.find(_._1 == "classifier").map(_._2); cl <- clOpt)
        dep0 = dep0.withPublication(dep0.publication.withClassifier(coursier.core.Classifier(cl)))
      for (tpeOpt <- dep.userParams.find(_._1 == "type").map(_._2); tpe <- tpeOpt)
        dep0 = dep0.withPublication(dep0.publication.withType(coursier.core.Type(tpe)))
      for (extOpt <- dep.userParams.find(_._1 == "ext").map(_._2); ext <- extOpt)
        dep0 = dep0.withPublication(dep0.publication.withExt(coursier.core.Extension(ext)))
      for (_ <- dep.userParams.find(_._1 == "intransitive"))
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
        case None         =>
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

  extension (fullDetailedArtifacts: Seq[(
    Dependency,
    Either[VariantPublication, Publication],
    Artifact,
    Option[File]
  )]) {
    def safeFullDetailedArtifacts: Either[BuildException, Seq[(
      Dependency,
      Publication,
      Artifact,
      Option[File]
    )]] =
      fullDetailedArtifacts
        .map {
          case (dependency, Right(publication), artifact, maybeFile) =>
            Right((dependency, publication, artifact, maybeFile))
          case (_, Left(variantPublication), _, _) =>
            // TODO: Add support for Gradle Module variants
            Left(UnsupportedGradleModuleVariantError(variantPublication))
        }
        .sequence
        .left
        .map(CompositeBuildException(_))
  }
}
