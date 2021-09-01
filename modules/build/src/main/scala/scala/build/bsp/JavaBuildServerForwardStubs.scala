package scala.build.bsp

import ch.epfl.scala.bsp4j.{JavacOptionsParams, JavacOptionsResult}
import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

trait JavaBuildServerForwardStubs extends b.JavaBuildServer {
  protected def forwardTo: b.JavaBuildServer

  override def buildTargetJavacOptions(
    params: JavacOptionsParams
  ): CompletableFuture[JavacOptionsResult] =
    forwardTo.buildTargetJavacOptions(params)
}
