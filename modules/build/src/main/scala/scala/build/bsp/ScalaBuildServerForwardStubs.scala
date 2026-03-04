package scala.build.bsp

import ch.epfl.scala.bsp4j as b

import java.util.concurrent.CompletableFuture

trait ScalaBuildServerForwardStubs extends b.ScalaBuildServer {
  protected def forwardTo: b.ScalaBuildServer
  @deprecated
  override def buildTargetScalaMainClasses(
    params: b.ScalaMainClassesParams
  ): CompletableFuture[b.ScalaMainClassesResult] =
    forwardTo.buildTargetScalaMainClasses(params)

  @deprecated
  override def buildTargetScalaTestClasses(
    params: b.ScalaTestClassesParams
  ): CompletableFuture[b.ScalaTestClassesResult] =
    forwardTo.buildTargetScalaTestClasses(params)
  override def buildTargetScalacOptions(
    params: b.ScalacOptionsParams
  ): CompletableFuture[b.ScalacOptionsResult] =
    forwardTo.buildTargetScalacOptions(params)
}
