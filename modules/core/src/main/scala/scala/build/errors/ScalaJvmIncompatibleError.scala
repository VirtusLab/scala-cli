package scala.build.errors

import scala.build.Position

final class ScalaJvmIncompatibleError(
  scalaVersion: String,
  jvmVersion: Int,
  minJdk: Int,
  jvmOrigin: String,
  override val positions: Seq[Position] = Nil
) extends BuildException(
      s"""Scala $scalaVersion requires at least Java $minJdk, but $jvmOrigin is Java $jvmVersion.
         |Pass `--jvm $minJdk` or higher, or use `//> using jvm $minJdk`.""".stripMargin
    )
