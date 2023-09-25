package scala.build.bsp

import ch.epfl.scala.bsp4j as b

import java.util.concurrent.CompletableFuture

trait JvmBuildServerForwardStubs extends b.JvmBuildServer {
  protected def forwardTo: b.JvmBuildServer

  override def buildTargetJvmRunEnvironment(params: b.JvmRunEnvironmentParams)
    : CompletableFuture[b.JvmRunEnvironmentResult] =
    forwardTo.buildTargetJvmRunEnvironment(params)

  override def buildTargetJvmTestEnvironment(params: b.JvmTestEnvironmentParams)
    : CompletableFuture[b.JvmTestEnvironmentResult] =
    forwardTo.buildTargetJvmTestEnvironment(params)
}
