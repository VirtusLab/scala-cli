package scala.build.preprocessing
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.ConfigMonoid
import scala.build.preprocessing.directives.{
  DirectiveHandler,
  ProcessedDirective,
  ScopedDirective,
  StrictDirective
}

object DirectivesProcessor {

  case class DirectivesProcessorOutput[T](
    global: T,
    scoped: Seq[Scoped[T]],
    unused: Seq[StrictDirective]
  )

  def process[T: ConfigMonoid](
    directives: Seq[StrictDirective],
    handlers: Seq[DirectiveHandler[T]],
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, DirectivesProcessorOutput[T]] = {
    val configMonoidInstance = implicitly[ConfigMonoid[T]]

//    val values = directives.map {
//      case (k, v) =>
//        k.getPath.asScala.mkString(".") -> v
//    }

    val handlersMap = handlers
      .flatMap { handler =>
        handler.keys.map(k => k -> handler.handleValues _)
      }
      .toMap

    val unused = directives.filter(d => !handlersMap.contains(d.key))

    val res = directives
      .iterator
      .flatMap {
        case d @ StrictDirective(k, _) =>
          handlersMap.get(k).iterator.map(_(ScopedDirective(d, path, cwd), logger))
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
