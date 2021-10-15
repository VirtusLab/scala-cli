package scala.build.bloop

import ch.epfl.scala.bsp4j

import scala.build.bsp.ScalaDebugServer

trait BuildServer extends bsp4j.BuildServer with bsp4j.ScalaBuildServer with bsp4j.JavaBuildServer
    with ScalaDebugServer
