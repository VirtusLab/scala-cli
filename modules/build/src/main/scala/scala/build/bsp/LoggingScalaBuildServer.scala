package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

trait LoggingScalaBuildServer extends b.ScalaBuildServer {
  protected def underlying: b.ScalaBuildServer
  override def buildTargetScalaMainClasses(
    params: b.ScalaMainClassesParams
  ): CompletableFuture[b.ScalaMainClassesResult] =
    underlying.buildTargetScalaMainClasses(pprint.better.log(params)).logF
  override def buildTargetScalaTestClasses(
    params: b.ScalaTestClassesParams
  ): CompletableFuture[b.ScalaTestClassesResult] =
    underlying.buildTargetScalaTestClasses(pprint.better.log(params)).logF
  override def buildTargetScalacOptions(
    params: b.ScalacOptionsParams
  ): CompletableFuture[b.ScalacOptionsResult] =
    underlying.buildTargetScalacOptions(pprint.better.log(params)).logF
}
