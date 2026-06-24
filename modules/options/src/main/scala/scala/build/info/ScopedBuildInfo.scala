package scala.build.info

import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.{Dependency, LocalRepositories, Repositories}
import dependency.*

import scala.build.Logger
import scala.build.options.{BuildOptions, ConfigMonoid, Platform, Scope}

final case class ScopedBuildInfo(
  sources: Seq[String] = Nil,
  scalacOptions: Seq[String] = Nil,
  scalaCompilerPlugins: Seq[ExportDependencyFormat] = Nil,
  dependencies: Seq[ExportDependencyFormat] = Nil,
  compileOnlyDependencies: Seq[ExportDependencyFormat] = Nil,
  /** Dependencies scala-cli adds to this scope's Coursier resolution beyond what the user declared
    * — e.g. the JVM test-runner, Scala Native runtime libs, the Native test-interface. Populated by
    * `export --json` (so consumers like packaging tools see the same effective resolution input
    * scala-cli would use at test/build time); empty in the generated runtime `BuildInfo` since the
    * user didn't write these and scala-cli supplies them automatically.
    */
  injectedDependencies: Seq[ExportDependencyFormat] = Nil,
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

  /** Build a [[ScopedBuildInfo]] for [[scope]]. When [[injectTestRunner]] is true, also inject the
    * direct dependencies that scala-cli adds to this scope's Coursier resolution at `test` time, so
    * consumers of `export --json` see the same effective resolution input scala-cli would use. The
    * runtime `BuildInfo.scala` generator must leave it false — these deps are supplied by scala-cli
    * at runtime, not declared by the user.
    *
    * scala-cli builds the main and test scopes as two separate `Build`s, each with its own Coursier
    * resolution. This method models that: each scope's `dependencies` field is the effective
    * direct-dep set for that scope's standalone resolution. When the two scopes pick conflicting
    * transitive winners (e.g. a test framework pinning a higher Scala Native version than the main
    * scope's `scala3lib_native`), each scope still gets its own winner — the export preserves that
    * asymmetry by listing per-scope effective inputs.
    *
    * Injected items per scope (all gated on `injectTestRunner = true`, mirroring how
    * `addTestRunnerDependency = true` on the shared `BuildOptions` flows into both scopes inside
    * scala-cli's `test` command):
    *   - Native runtime deps (`javalib_native`, `scala3lib_native`/`scalalib_native`) and the
    *     `nscplugin` compiler plugin: both scopes, when platform is Native.
    *   - JS runtime deps (`scalajs-library`) and the Scala 2 `scalajs-compiler` plugin: both
    *     scopes, when platform is JS.
    *   - JVM test-runner (`org.virtuslab.scala-cli:test-runner_<scalaBinary>`): Test scope, when
    *     platform is JVM and the build has a Scala version.
    *   - Native test-interface (`org.scala-native:test-interface_native<snBinary>_<scalaBinary>`):
    *     Test scope, when platform is Native and Scala Native is at least 0.4.3. Coursier may still
    *     pick a higher transitive version as the test scope's winner, but the main scope's
    *     resolution pins scala-cli's bundled version, so an offline cache built from this export
    *     carries both.
    *   - JS test-bridge (`org.scala-js:scalajs-test-bridge`): Test scope, when platform is JS.
    *
    * Each injection is also gated on the relevant scope being non-empty (matching
    * [[scala.build.Artifacts.apply]]'s gating).
    */
  def forScope(
    options: BuildOptions,
    sourcePaths: Seq[String],
    scope: Scope,
    logger: Logger,
    injectTestRunner: Boolean = false
  ): ScopedBuildInfo = {
    val base = apply(options, sourcePaths)
    if injectTestRunner && sourcePaths.nonEmpty then
      withInjectedDeps(base, options, scope, logger)
    else base
  }

  /** Inject the direct dependencies scala-cli adds to this scope's Coursier resolution at test
    * time. See [[forScope]] for the per-platform/scope conditions.
    */
  private def withInjectedDeps(
    base: ScopedBuildInfo,
    options: BuildOptions,
    scope: Scope,
    logger: Logger
  ): ScopedBuildInfo = {
    val scalaParamsOpt = options.scalaParams.toOption.flatten
    if scalaParamsOpt.isEmpty then base
    else {
      val scalaParams  = scalaParamsOpt.get
      val platform     = options.platform.value
      val isTest       = scope == Scope.Test
      val platformDeps = platform match {
        case Platform.Native => nativeScopeDeps(options, scalaParamsOpt)
        case Platform.JS     => jsScopeDeps(options, scalaParamsOpt)
        case _               => Nil
      }
      val testRunnerDepOpt = (platform, isTest) match {
        case (Platform.JVM, true) =>
          Some(scala.build.Artifacts.jvmTestRunnerExportDependency(
            scalaVersion = scalaParams.scalaVersion,
            scalaBinaryVersion = scalaParams.scalaBinaryVersion,
            jvmVersion = options.javaHome().value.version,
            logger = logger
          ))
        case (Platform.Native, true) =>
          nativeTestInterfaceExportDependency(options, scalaParamsOpt)
        case (Platform.JS, true) =>
          Some(jsTestBridgeExportDependency(options))
        case _ => None
      }
      val injected = platformDeps ++ testRunnerDepOpt.toSeq
      if injected.isEmpty then base
      else base.copy(injectedDependencies = base.injectedDependencies ++ injected)
    }
  }

  /** Native runtime deps + compiler plugin that scala-cli adds to every Native scope's resolution
    * (both Main and Test). These come from [[ScalaNativeOptions.nativeDependencies]] and
    * [[ScalaNativeOptions.compilerPlugins]], which feed `BuildOptions.defaultDependencies`.
    */
  private def nativeScopeDeps(
    options: BuildOptions,
    scalaParamsOpt: Option[dependency.ScalaParameters]
  ): Seq[ExportDependencyFormat] = {
    val nativeOptions = options.scalaNativeOptions
    val sv            = scalaParamsOpt.map(_.scalaVersion)
      .getOrElse(scala.build.internal.Constants.defaultScalaVersion)
    val runtime         = nativeOptions.nativeDependencies(sv)
    val compilerPlugins = nativeOptions.compilerPlugins
    (runtime ++ compilerPlugins).map(ExportDependencyFormat(_, scalaParamsOpt))
  }

  /** JS runtime deps + compiler plugin (Scala 2 only) that scala-cli adds to every JS scope's
    * resolution. Counterpart of [[nativeScopeDeps]] for the JS platform.
    */
  private def jsScopeDeps(
    options: BuildOptions,
    scalaParamsOpt: Option[dependency.ScalaParameters]
  ): Seq[ExportDependencyFormat] = {
    val jsOptions = options.scalaJsOptions
    val sv        = scalaParamsOpt.map(_.scalaVersion)
      .getOrElse(scala.build.internal.Constants.defaultScalaVersion)
    val runtime         = jsOptions.jsDependencies(sv)
    val compilerPlugins = jsOptions.compilerPlugins(sv)
    (runtime ++ compilerPlugins).map(ExportDependencyFormat(_, scalaParamsOpt))
  }

  /** Mirrors [[scala.build.options.BuildOptions.addNativeTestInterface]]: returns the
    * `test-interface_native<snBinary>_<scalaBinary>:<snVersion>` dep that scala-cli's test-time
    * resolution injects, when Scala Native is at least 0.4.3 (the version that started shipping a
    * separate `test-interface` artifact).
    */
  private def nativeTestInterfaceExportDependency(
    options: BuildOptions,
    scalaParamsOpt: Option[dependency.ScalaParameters]
  ): Option[ExportDependencyFormat] = {
    val snVersion = options.scalaNativeOptions.finalVersion
    val minVer    = coursier.core.Version("0.4.3")
    if minVer.compareTo(coursier.core.Version(snVersion)) <= 0 then
      Some(ExportDependencyFormat(
        dep"org.scala-native::test-interface::$snVersion",
        scalaParamsOpt
      ))
    else None
  }

  /** Mirrors the JS test-bridge injection in [[scala.build.Artifacts.apply]]: the artifact id
    * differs between Scala 2.x (cross-versioned with the project's Scala binary) and Scala 3
    * (always `scalajs-test-bridge_2.13`).
    */
  private def jsTestBridgeExportDependency(
    options: BuildOptions
  ): ExportDependencyFormat = {
    val scalaParams = options.scalaParams.toOption.flatten
    val sv          = scalaParams.map(_.scalaVersion).getOrElse("")
    val jsVersion   = options.scalaJsOptions.finalVersion
    val dep0        =
      if sv.startsWith("2.") then dep"org.scala-js::scalajs-test-bridge:$jsVersion"
      else dep"org.scala-js:scalajs-test-bridge_2.13:$jsVersion"
    ExportDependencyFormat(dep0, scalaParams)
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
