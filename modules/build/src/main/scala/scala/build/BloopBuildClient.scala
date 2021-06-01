package scala.build

import java.io.File
import java.net.URI
import java.nio.file.Paths

import ch.epfl.scala.bsp4j

import scala.collection.JavaConverters._
import scala.collection.mutable

class BloopBuildClient(
  logger: Logger,
  generatedSources: Map[os.Path, (os.Path, Int)],
  keepDiagnostics: Boolean = false
) extends bsp4j.BuildClient {

  protected var printedStart = false
  private val gray = "\u001b[90m"
  private val reset = Console.RESET

  private var diagnostics0 = new mutable.ListBuffer[(os.Path, bsp4j.Diagnostic)]

  def diagnostics: Option[Seq[(os.Path, bsp4j.Diagnostic)]] =
    if (keepDiagnostics) Some(diagnostics0.result())
    else None

  private def postProcessDiagnostic(path: os.Path, diag: bsp4j.Diagnostic): Option[(os.Path, bsp4j.Diagnostic)] =
    generatedSources.get(path).collect {
      case (originalPath, lineOffset) if diag.getRange.getStart.getLine + lineOffset >= 0 && diag.getRange.getEnd.getLine + lineOffset >= 0 =>
        val start = new bsp4j.Position(diag.getRange.getStart.getLine + lineOffset, diag.getRange.getStart.getCharacter)
        val end = new bsp4j.Position(diag.getRange.getEnd.getLine + lineOffset, diag.getRange.getEnd.getCharacter)
        val range = new bsp4j.Range(start, end)

        val updatedDiag = new bsp4j.Diagnostic(range, diag.getMessage)
        updatedDiag.setCode(diag.getCode)
        updatedDiag.setRelatedInformation(diag.getRelatedInformation)
        updatedDiag.setSeverity(diag.getSeverity)
        updatedDiag.setSource(diag.getSource)

        (originalPath, updatedDiag)
    }

  def printDiagnostic(path: os.Path, diag: bsp4j.Diagnostic): Unit =
    if (diag.getSeverity == bsp4j.DiagnosticSeverity.ERROR || diag.getSeverity == bsp4j.DiagnosticSeverity.WARNING) {
      val red = Console.RED
      val yellow = Console.YELLOW
      val reset = Console.RESET
      val prefix = if (diag.getSeverity == bsp4j.DiagnosticSeverity.ERROR) s"[${red}error$reset] " else s"[${yellow}warn$reset] "

      val line = (diag.getRange.getStart.getLine + 1).toString + ":"
      val col = (diag.getRange.getStart.getCharacter + 1).toString + ":"
      val msgIt = diag.getMessage.linesIterator

      val path0 =
        if (path.startsWith(Os.pwd)) "." + File.separator + path.relativeTo(Os.pwd).toString
        else path.toString
      println(s"$prefix$path0:$line$col" + (if (msgIt.hasNext) " " + msgIt.next() else ""))
      for (line <- msgIt)
        println(prefix + line)
      for (code <- Option(diag.getCode))
        code.linesIterator.map(prefix + _).foreach(println(_))
      if (diag.getRange.getStart.getLine == diag.getRange.getEnd.getLine && diag.getRange.getStart.getCharacter != null && diag.getRange.getEnd.getCharacter != null)
        println(prefix + " " * diag.getRange.getStart.getCharacter + "^" * (diag.getRange.getEnd.getCharacter - diag.getRange.getStart.getCharacter + 1))
    }


  def onBuildPublishDiagnostics(params: bsp4j.PublishDiagnosticsParams): Unit = {
    logger.debug("Received onBuildPublishDiagnostics from bloop: " + pprint.tokenize(params).map(_.render).mkString)
    for (diag <- params.getDiagnostics.asScala) {
      val path = os.Path(Paths.get(new URI(params.getTextDocument.getUri)).toAbsolutePath)
      val (updatedPath, updatedDiag) = postProcessDiagnostic(path, diag).getOrElse((path, diag))
      if (keepDiagnostics)
        diagnostics0 += updatedPath -> updatedDiag
      printDiagnostic(updatedPath, updatedDiag)
    }
  }

  def onBuildLogMessage(params: bsp4j.LogMessageParams): Unit = {
    logger.debug("Received onBuildLogMessage from bloop: " + pprint.tokenize(params).map(_.render).mkString)
    val prefix = params.getType match {
      case bsp4j.MessageType.ERROR       => "Error: "
      case bsp4j.MessageType.WARNING     => "Warning: "
      case bsp4j.MessageType.INFORMATION => ""
      case bsp4j.MessageType.LOG         => "" // discard those by default?
    }
    System.err.println(prefix + params.getMessage)
  }

  def onBuildShowMessage(params: bsp4j.ShowMessageParams): Unit =
    logger.debug("Received onBuildShowMessage from bloop: " + pprint.tokenize(params).map(_.render).mkString)

  def onBuildTargetDidChange(params: bsp4j.DidChangeBuildTarget): Unit =
    logger.debug("Received onBuildTargetDidChange from bloop: " + pprint.tokenize(params).map(_.render).mkString)

  def onBuildTaskStart(params: bsp4j.TaskStartParams): Unit = {
    logger.debug("Received onBuildTaskStart from bloop: " + pprint.tokenize(params).map(_.render).mkString)
    for (msg <- Option(params.getMessage) if !msg.contains(" no-op compilation")) {
      printedStart = true
      System.err.println(gray + msg + reset)
    }
  }

  def onBuildTaskProgress(params: bsp4j.TaskProgressParams): Unit =
    logger.debug("Received onBuildTaskProgress from bloop: " + pprint.tokenize(params).map(_.render).mkString)

  def onBuildTaskFinish(params: bsp4j.TaskFinishParams): Unit = {
    logger.debug("Received onBuildTaskFinish from bloop: " + pprint.tokenize(params).map(_.render).mkString)
    if (printedStart)
      for (msg <- Option(params.getMessage))
        System.err.println(gray + msg + reset)
  }

  override def onConnectWithServer(server: bsp4j.BuildServer): Unit = {}
}
