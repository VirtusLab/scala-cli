package scala.build.info

import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.{Dependency, LocalRepositories, Repositories}
import dependency.AnyDependency

import scala.build.options.{BuildOptions, ConfigMonoid}

final case class ScopedBuildInfo(
  sources: Seq[String] = Nil,
  scalacOptions: Seq[String] = Nil,
  scalaCompilerPlugins: Seq[ExportDependencyFormat] = Nil,
  dependencies: Seq[ExportDependencyFormat] = Nil,
  compileOnlyDependencies: Seq[ExportDependencyFormat] = Nil,
  resolvers: Seq[String] = Nil,
  resourceDirs: Seq[String] = Nil,
  customJarsDecls: Seq[String] = Nil
) {
  def +(other: ScopedBuildInfo): ScopedBuildInfo =
    ScopedBuildInfo.monoid.orElse(this, other)

  def generateContentLines(): Seq[String] =
    Seq(
      Seq("/** sources found for the scope */", "val sources = ")        -> sources,
      Seq("/** scalac options for the scope */", "val scalacOptions = ") -> scalacOptions,
      Seq(
        "/** compiler plugins used in this scope */",
        "val scalaCompilerPlugins = "
      ) -> scalaCompilerPlugins.map(_.toString()),
      Seq("/** dependencies used in this scope */", "val dependencies = ") -> dependencies.map(
        _.toString()
      ),
      Seq("/** dependency resolvers used in this scope */", "val resolvers = ")    -> resolvers,
      Seq("/** resource directories used in this scope */", "val resourceDirs = ") -> resourceDirs,
      Seq("/** custom jars added to this scope */", "val customJarsDecls = ") -> customJarsDecls
    ).flatMap { case (Seq(scaladoc, prefix), values) =>
      val sb = new StringBuilder
      sb.append(prefix)
      if (values.isEmpty) sb.append("Nil")
      else sb.append {
        values.map(str => s"\"${BuildInfo.escapeBackslashes(str)}\"")
          .mkString("Seq(", ", ", ")")
      }
      Seq(scaladoc, sb.toString())
    }
}

object ScopedBuildInfo {
  def empty: ScopedBuildInfo = ScopedBuildInfo()

  def apply(options: BuildOptions, sourcePaths: Seq[String]): ScopedBuildInfo =
    Seq(
      ScopedBuildInfo(sources = sourcePaths),
      scalaCompilerPlugins(options),
      scalacOptionsSettings(options),
      dependencySettings(options),
      repositorySettings(options),
      customResourcesSettings(options),
      customJarsSettings(options)
    )
      .reduceLeft(_ + _)

  private def scalacOptionsSettings(options: BuildOptions): ScopedBuildInfo =
    ScopedBuildInfo(scalacOptions = options.scalaOptions.scalacOptions.toSeq.map(_.value.value))

  private def scalaCompilerPlugins(options: BuildOptions): ScopedBuildInfo =
    val compilerPlugins = options.scalaOptions.compilerPlugins.map(_.value)
      .map(ExportDependencyFormat(_, options.scalaParams.getOrElse(None)))

    ScopedBuildInfo(scalaCompilerPlugins = compilerPlugins)

  private def dependencySettings(options: BuildOptions): ScopedBuildInfo = {
    val directDeps = options.classPathOptions.extraDependencies.toSeq.map(_.value)
      .map(ExportDependencyFormat(_, options.scalaParams.getOrElse(None)))
    val compileDeps = options.classPathOptions.extraCompileOnlyDependencies.toSeq.map(_.value)
      .map(ExportDependencyFormat(_, options.scalaParams.getOrElse(None)))

    ScopedBuildInfo(
      dependencies = directDeps,
      compileOnlyDependencies = compileDeps
    )
  }

  private def repositorySettings(options: BuildOptions): ScopedBuildInfo = {
    val resolvers = options.finalRepositories
      .getOrElse(Nil)
      .appended(Repositories.central)
      .appended(LocalRepositories.ivy2Local)
      .collect {
        case repo: MavenRepository => repo.root
        case repo: IvyRepository   => s"ivy:${repo.pattern.string}"
      }
      .distinct

    ScopedBuildInfo(resolvers = resolvers)
  }

  private def customResourcesSettings(options: BuildOptions): ScopedBuildInfo =
    ScopedBuildInfo(resourceDirs = options.classPathOptions.resourcesDir.map(_.toNIO.toString))

  private def customJarsSettings(options: BuildOptions): ScopedBuildInfo = {

    val customCompileOnlyJarsDecls =
      options.classPathOptions.extraCompileOnlyJars.map(_.toNIO.toString)

    val customJarsDecls = options.classPathOptions.extraClassPath.map(_.toNIO.toString)

    ScopedBuildInfo(
      customJarsDecls = customCompileOnlyJarsDecls ++ customJarsDecls
    )
  }

  implicit val monoid: ConfigMonoid[ScopedBuildInfo] = ConfigMonoid.derive
}

final case class ExportDependencyFormat(groupId: String, artifactId: ArtifactId, version: String) {
  override def toString(): String = {
    val sb = new StringBuilder
    sb.append(groupId)
    sb.append(':')
    sb.append(artifactId.fullName)
    sb.append(':')
    sb.append(version)
    sb.toString()
  }
}

final case class ArtifactId(name: String, fullName: String)

object ExportDependencyFormat {
  def apply(dep: Dependency): ExportDependencyFormat = {
    val scalaVersionStartIndex = dep.module.name.value.lastIndexOf('_')
    val shortDepName           = if (scalaVersionStartIndex == -1)
      dep.module.name.value
    else
      dep.module.name.value.take(scalaVersionStartIndex)
    new ExportDependencyFormat(
      dep.module.organization.value,
      ArtifactId(shortDepName, dep.module.name.value),
      dep.version
    )
  }

  def apply(
    dep: AnyDependency,
    scalaParamsOpt: Option[dependency.ScalaParameters]
  ): ExportDependencyFormat = {
    import scala.build.internal.Util.*
    dep.toCs(scalaParamsOpt)
      .map(ExportDependencyFormat.apply)
      .getOrElse(
        ExportDependencyFormat(
          dep.module.organization,
          ArtifactId(dep.module.name, dep.module.name),
          dep.version
        )
      )
  }
}
