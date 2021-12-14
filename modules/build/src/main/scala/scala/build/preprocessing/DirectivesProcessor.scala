package scala.build.preprocessing
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.{BuildOptions, ConfigMonoid, ScalaOptions}
import scala.build.preprocessing.directives.{DirectiveHandler, ProcessedDirective, StrictDirective}
import scala.collection.JavaConverters._

object DirectivesProcessor {

  case class DirectivesProcessorOutput[T](
    global: T,
    scoped: Seq[Scoped[T]],
    unused: Seq[StrictDirective]
  )

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
    directives: Seq[StrictDirective],
    handlers: Seq[DirectiveHandler[T]],
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, DirectivesProcessorOutput[T]] = {
    val configMonoidInstance = implicitly[ConfigMonoid[T]]

//    val values = directives.map {
//      case (k, v) =>
//        k.getPath.asScala.mkString(".") -> v
//    }

    val handlersMap = handlers
      .flatMap { handler =>
        handler.keys.map(k => k -> (handler.handleValues _))
      }
      .toMap

    val unused = directives.filter(d => !handlersMap.contains(d.key))

    val res = directives
      .iterator
      .flatMap {
        case d @ StrictDirective(k, _) =>
          handlersMap.get(k).iterator.map(_(d, path, cwd))
      }
      .toVector
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.foldLeft((configMonoidInstance.zero, Seq.empty[Scoped[T]])) {
        case ((globalAcc, scopedAcc), ProcessedDirective(global, scoped)) => (
            global.fold(globalAcc)(ns => configMonoidInstance.orElse(ns, globalAcc)),
            scopedAcc ++ scoped
          )
      })
    res.map {
      case (g, s) => DirectivesProcessorOutput(g, s, unused)
    }
  }
}
