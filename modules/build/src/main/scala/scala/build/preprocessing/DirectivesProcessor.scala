package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.{Path, Value}

import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.preprocessing.directives.DirectiveHandler
import scala.collection.JavaConverters._

object DirectivesProcessor {

  private def processScala(value: Any): BuildOptions = {

    val versions = Some(value)
      .toList
      .collect {
        case list: java.util.List[_] =>
          list.asScala.collect { case v: String => v }.toList
        case v: String =>
          List(v)
      }
      .flatten
      .map(_.filter(!_.isSpaceChar))
      .filter(_.nonEmpty)
      .distinct

    versions match {
      case Nil => BuildOptions()
      case v :: Nil =>
        BuildOptions(
          scalaOptions = ScalaOptions(
            scalaVersion = Some(v)
          )
        )
      case _ =>
        val highest = versions.maxBy(coursier.core.Version(_))
        BuildOptions(
          scalaOptions = ScalaOptions(
            scalaVersion = Some(highest)
          )
        )
    }
  }

  def process(
    directives: Map[Path, Seq[Value[_]]],
    handlers: Seq[DirectiveHandler],
    cwd: ScopePath
  ): Either[BuildException, BuildOptions] = {

    val values = directives.map {
      case (k, v) =>
        k.getPath.asScala.mkString(".") -> v.map(_.get: Any)
    }

    val handlersMap = handlers
      .flatMap { handler =>
        handler.keys.map(k => k -> (handler.handleValues _))
      }
      .toMap

    values
      .iterator
      .flatMap {
        case (k, v) =>
          handlersMap.get(k).iterator.map(_(v, cwd))
      }
      .toVector
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.foldLeft(BuildOptions())(_ orElse _))
  }
}
