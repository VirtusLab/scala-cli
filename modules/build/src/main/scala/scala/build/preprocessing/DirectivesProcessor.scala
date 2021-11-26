package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.{Path, Value}

import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.{BuildOptions, ConfigMonoid, ScalaOptions}
import scala.build.preprocessing.directives.{DirectiveHandler, StrictDirective}
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

  def process[T: ConfigMonoid](
    directives: Seq[(Path, Seq[Value[_]])],
    handlers: Seq[DirectiveHandler[T]],
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, (T, Seq[Scoped[T]])] = {
    val configMonoidInstance = implicitly[ConfigMonoid[T]]

    val values = directives.map {
      case (k, v) =>
        k.getPath.asScala.mkString(".") -> v
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
          handlersMap.get(k).iterator.map(_(StrictDirective(k, v), path, cwd))
      }
      .toVector
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.foldLeft((configMonoidInstance.zero, Seq.empty[Scoped[T]])) {
        case ((nonscopedAcc, scopedAcc), (nonscoped, scoped)) => (
            nonscoped.fold(nonscopedAcc)(ns => configMonoidInstance.orElse(ns, nonscopedAcc)),
            scopedAcc ++ scoped
          )
      })
  }
}
