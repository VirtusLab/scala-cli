package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

trait LoggingBuildClient extends b.BuildClient {
  protected def underlying: b.BuildClient
  override def onBuildLogMessage(params: b.LogMessageParams): Unit =
    underlying.onBuildLogMessage(pprint.stderr.log(params))
  override def onBuildPublishDiagnostics(params: b.PublishDiagnosticsParams): Unit =
    underlying.onBuildPublishDiagnostics(pprint.stderr.log(params))
  override def onBuildShowMessage(params: b.ShowMessageParams): Unit =
    underlying.onBuildShowMessage(pprint.stderr.log(params))
  override def onBuildTargetDidChange(params: b.DidChangeBuildTarget): Unit =
    underlying.onBuildTargetDidChange(pprint.stderr.log(params))
  override def onBuildTaskFinish(params: b.TaskFinishParams): Unit =
    underlying.onBuildTaskFinish(pprint.stderr.log(params))
  override def onBuildTaskProgress(params: b.TaskProgressParams): Unit =
    underlying.onBuildTaskProgress(pprint.stderr.log(params))
  override def onBuildTaskStart(params: b.TaskStartParams): Unit =
    underlying.onBuildTaskStart(pprint.stderr.log(params))
}
