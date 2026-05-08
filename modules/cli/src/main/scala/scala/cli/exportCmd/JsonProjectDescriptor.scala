package scala.cli.exportCmd
import scala.build.errors.BuildException
import scala.build.info.{BuildInfo, ScopedBuildInfo}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{Logger, Sources}

final case class JsonProjectDescriptor(
  projectName: Option[String] = None,
  workspace: os.Path,
  logger: Logger
) extends ProjectDescriptor {

  def `export`(
    optionsMain: BuildOptions,
    optionsTest: BuildOptions,
    sourcesMain: Sources,
    sourcesTest: Sources
  ): Either[BuildException, JsonProject] = {
    def getScopedBuildInfo(options: BuildOptions, sources: Sources, scope: Scope) =
      val sourcePaths   = sources.paths.map(_._1.toString)
      val inMemoryPaths = sources.inMemory.flatMap(_.originalPath.toSeq.map(_._2.toString))

      ScopedBuildInfo.forScope(
        options,
        sourcePaths ++ inMemoryPaths,
        scope,
        logger,
        injectTestRunner = true
      )

    for {
      baseBuildInfo <- BuildInfo(optionsMain, workspace)
      mainBuildInfo = getScopedBuildInfo(optionsMain, sourcesMain, Scope.Main)
      testBuildInfo = getScopedBuildInfo(optionsTest, sourcesTest, Scope.Test)
    } yield JsonProject(baseBuildInfo
      .withScope(Scope.Main.name, mainBuildInfo)
      .withScope(Scope.Test.name, testBuildInfo))
  }
}
