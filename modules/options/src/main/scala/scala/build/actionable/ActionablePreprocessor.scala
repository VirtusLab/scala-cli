package scala.build.actionable

import scala.build.Logger
import scala.build.options.BuildOptions

case object ActionablePreprocessor {

  val actionableHandlers = Seq(
    ActionableDependencyHandler
  )

  def generateActionableDiagnostics(options: BuildOptions): Seq[ActionableDiagnostic] =
    actionableHandlers.flatMap { handler =>
      val values = handler.extractValues(options)
      values.map(v => handler.createActionableDiagnostic(v, options))
    }

  def process(options: BuildOptions, logger: Logger): Unit = {

    val actionableDiagnostics = generateActionableDiagnostics(options)
    actionableDiagnostics.foreach(ad => logger.log(Seq(ad.toDiagnostic)))
  }
}
