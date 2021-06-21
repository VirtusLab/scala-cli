package scala.build.internal

import org.scalajs.linker.interface.StandardConfig

// Simple wrapper around StandardConfig, that we can't use in Java because of 'interface' in its namespace
final class ScalaJsConfig(val config: StandardConfig)
