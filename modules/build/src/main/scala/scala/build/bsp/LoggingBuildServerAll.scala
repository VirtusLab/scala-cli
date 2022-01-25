package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

class LoggingBuildServerAll(
  val underlying: b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer
    with ScalaScriptBuildServer
) extends LoggingBuildServer with LoggingScalaBuildServer with LoggingJavaBuildServer
    with ScalaScriptBuildServer {

  def buildTargetWrappedSources(params: WrappedSourcesParams)
    : CompletableFuture[WrappedSourcesResult] =
    underlying.buildTargetWrappedSources(pprint.stderr.log(params)).logF

}
