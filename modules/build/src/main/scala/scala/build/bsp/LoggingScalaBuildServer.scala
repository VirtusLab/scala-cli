package scala.build.bsp

import ch.epfl.scala.bsp4j as b

import java.util.concurrent.CompletableFuture

trait LoggingScalaBuildServer extends b.ScalaBuildServer {
  protected def underlying: b.ScalaBuildServer
  @deprecated
  override def buildTargetScalaMainClasses(
    params: b.ScalaMainClassesParams
  ): CompletableFuture[b.ScalaMainClassesResult] =
    underlying.buildTargetScalaMainClasses(pprint.err.log(params)).logF
  @deprecated
  override def buildTargetScalaTestClasses(
    params: b.ScalaTestClassesParams
  ): CompletableFuture[b.ScalaTestClassesResult] =
    underlying.buildTargetScalaTestClasses(pprint.err.log(params)).logF
  override def buildTargetScalacOptions(
    params: b.ScalacOptionsParams
  ): CompletableFuture[b.ScalacOptionsResult] =
    underlying.buildTargetScalacOptions(pprint.err.log(params)).logF
}
