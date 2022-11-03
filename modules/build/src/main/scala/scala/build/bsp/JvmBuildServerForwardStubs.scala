package scala.build.bsp

import ch.epfl.scala.bsp4j as b

import java.util.concurrent.CompletableFuture

trait JvmBuildServerForwardStubs extends b.JvmBuildServer {
  protected def forwardTo: b.JvmBuildServer

  override def jvmRunEnvironment(params: b.JvmRunEnvironmentParams)
    : CompletableFuture[b.JvmRunEnvironmentResult] =
    forwardTo.jvmRunEnvironment(params)

  override def jvmTestEnvironment(params: b.JvmTestEnvironmentParams)
    : CompletableFuture[b.JvmTestEnvironmentResult] =
    forwardTo.jvmTestEnvironment(params)
}
