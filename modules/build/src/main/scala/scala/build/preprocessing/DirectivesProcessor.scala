package scala.build.preprocessing
import scala.build.Logger
import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException, DirectiveErrors}
import scala.build.internal.util.WarningMessages.experimentalDirectiveUsed
import scala.build.options.ConfigMonoid
import scala.build.preprocessing.directives.{
  DirectiveHandler,
  DirectiveUtil,
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
    logger: Logger,
    allowRestrictedFeatures: Boolean
  ): Either[BuildException, DirectivesProcessorOutput[T]] = {
    val configMonoidInstance = implicitly[ConfigMonoid[T]]

    def handleValues(handler: DirectiveHandler[T])(
      scopedDirective: ScopedDirective,
      logger: Logger
    ) =
      if !allowRestrictedFeatures && (handler.isRestricted || handler.isExperimental) then
        val powerDirectiveType = if handler.isExperimental then "experimental" else "restricted"
        val msg =
          s"""This directive is $powerDirectiveType.
             |Please run it with the '--power' flag or turn this flag on globally by running 'config power true'""".stripMargin
        Left(DirectiveErrors(
          ::(msg, Nil),
          DirectiveUtil.positions(scopedDirective.directive.values, path)
        ))
      else
        if handler.isExperimental then
          logger.message(experimentalDirectiveUsed(scopedDirective.directive.toString))
        handler.handleValues(scopedDirective, logger)

    val handlersMap = handlers
      .flatMap { handler =>
        handler.keys.map(k => k -> handleValues(handler))
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
