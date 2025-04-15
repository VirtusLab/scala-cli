package scala.build.preprocessing.directives

import scala.build.directives.{
  HasBuildOptions,
  HasBuildOptionsWithRequirements,
  HasBuildRequirements
}
import scala.build.options.{BuildOptions, BuildRequirements, WithBuildRequirements}
import scala.build.preprocessing.directives

object Directives {
  val usingDirectiveHandlers: Seq[DirectiveHandler[BuildOptions]] =
    Seq[DirectiveHandler[? <: HasBuildOptions]](
      directives.Benchmarking.handler,
      directives.BuildInfo.handler,
      directives.ComputeVersion.handler,
      directives.Exclude.handler,
      directives.JavaHome.handler,
      directives.Jvm.handler,
      directives.MainClass.handler,
      directives.ObjectWrapper.handler,
      directives.Packaging.handler,
      directives.Platform.handler,
      directives.Plugin.handler,
      directives.Publish.handler,
      directives.PublishContextual.Local.handler,
      directives.PublishContextual.CI.handler,
      directives.Python.handler,
      directives.Repository.handler,
      directives.ScalaJs.handler,
      directives.ScalaNative.handler,
      directives.ScalaVersion.handler,
      directives.Sources.handler,
      directives.Tests.handler
    ).map(_.mapE(_.buildOptions))

  val usingDirectiveWithReqsHandlers
    : Seq[DirectiveHandler[List[WithBuildRequirements[BuildOptions]]]] =
    Seq[DirectiveHandler[? <: HasBuildOptionsWithRequirements]](
      directives.CustomJar.handler,
      directives.Dependency.handler,
      directives.JavaOptions.handler,
      directives.JavacOptions.handler,
      directives.JavaProps.handler,
      directives.Resources.handler,
      directives.ScalacOptions.handler,
      directives.Toolkit.handler
    ).map(_.mapE(_.buildOptionsWithRequirements))

  val requireDirectiveHandlers: Seq[DirectiveHandler[BuildRequirements]] =
    Seq[DirectiveHandler[? <: HasBuildRequirements]](
      directives.RequirePlatform.handler,
      directives.RequireScalaVersion.handler,
      directives.RequireScalaVersionBounds.handler,
      directives.RequireScope.handler
    ).map(_.mapE(_.buildRequirements))

  def allDirectiveHandlers: Seq[DirectiveHandler[BuildRequirements | BuildOptions]] =
    usingDirectiveHandlers ++ requireDirectiveHandlers

  def getDirectiveHandler(key: String): Option[DirectiveHandler[BuildRequirements | BuildOptions]] =
    allDirectiveHandlers.find(_.keys.exists(_.nameAliases.contains(key)))

}
