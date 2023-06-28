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
import scala.build.internal.Runner.frameworkName
import scala.build.options.{BuildOptions, Scope}
import scala.build.testrunner.AsmTestRunner
import scala.build.{Logger, Positioned, Sources}
import scala.cli.util.SeqHelpers.*

final case class JsonProjectDescriptor(
  projectName: Option[String] = None,
  logger: Logger
) extends ProjectDescriptor {
  private val charSet = StandardCharsets.UTF_8

  def `export`(
    optionsMain: BuildOptions,
    optionsTest: BuildOptions,
    sourcesMain: Sources,
    sourcesTest: Sources
  ): JsonProject = {
    def getScopedBuildInfo(options: BuildOptions, sources: Sources) =
      val sourcePaths   = sources.paths.map(_._1.toString)
      val inMemoryPaths = sources.inMemory.flatMap(_.originalPath.toSeq.map(_._2.toString))

      ScopedBuildInfo(options, sourcePaths ++ inMemoryPaths)

    val baseBuildInfo = BuildInfo(optionsMain)

    val mainBuildInfo = getScopedBuildInfo(optionsMain, sourcesMain)
    val testBuildInfo = getScopedBuildInfo(optionsTest, sourcesTest)

    JsonProject(baseBuildInfo
      .withScope(Scope.Main.name, mainBuildInfo)
      .withScope(Scope.Test.name, testBuildInfo))
  }
}
