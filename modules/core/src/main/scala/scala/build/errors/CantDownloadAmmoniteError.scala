package scala.build.errors

import coursier.error.ResolutionError

import scala.build.Position

final class CantDownloadAmmoniteError(
  ammoniteVersion: String,
  scalaVersion: String,
  underlying: ResolutionError.CantDownloadModule,
  override val positions: Seq[Position]
) extends BuildException(
      s"""Can't download Ammonite $ammoniteVersion for Scala $scalaVersion.
         |Ammonite with this Scala version might not yet be supported.
         |Try passing a different Scala version with the -S option.""".stripMargin,
      positions,
      underlying
    )

object CantDownloadAmmoniteError {
  def apply(
    ammoniteVersion: String,
    scalaVersion: String,
    underlying: ResolutionError.CantDownloadModule,
    positions: Seq[Position]
  ): CantDownloadAmmoniteError =
    new CantDownloadAmmoniteError(ammoniteVersion, scalaVersion, underlying, positions)
}
