package scala.build.errors

import scala.build.Position

abstract class UnsupportedFeatureError(
  val featureDescription: String,
  override val positions: Seq[Position] = Nil
) extends BuildException(s"Unsupported feature: $featureDescription")
