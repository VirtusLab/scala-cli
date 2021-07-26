package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

trait LoggingJavaBuildServer extends b.JavaBuildServer {
  protected def underlying: b.JavaBuildServer
  override def buildTargetJavacOptions(params: b.JavacOptionsParams): CompletableFuture[b.JavacOptionsResult] =
    underlying.buildTargetJavacOptions(pprint.better.log(params)).logF
}
