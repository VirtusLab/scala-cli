package scala.build.info

import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.{Dependency, LocalRepositories, Repositories}
import dependency.AnyDependency

import scala.build.Logger
import scala.build.options.{BuildOptions, ConfigMonoid, Platform, Scope}

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
      "/** sources found for the scope */"  -> "val sources = "       -> sources,
      "/** scalac options for the scope */" -> "val scalacOptions = " -> scalacOptions,
      "/** compiler plugins used in this scope */" -> "val scalaCompilerPlugins = " ->
        scalaCompilerPlugins.map(_.toString()),
      "/** dependencies used in this scope */" -> "val dependencies = " ->
        dependencies.map(_.toString()),
      "/** dependency resolvers used in this scope */" -> "val resolvers = "    -> resolvers,
      "/** resource directories used in this scope */" -> "val resourceDirs = " -> resourceDirs,
      "/** custom jars added to this scope */" -> "val customJarsDecls = " -> customJarsDecls
    ).flatMap {
      case ((scaladoc, prefix), values) =>
        val sb = new StringBuilder
        sb.append(prefix)
        if values.isEmpty
        then sb.append("Nil")
        else
          sb.append {
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

  /** Build a [[ScopedBuildInfo]] for [[scope]] and inject the JVM test-runner dependency that
    * scala-cli silently adds at test time, so consumers of `export --json` see the same classpath
    * scala-cli would use.
    *
    * Injection conditions match [[scala.build.Artifacts.apply]]: scope is Test, the scope is
    * non-empty (has sources), the platform is JVM, and the build has a Scala version (i.e. is not
    * Java-only).
    */
  def forScope(
    options: BuildOptions,
    sourcePaths: Seq[String],
    scope: Scope,
    logger: Logger
  ): ScopedBuildInfo = {
    val base = apply(options, sourcePaths)
    if scope == Scope.Test && sourcePaths.nonEmpty then
      withJvmTestRunner(base, options, logger)
    else base
  }

  private def withJvmTestRunner(
    base: ScopedBuildInfo,
    options: BuildOptions,
    logger: Logger
  ): ScopedBuildInfo = {
    val isJvm        = options.platform.value == Platform.JVM
    val scalaParams  = options.scalaParams.toOption.flatten
    val isScalaBuild = scalaParams.nonEmpty
    if isJvm && isScalaBuild then {
      val params  = scalaParams.get
      val dep     = scala.build.Artifacts.jvmTestRunnerExportDependency(
        scalaVersion = params.scalaVersion,
        scalaBinaryVersion = params.scalaBinaryVersion,
        jvmVersion = options.javaHome().value.version,
        logger = logger
      )
      base.copy(dependencies = base.dependencies :+ dep)
    }
    else base
  }

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
      dep.versionConstraint.asString
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
