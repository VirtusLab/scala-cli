package scala.build.errors

import coursier.core.VariantPublication

import scala.build.Position

class UnsupportedGradleModuleVariantError(
  val variantPublication: VariantPublication,
  override val positions: Seq[Position] = Nil
) extends UnsupportedFeatureError(featureDescription =
      s"Gradle Module variant: ${variantPublication.name} (${variantPublication.url})"
    )
