package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.{Path, Value}
import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.options.{BuildOptions, ClassPathOptions, ScalaOptions}
import scala.collection.JavaConverters._

object DirectivesProcessor {

  private val processors = Map(
    "lib" -> (processLib _)
  )

  private def processLib(values: Seq[Any]): BuildOptions = {

    pprint.stderr.log(values)

    val extraDependencies = values
      .collect {
        case list: java.util.List[_] =>
          list
            .asScala
            .map {
              case v: Value[_] => v.get()
            }
            .collect {
              case s: String => s
            }
            .toVector
        case s: String =>
          Vector(s)
      }
      .flatten
      .map { dep =>
        // Really necessary? (might already be handled by the coursier-dependency library)
        val dep0 = dep.filter(!_.isSpaceChar)

        parseDependency(dep0)
      }

    BuildOptions(
      classPathOptions = ClassPathOptions(
        extraDependencies = extraDependencies
      )
    )
  }

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

  def process(directives: Map[Path, Seq[Value[_]]]): BuildOptions = {

    val values = directives.map {
      case (k, v) =>
        k.getPath.asScala.mkString(".") -> v.map(_.get: Any)
    }

    values
      .iterator
      .flatMap {
        case (k, v) =>
          processors.get(k).iterator.map { f =>
            f(v)
          }
      }
      .foldLeft(BuildOptions())(_ orElse _)
  }

  private def parseDependency(str: String): AnyDependency =
    DependencyParser.parse(str) match {
      case Left(msg)  => sys.error(s"Malformed dependency '$str': $msg")
      case Right(dep) => dep
    }
}
