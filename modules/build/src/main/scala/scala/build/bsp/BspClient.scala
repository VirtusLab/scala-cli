package scala.build.bsp

import ch.epfl.scala.bsp4j.{ScalaAction, ScalaDiagnostic, ScalaTextEdit, ScalaWorkspaceEdit}
import ch.epfl.scala.bsp4j as b
import com.google.gson.Gson

import java.lang.Boolean as JBoolean
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.{ConcurrentHashMap, ExecutorService}

import scala.build.Position.File
import scala.build.bsp.protocol.TextEdit
import scala.build.errors.{BuildException, CompositeBuildException, Diagnostic, Severity}
import scala.build.internal.util.WarningMessages
import scala.build.postprocessing.LineConversion.scalaLineToScLine
import scala.build.{BloopBuildClient, GeneratedSource, Logger}
import scala.jdk.CollectionConverters.*

class BspClient(
  readFilesEs: ExecutorService,
  @volatile var logger: Logger,
  var forwardToOpt: Option[b.BuildClient] = None
) extends b.BuildClient with BuildClientForwardStubs with BloopBuildClient
    with HasGeneratedSourcesImpl {

  private def updatedPublishDiagnosticsParams(
    params: b.PublishDiagnosticsParams,
    genSource: GeneratedSource
  ): Either[() => b.PublishDiagnosticsParams, b.PublishDiagnosticsParams] = {
    val updatedUri = genSource.reportingPath.fold(
      _ => params.getTextDocument.getUri,
      _.toNIO.toUri.toASCIIString
    )
    val updatedDiagnostics =
      if (genSource.wrapperParamsOpt.isEmpty)
        Right(params.getDiagnostics)
      else
        Left { () =>
          val updateLine = scalaLine => scalaLineToScLine(scalaLine, genSource.wrapperParamsOpt)
          params.getDiagnostics.asScala.toSeq
            .map { diag =>
              val updatedDiagOpt = for {
                startLine <- updateLine(diag.getRange.getStart.getLine)
                endLine   <- updateLine(diag.getRange.getEnd.getLine)
              } yield {
                val diag0 = diag.duplicate()
                diag0.getRange.getStart.setLine(startLine)
                diag0.getRange.getEnd.setLine(endLine)

                if (
                  diag0.getMessage.contains(
                    "cannot be a main method since it cannot be accessed statically"
                  )
                )
                  diag0.setMessage(
                    WarningMessages.mainAnnotationNotSupported( /* annotationIgnored */ false)
                  )

                diag0
              }
              updatedDiagOpt.getOrElse(diag)
            }
            .asJava
        }
    def updatedParamsFor(
      updatedDiagnostics: java.util.List[b.Diagnostic]
    ): b.PublishDiagnosticsParams = {
      val updatedTextDoc = new b.TextDocumentIdentifier(updatedUri)
      val updatedParams = new b.PublishDiagnosticsParams(
        updatedTextDoc,
        params.getBuildTarget,
        updatedDiagnostics,
        params.getReset
      )
      updatedParams.setOriginId(params.getOriginId)
      updatedParams
    }
    updatedDiagnostics
      .left.map(f => () => updatedParamsFor(f()))
      .map(updatedParamsFor(_))
  }

  override def onBuildPublishDiagnostics(params: b.PublishDiagnosticsParams): Unit = {
    val path = os.Path(Paths.get(new URI(params.getTextDocument.getUri)))
    buildExceptionDiagnosticsDocs.remove((path, params.getBuildTarget))

    actualBuildPublishDiagnostics(params)
  }

  private def actualBuildPublishDiagnostics(params: b.PublishDiagnosticsParams): Unit = {
    val updatedParamsOpt = targetScopeOpt(params.getBuildTarget).flatMap { scope =>
      generatedSources.getOrElse(scope, HasGeneratedSources.GeneratedSources(Nil))
        .uriMap
        .get(params.getTextDocument.getUri)
        .map { genSource =>
          updatedPublishDiagnosticsParams(params, genSource)
        }
    }

    def call(updatedParams0: b.PublishDiagnosticsParams): Unit =
      super.onBuildPublishDiagnostics(updatedParams0)
    updatedParamsOpt match {
      case None =>
        call(params)
      case Some(Right(updatedParams0)) =>
        call(updatedParams0)
      case Some(Left(updateParamsFunc)) =>
        val runnable =
          new Runnable {
            def run(): Unit =
              try call(updateParamsFunc())
              catch {
                case t: Throwable =>
                  logger.debug(s"Caught $t while publishing updated diagnostics")
              }
          }
        readFilesEs.submit(runnable)
    }
  }

  def setProjectParams(newParams: Seq[String]): Unit                    = {}
  def diagnostics: Option[Seq[(Either[String, os.Path], b.Diagnostic)]] = None
  def clear(): Unit                                                     = {}

  private val buildExceptionDiagnosticsDocs =
    new ConcurrentHashMap[(os.Path, b.BuildTargetIdentifier), JBoolean]

  def resetDiagnostics(path: os.Path, targetId: b.BuildTargetIdentifier): Unit = {
    val id = new b.TextDocumentIdentifier(path.toNIO.toUri.toASCIIString)
    val params = new b.PublishDiagnosticsParams(
      id,
      targetId,
      List.empty[b.Diagnostic].asJava,
      true
    )
    actualBuildPublishDiagnostics(params)
  }

  def resetBuildExceptionDiagnostics(targetId: b.BuildTargetIdentifier): Unit =
    for {
      (key @ (path, elemTargetId), _) <- buildExceptionDiagnosticsDocs.asScala.toVector
      if elemTargetId == targetId
    } {
      val removedValue = buildExceptionDiagnosticsDocs.remove(key)
      if (removedValue != null) {
        val id = new b.TextDocumentIdentifier(path.toNIO.toUri.toASCIIString)
        val params = new b.PublishDiagnosticsParams(
          id,
          targetId,
          List.empty[b.Diagnostic].asJava,
          true
        )
        actualBuildPublishDiagnostics(params)
      }
    }

  def reportBuildException(
    targetIdOpt: Option[b.BuildTargetIdentifier],
    ex: BuildException,
    reset: Boolean = true
  ): Unit =
    targetIdOpt match {
      case None =>
        logger.debug(s"Not reporting $ex to users (no build target id)")
      case Some(targetId) =>
        val touchedFiles = (ex match {
          case c: CompositeBuildException =>
            reportDiagnosticsForFiles(targetId, c.exceptions, reset = reset)
          case _ => reportDiagnosticsForFiles(targetId, Seq(ex), reset = reset)
        }).toSet

        // Small chance of us wiping some Bloop diagnostics, if these happen
        // between the call to remove and the call to actualBuildPublishDiagnostics.
        for {
          (key @ (path, elemTargetId), _) <- buildExceptionDiagnosticsDocs.asScala.toVector
          if elemTargetId == targetId && !touchedFiles(path)
        } {
          val removedValue = buildExceptionDiagnosticsDocs.remove(key)
          if (removedValue != null) {
            val id = new b.TextDocumentIdentifier(path.toNIO.toUri.toASCIIString)
            val params = new b.PublishDiagnosticsParams(
              id,
              targetId,
              List.empty[b.Diagnostic].asJava,
              true
            )
            actualBuildPublishDiagnostics(params)
          }
        }
    }

  def reportDiagnosticsForFiles(
    targetId: b.BuildTargetIdentifier,
    diags: Seq[Diagnostic],
    reset: Boolean = true
  ): Seq[os.Path] =
    if reset then // send diagnostic with reset only once for every file path
      diags.flatMap { diag =>
        diag.positions.map { position =>
          Diagnostic(diag.message, diag.severity, Seq(position), diag.textEdit)
        }
      }
        .groupBy(_.positions.headOption match
          case Some(File(Right(path), _, _)) => Some(path)
          case _                             => None
        )
        .filter(_._1.isDefined)
        .values
        .toSeq
        .flatMap {
          case head :: tail =>
            reportDiagnosticForFiles(targetId, reset = reset)(head)
              ++ tail.flatMap(reportDiagnosticForFiles(targetId))
          case _ => Nil
        }
    else
      diags.flatMap(reportDiagnosticForFiles(targetId))

  private def reportDiagnosticForFiles(
    targetId: b.BuildTargetIdentifier,
    reset: Boolean = false
  )(diag: Diagnostic): Seq[os.Path] =
    diag.positions.flatMap {
      case File(Right(path), (startLine, startC), (endL, endC)) =>
        val id       = new b.TextDocumentIdentifier(path.toNIO.toUri.toASCIIString)
        val startPos = new b.Position(startLine, startC)
        val endPos   = new b.Position(endL, endC)
        val range    = new b.Range(startPos, endPos)
        val bDiag =
          new b.Diagnostic(range, diag.message)

        diag.textEdit.foreach { textEdit =>
          val bScalaTextEdit      = new ScalaTextEdit(range, textEdit.newText)
          val bScalaWorkspaceEdit = new ScalaWorkspaceEdit(List(bScalaTextEdit).asJava)
          val bAction             = new ScalaAction(textEdit.title)
          bAction.setEdit(bScalaWorkspaceEdit)
          val bScalaDiagnostic = new ScalaDiagnostic
          bScalaDiagnostic.setActions(List(bAction).asJava)
          bDiag.setDataKind("scala")
          bDiag.setData(new Gson().toJsonTree(bScalaDiagnostic))
        }

        bDiag.setSeverity(diag.severity.toBsp4j)
        bDiag.setSource("scala-cli")
        val params = new b.PublishDiagnosticsParams(
          id,
          targetId,
          List(bDiag).asJava,
          reset
        )
        buildExceptionDiagnosticsDocs.put((path, targetId), JBoolean.TRUE)
        actualBuildPublishDiagnostics(params)
        Seq(path)
      case _ => Nil
    }
}
