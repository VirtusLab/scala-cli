package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

trait ScalaBuildServerForwardStubs extends b.ScalaBuildServer {
  protected def forwardTo: b.ScalaBuildServer
  override def buildTargetScalaMainClasses(
    params: b.ScalaMainClassesParams
  ): CompletableFuture[b.ScalaMainClassesResult] =
    forwardTo.buildTargetScalaMainClasses(params)
  override def buildTargetScalaTestClasses(
    params: b.ScalaTestClassesParams
  ): CompletableFuture[b.ScalaTestClassesResult] =
    forwardTo.buildTargetScalaTestClasses(params)
  override def buildTargetScalacOptions(
    params: b.ScalacOptionsParams
  ): CompletableFuture[b.ScalacOptionsResult] =
    forwardTo.buildTargetScalacOptions(params)
}
