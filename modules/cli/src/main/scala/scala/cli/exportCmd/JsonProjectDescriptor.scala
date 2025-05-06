package scala.cli.exportCmd
import com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig
import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.parse.RepositoryParser
import coursier.{LocalRepositories, Repositories}
import dependency.NoAttributes

import java.nio.charset.StandardCharsets
import java.nio.file.Path

import scala.build.errors.BuildException
import scala.build.info.{BuildInfo, ScopedBuildInfo}
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, Scope}
import scala.build.testrunner.AsmTestRunner
import scala.build.{Logger, Positioned, Sources}
import scala.cli.commands.util.CommandHelpers
import scala.cli.util.SeqHelpers._

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
    def getScopedBuildInfo(options: BuildOptions, sources: Sources) =
      val sourcePaths   = sources.paths.map(_._1.toString)
      val inMemoryPaths = sources.inMemory.flatMap(_.originalPath.toSeq.map(_._2.toString))

      ScopedBuildInfo(options, sourcePaths ++ inMemoryPaths)

    for {
      baseBuildInfo <- BuildInfo(optionsMain, workspace)
      mainBuildInfo = getScopedBuildInfo(optionsMain, sourcesMain)
      testBuildInfo = getScopedBuildInfo(optionsTest, sourcesTest)
    } yield JsonProject(baseBuildInfo
      .withScope(Scope.Main.name, mainBuildInfo)
      .withScope(Scope.Test.name, testBuildInfo))
  }
}
