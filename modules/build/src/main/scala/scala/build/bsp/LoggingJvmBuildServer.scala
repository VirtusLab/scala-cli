package scala.build.bsp

import ch.epfl.scala.bsp4j as b

import java.util.concurrent.CompletableFuture

trait LoggingJvmBuildServer extends b.JvmBuildServer {
  protected def underlying: b.JvmBuildServer

  override def jvmRunEnvironment(params: b.JvmRunEnvironmentParams)
    : CompletableFuture[b.JvmRunEnvironmentResult] =
    underlying.jvmRunEnvironment(pprint.err.log(params)).logF

  override def jvmTestEnvironment(params: b.JvmTestEnvironmentParams)
    : CompletableFuture[b.JvmTestEnvironmentResult] =
    underlying.jvmTestEnvironment(pprint.err.log(params)).logF
}
