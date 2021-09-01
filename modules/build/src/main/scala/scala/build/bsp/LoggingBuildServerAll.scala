package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

class LoggingBuildServerAll(
  val underlying: b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer
) extends LoggingBuildServer with LoggingScalaBuildServer with LoggingJavaBuildServer
