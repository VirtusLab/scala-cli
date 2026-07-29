package scala.build

import ch.epfl.scala.bsp4j

class BloopTestBuildClient(logger: Logger)
    extends ConsoleBloopBuildClient(logger, keepDiagnostics = false) {
  private var testsRanCount: Int = 0

  def testsRan: Int = testsRanCount

  override def onBuildLogMessage(params: bsp4j.LogMessageParams): Unit = {
    logger.debug("Received onBuildLogMessage from bloop: " + params)
    System.out.println(params.getMessage)
  }

  override def onBuildTaskStart(params: bsp4j.TaskStartParams): Unit = {
    logger.debug("Received onBuildTaskStart from bloop: " + params)
    Option(params.getMessage).foreach(System.out.println)
  }

  override def onBuildTaskFinish(params: bsp4j.TaskFinishParams): Unit = {
    logger.debug("Received onBuildTaskFinish from bloop: " + params)
    Option(params.getMessage).foreach(System.out.println)
    if params.getDataKind == "test-report" then
      params.getData match {
        case report: bsp4j.TestReport =>
          testsRanCount += report.getPassed + report.getFailed + report.getIgnored +
            report.getCancelled + report.getSkipped
        case _ =>
      }
  }

  override def onBuildShowMessage(params: bsp4j.ShowMessageParams): Unit = {
    logger.debug("Received onBuildShowMessage from bloop: " + params)
    System.out.println(params.getMessage)
  }
}

object BloopTestBuildClient {
  def create(logger: Logger): BloopTestBuildClient =
    new BloopTestBuildClient(logger)
}
