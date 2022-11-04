package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

class LoggingBuildServerAll(
  val underlying: b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer with b.JvmBuildServer
    with ScalaScriptBuildServer
) extends LoggingBuildServer with LoggingScalaBuildServer with LoggingJavaBuildServer
    with LoggingJvmBuildServer
    with ScalaScriptBuildServer {

  def buildTargetWrappedSources(params: WrappedSourcesParams)
    : CompletableFuture[WrappedSourcesResult] =
    underlying.buildTargetWrappedSources(pprint.err.log(params)).logF

  override def buildTargetOutputPaths(params: b.OutputPathsParams)
    : CompletableFuture[b.OutputPathsResult] =
    underlying.buildTargetOutputPaths(pprint.err.log(params)).logF

}
