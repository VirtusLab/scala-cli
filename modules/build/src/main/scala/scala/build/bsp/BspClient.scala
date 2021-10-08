package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.ExecutorService

import scala.build.postprocessing.LineConversion
import scala.build.{BloopBuildClient, GeneratedSource, Logger}
import scala.jdk.CollectionConverters._

class BspClient(
  readFilesEs: ExecutorService,
  logger: Logger,
  var forwardToOpt: Option[b.BuildClient] = None
) extends b.BuildClient with BuildClientForwardStubs with BloopBuildClient
    with HasGeneratedSources {

  private def updatedPublishDiagnosticsParams(
    params: b.PublishDiagnosticsParams,
    genSource: GeneratedSource
  ): Either[() => b.PublishDiagnosticsParams, b.PublishDiagnosticsParams] = {
    val updatedUri = genSource.reportingPath.fold(
      _ => params.getTextDocument.getUri,
      _.toNIO.toUri.toASCIIString
    )
    val updatedDiagnostics =
      if (genSource.topWrapperLen == 0)
        Right(params.getDiagnostics)
      else
        Left { () =>
          val updateLine = LineConversion.scalaLineToScLine(
            os.read(os.Path(Paths.get(new URI(updatedUri)))),
            os.read(os.Path(Paths.get(new URI(params.getTextDocument.getUri)))),
            genSource.topWrapperLen
          )
          params.getDiagnostics.asScala.toSeq
            .map { diag =>
              val updatedDiagOpt = for {
                startLine <- updateLine(diag.getRange.getStart.getLine)
                endLine   <- updateLine(diag.getRange.getEnd.getLine)
              } yield {
                val diag0 = diag.duplicate()
                diag0.getRange.getStart.setLine(startLine)
                diag0.getRange.getEnd.setLine(endLine)
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
    val updatedParamsOpt =
      if (validTarget(params.getBuildTarget))
        generatedSources.uriMap.get(params.getTextDocument.getUri).map { genSource =>
          updatedPublishDiagnosticsParams(params, genSource)
        }
      else None
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

  def setProjectParams(newParams: Seq[String]): Unit = {}
  def diagnostics: Option[Seq[(Either[String, os.Path], b.Diagnostic)]] = None
  def clear(): Unit = {}
}
