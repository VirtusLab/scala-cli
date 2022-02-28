package scala.build

import ch.epfl.scala.bsp4j

import java.io.File
import java.net.URI
import java.nio.file.Paths

import scala.build.errors.Severity
import scala.build.options.Scope
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class ConsoleBloopBuildClient(
  logger: Logger,
  keepDiagnostics: Boolean = false,
  generatedSources: mutable.Map[Scope, Seq[GeneratedSource]] = mutable.Map()
) extends BloopBuildClient {
  import ConsoleBloopBuildClient._
  private var projectParams = Seq.empty[String]

  private def projectNameSuffix =
    if (projectParams.isEmpty) ""
    else " (" + projectParams.mkString(", ") + ")"

  private def projectName = "project" + projectNameSuffix

  private var printedStart = false

  private val diagnostics0 = new mutable.ListBuffer[(Either[String, os.Path], bsp4j.Diagnostic)]

  def setGeneratedSources(scope: Scope, newGeneratedSources: Seq[GeneratedSource]) =
    generatedSources(scope) = newGeneratedSources
  def setProjectParams(newParams: Seq[String]): Unit = {
    projectParams = newParams
  }
  def diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]] =
    if (keepDiagnostics) Some(diagnostics0.result())
    else None

  private def postProcessDiagnostic(
    path: os.Path,
    diag: bsp4j.Diagnostic,
    diagnosticMappings: Map[os.Path, (Either[String, os.Path], Int)]
  ): Option[(Either[String, os.Path], bsp4j.Diagnostic)] =
    diagnosticMappings.get(path).collect {
      case (originalPath, lineOffset)
          if diag.getRange.getStart.getLine + lineOffset >= 0 &&
            diag.getRange.getEnd.getLine + lineOffset >= 0 =>
        val start = new bsp4j.Position(
          diag.getRange.getStart.getLine + lineOffset,
          diag.getRange.getStart.getCharacter
        )
        val end = new bsp4j.Position(
          diag.getRange.getEnd.getLine + lineOffset,
          diag.getRange.getEnd.getCharacter
        )
        val range = new bsp4j.Range(start, end)

        val updatedDiag = new bsp4j.Diagnostic(range, diag.getMessage)
        updatedDiag.setCode(diag.getCode)
        updatedDiag.setRelatedInformation(diag.getRelatedInformation)
        updatedDiag.setSeverity(diag.getSeverity)
        updatedDiag.setSource(diag.getSource)

        (originalPath, updatedDiag)
    }

  override def onBuildPublishDiagnostics(params: bsp4j.PublishDiagnosticsParams): Unit = {
    logger.debug("Received onBuildPublishDiagnostics from bloop: " + params)
    for (diag <- params.getDiagnostics.asScala) {

      val diagnosticMappings = generatedSources.valuesIterator
        .flatMap(_.iterator)
        .map { source =>
          val lineShift = -os.read(source.generated)
            .take(source.topWrapperLen)
            .count(_ == '\n') // charset?
          (source.generated, (source.reportingPath, lineShift))
        }
        .toMap

      val path = os.Path(Paths.get(new URI(params.getTextDocument.getUri)).toAbsolutePath)
      val (updatedPath, updatedDiag) = postProcessDiagnostic(path, diag, diagnosticMappings)
        .getOrElse((Right(path), diag))
      if (keepDiagnostics)
        diagnostics0 += updatedPath -> updatedDiag
      ConsoleBloopBuildClient.printFileDiagnostic(logger, updatedPath, updatedDiag)
    }
  }

  override def onBuildLogMessage(params: bsp4j.LogMessageParams): Unit = {
    logger.debug("Received onBuildLogMessage from bloop: " + params)
    val prefix = params.getType match {
      case bsp4j.MessageType.ERROR       => "Error: "
      case bsp4j.MessageType.WARNING     => "Warning: "
      case bsp4j.MessageType.INFORMATION => ""
      case bsp4j.MessageType.LOG         => "" // discard those by default?
    }
    logger.message(prefix + params.getMessage)
  }

  override def onBuildShowMessage(params: bsp4j.ShowMessageParams): Unit =
    logger.debug("Received onBuildShowMessage from bloop: " + params)

  override def onBuildTargetDidChange(params: bsp4j.DidChangeBuildTarget): Unit =
    logger.debug("Received onBuildTargetDidChange from bloop: " + params)

  override def onBuildTaskStart(params: bsp4j.TaskStartParams): Unit = {
    logger.debug("Received onBuildTaskStart from bloop: " + params)
    for (msg <- Option(params.getMessage) if !msg.contains(" no-op compilation")) {
      printedStart = true
      val msg0 =
        if (params.getDataKind == "compile-task") s"Compiling $projectName"
        else msg
      logger.message(gray + msg0 + reset)
    }
  }

  override def onBuildTaskProgress(params: bsp4j.TaskProgressParams): Unit =
    logger.debug("Received onBuildTaskProgress from bloop: " + params)

  override def onBuildTaskFinish(params: bsp4j.TaskFinishParams): Unit = {
    logger.debug("Received onBuildTaskFinish from bloop: " + params)
    if (printedStart)
      for (msg <- Option(params.getMessage)) {
        val msg0 =
          if (params.getDataKind == "compile-report")
            params.getStatus match {
              case bsp4j.StatusCode.OK        => s"Compiled $projectName"
              case bsp4j.StatusCode.ERROR     => s"Error compiling $projectName"
              case bsp4j.StatusCode.CANCELLED => s"Compilation cancelled$projectNameSuffix"
              case _                          => s"Compiled $projectName" // ???
            }
          else msg
        logger.message(gray + msg0 + reset)
      }
  }

  override def onConnectWithServer(server: bsp4j.BuildServer): Unit = {}

  def clear(): Unit = {
    generatedSources.clear()
    diagnostics0.clear()
    printedStart = false
  }
}

object ConsoleBloopBuildClient {
  private val gray   = "\u001b[90m"
  private val reset  = Console.RESET
  private val red    = Console.RED
  private val yellow = Console.YELLOW

  def diagnosticPrefix(isError: Boolean): String =
    if (isError) s"[${red}error$reset] "
    else s"[${yellow}warn$reset] "

  def printFileDiagnostic(
    logger: Logger,
    path: Either[String, os.Path],
    diag: bsp4j.Diagnostic
  ): Unit = {
    val isWarningOrError = diag.getSeverity == bsp4j.DiagnosticSeverity.ERROR ||
      diag.getSeverity == bsp4j.DiagnosticSeverity.WARNING
    if (isWarningOrError) {
      val prefix = diagnosticPrefix(diag.getSeverity == bsp4j.DiagnosticSeverity.ERROR)

      val line  = (diag.getRange.getStart.getLine + 1).toString + ":"
      val col   = (diag.getRange.getStart.getCharacter + 1).toString + ":"
      val msgIt = diag.getMessage.linesIterator

      val path0 = path match {
        case Left(source) => source
        case Right(p) if p.startsWith(Os.pwd) =>
          "." + File.separator + p.relativeTo(Os.pwd).toString
        case Right(p) => p.toString
      }
      logger.message(s"$prefix$path0:$line$col" + (if (msgIt.hasNext) " " + msgIt.next() else ""))
      for (line <- msgIt)
        logger.message(prefix + line)
      val codeOpt = Option(diag.getCode).orElse {
        val lineOpt =
          if (diag.getRange.getStart.getLine == diag.getRange.getEnd.getLine)
            Option(diag.getRange.getStart.getLine)
          else None
        for {
          line <- lineOpt
          p    <- path.toOption
          lines = os.read.lines(p)
          line <- if (line < lines.length) Some(lines(line)) else None
        } yield line
      }
      for (code <- codeOpt)
        code.linesIterator.map(prefix + _).foreach(logger.message(_))
      val canPrintUnderline = diag.getRange.getStart.getLine == diag.getRange.getEnd.getLine &&
        diag.getRange.getStart.getCharacter != null &&
        diag.getRange.getEnd.getCharacter != null &&
        codeOpt.nonEmpty
      if (canPrintUnderline) {
        val len =
          math.max(1, diag.getRange.getEnd.getCharacter - diag.getRange.getStart.getCharacter)
        logger.message(
          prefix + " " * diag.getRange.getStart.getCharacter + "^" * len
        )
      }
    }
  }

  def printOtherDiagnostic(
    logger: Logger,
    message: String,
    severity: Severity,
    positions: Seq[Position]
  ): Unit = {
    val isWarningOrError = true
    if (isWarningOrError) {
      val msgIt  = message.linesIterator
      val prefix = diagnosticPrefix(severity == Severity.Error)
      logger.message(prefix + (if (msgIt.hasNext) " " + msgIt.next() else ""))
      msgIt.foreach(line => logger.message(prefix + line))

      positions.foreach {
        case Position.Bloop(bloopJavaPath) =>
          val bloopOutputPrefix = s"[current bloop jvm] "
          logger.message(prefix + bloopOutputPrefix + bloopJavaPath)
          logger.message(prefix + " " * bloopOutputPrefix.length + "^" * bloopJavaPath.length())
        case pos => logger.message(prefix + pos.render())
      }
    }
  }

}
