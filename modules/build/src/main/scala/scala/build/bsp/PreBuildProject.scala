package scala.build.bsp

import dependency.ScalaParameters

import scala.build.errors.Diagnostic
import scala.build.options.BuildOptions
import scala.build.{Artifacts, GeneratedSource, Project, Sources}

final case class PreBuildData(
  sources: Sources,
  buildOptions: BuildOptions,
  classesDir: os.Path,
  scalaParams: ScalaParameters,
  artifacts: Artifacts,
  project: Project,
  generatedSources: Seq[GeneratedSource],
  buildChanged: Boolean
)

final case class PreBuildProject(
  mainScope: PreBuildData,
  testScope: PreBuildData,
  diagnostics: Seq[Diagnostic]
)
