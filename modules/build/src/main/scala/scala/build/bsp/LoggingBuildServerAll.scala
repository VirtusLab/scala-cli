package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

class LoggingBuildServerAll(val underlying: b.BuildServer with b.ScalaBuildServer)
  extends LoggingBuildServer with LoggingScalaBuildServer
