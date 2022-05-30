package scala.build.actionable

import scala.build.Logger
import scala.build.options.BuildOptions

case object ActionablePreprocessor {
  val actionableHandlers = Seq(
    ActionableDependencyHandler
  )

  def generateActionableDiagnostics(
    options: BuildOptions,
    logger: Logger
  ): Seq[ActionableDiagnostic] =
    actionableHandlers.flatMap { handler =>
      val values = handler.extractPositionedOptions(options)
      val (errors, actionableDiagnostic) =
        values.map(v => handler.createActionableDiagnostic(v, options)).partitionMap(identity)
      errors.map(logger.debug(_))
      actionableDiagnostic.flatten
    }

  def process(options: BuildOptions, logger: Logger): Unit = {
    val actionableDiagnostics = generateActionableDiagnostics(options, logger)
    actionableDiagnostics.foreach(ad => logger.log(Seq(ad.toDiagnostic)))
  }
}
