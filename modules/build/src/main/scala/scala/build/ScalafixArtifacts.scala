package scala.build

import coursier.cache.FileCache
import coursier.core.Repository
import coursier.util.Task
import dependency.*
import org.apache.commons.compress.archivers.zip.ZipFile
import os.Path

import java.io.ByteArrayInputStream
import java.util.Properties

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.internal.CsLoggerUtil.*

final case class ScalafixArtifacts(
  scalafixJars: Seq[os.Path],
  toolsJars: Seq[os.Path]
)

object ScalafixArtifacts {

  def artifacts(
    scalaVersion: String,
    externalRulesDeps: Seq[Positioned[AnyDependency]],
    extraRepositories: Seq[Repository],
    logger: Logger,
    cache: FileCache[Task]
  ): Either[BuildException, ScalafixArtifacts] =
    either {
      val scalafixProperties =
        value(fetchOrLoadScalafixProperties(extraRepositories, logger, cache))
      val key =
        value(scalafixPropsKey(scalaVersion))
      val fetchScalaVersion = scalafixProperties.getProperty(key)

      val scalafixDeps =
        Seq(dep"ch.epfl.scala:scalafix-cli_$fetchScalaVersion:${Constants.scalafixVersion}")

      val scalafix =
        value(
          Artifacts.artifacts(
            scalafixDeps.map(Positioned.none),
            extraRepositories,
            None,
            logger,
            cache.withMessage(s"Downloading scalafix-cli ${Constants.scalafixVersion}")
          )
        )

      val scalaParameters =
        // Scalafix for scala 3 uses 2.13-published community rules
        // https://github.com/scalacenter/scalafix/issues/2041
        if (scalaVersion.startsWith("3")) ScalaParameters(Constants.defaultScala213Version)
        else ScalaParameters(scalaVersion)

      val tools =
        value(
          Artifacts.artifacts(
            externalRulesDeps,
            extraRepositories,
            Some(scalaParameters),
            logger,
            cache.withMessage(s"Downloading scalafix.deps")
          )
        )

      ScalafixArtifacts(scalafix.map(_._2), tools.map(_._2))
    }

  private def fetchOrLoadScalafixProperties(
    extraRepositories: Seq[Repository],
    logger: Logger,
    cache: FileCache[Task]
  ): Either[BuildException, Properties] =
    either {
      val cacheDir  = Directories.directories.cacheDir / "scalafix-props-cache"
      val cachePath = cacheDir / s"scalafix-interfaces-${Constants.scalafixVersion}.properties"

      val content =
        if (!os.exists(cachePath)) {
          val interfacesJar = value(fetchScalafixInterfaces(extraRepositories, logger, cache))
          val propsData     = value(readScalafixProperties(interfacesJar))
          if (!os.exists(cacheDir)) os.makeDir(cacheDir)
          os.write(cachePath, propsData)
          propsData
        }
        else os.read(cachePath)
      val props  = new Properties()
      val stream = new ByteArrayInputStream(content.getBytes())
      props.load(stream)
      props
    }

  private def fetchScalafixInterfaces(
    extraRepositories: Seq[Repository],
    logger: Logger,
    cache: FileCache[Task]
  ): Either[BuildException, Path] =
    either {
      val scalafixInterfaces = dep"ch.epfl.scala:scalafix-interfaces:${Constants.scalafixVersion}"

      val fetchResult =
        value(
          Artifacts.artifacts(
            List(scalafixInterfaces).map(Positioned.none),
            extraRepositories,
            None,
            logger,
            cache.withMessage(s"Downloading scalafix-interfaces ${scalafixInterfaces.version}")
          )
        )

      val expectedJarName = s"scalafix-interfaces-${Constants.scalafixVersion}.jar"
      val interfacesJar   = fetchResult.collectFirst {
        case (_, path) if path.last == expectedJarName => path
      }

      value(
        interfacesJar.toRight(new BuildException("Failed to found scalafix-interfaces jar") {})
      )
    }

  private def readScalafixProperties(jar: Path): Either[BuildException, String] = {
    import scala.jdk.CollectionConverters.*
    val zipFile = new ZipFile(jar.toNIO)
    val entry   = zipFile.getEntries().asScala.find(entry =>
      entry.getName() == "scalafix-interfaces.properties"
    )
    val out =
      entry.toRight(new BuildException("Failed to found scalafix properties") {})
        .map { entry =>
          val stream = zipFile.getInputStream(entry)
          val bytes  = stream.readAllBytes()
          new String(bytes)
        }
    zipFile.close()
    out
  }

  private def scalafixPropsKey(scalaVersion: String): Either[BuildException, String] = {
    val regex = "(\\d)\\.(\\d+).+".r
    scalaVersion match {
      case regex("2", "12")              => Right("scala212")
      case regex("2", "13")              => Right("scala213")
      case regex("3", x) if x.toInt <= 3 => Right("scala3LTS")
      case regex("3", _)                 => Right("scala3Next")
      case _                             =>
        Left(new BuildException(s"Scalafix is not supported for Scala version: $scalaVersion") {})
    }

  }

}
