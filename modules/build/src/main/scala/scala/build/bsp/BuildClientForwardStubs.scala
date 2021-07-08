package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

trait BuildClientForwardStubs extends b.BuildClient {
  protected def forwardToOpt: Option[b.BuildClient]
  override def onBuildLogMessage(params: b.LogMessageParams): Unit =
    forwardToOpt.foreach(_.onBuildLogMessage(params))
  override def onBuildPublishDiagnostics(params: b.PublishDiagnosticsParams): Unit =
    forwardToOpt.foreach(_.onBuildPublishDiagnostics(params))
  override def onBuildShowMessage(params: b.ShowMessageParams): Unit =
    forwardToOpt.foreach(_.onBuildShowMessage(params))
  override def onBuildTargetDidChange(params: b.DidChangeBuildTarget): Unit =
    forwardToOpt.foreach(_.onBuildTargetDidChange(params))
  override def onBuildTaskFinish(params: b.TaskFinishParams): Unit =
    forwardToOpt.foreach(_.onBuildTaskFinish(params))
  override def onBuildTaskProgress(params: b.TaskProgressParams): Unit =
    forwardToOpt.foreach(_.onBuildTaskProgress(params))
  override def onBuildTaskStart(params: b.TaskStartParams): Unit =
    forwardToOpt.foreach(_.onBuildTaskStart(params))
}
