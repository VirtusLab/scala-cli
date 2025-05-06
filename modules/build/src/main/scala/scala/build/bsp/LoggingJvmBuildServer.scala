package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

trait LoggingJvmBuildServer extends b.JvmBuildServer {
  protected def underlying: b.JvmBuildServer

  override def buildTargetJvmRunEnvironment(params: b.JvmRunEnvironmentParams)
    : CompletableFuture[b.JvmRunEnvironmentResult] =
    underlying.buildTargetJvmRunEnvironment(pprint.err.log(params)).logF

  override def buildTargetJvmTestEnvironment(params: b.JvmTestEnvironmentParams)
    : CompletableFuture[b.JvmTestEnvironmentResult] =
    underlying.buildTargetJvmTestEnvironment(pprint.err.log(params)).logF
}
