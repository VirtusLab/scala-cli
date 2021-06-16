package scala.build

import _root_.bloop.config.{Config => BloopConfig}
import dependency._
import org.scalajs.linker.interface.{ESFeatures, ModuleKind, Semantics, StandardConfig}

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Locale

import scala.build.internal.CodeWrapper
import scala.build.internal.Constants
import scala.build.internal.Constants._
import scala.scalanative.{build => sn}
import java.nio.file.Paths
import coursier.cache.FileCache
import coursier.util.Task
import coursier.jvm.JvmCache
import coursier.jvm.JavaHome
import scala.util.Properties

final case class BuildOptions(
                scalaVersion: Option[String]             = None,
          scalaBinaryVersion: Option[String]             = None,
                 codeWrapper: Option[CodeWrapper]        = None,
              scalaJsOptions: BuildOptions.ScalaJsOptions = BuildOptions.ScalaJsOptions(),
          scalaNativeOptions: BuildOptions.ScalaNativeOptions = BuildOptions.ScalaNativeOptions(),
                 javaHomeOpt: Option[String]             = None,
                    jvmIdOpt: Option[String]             = None,
       addStubsDependencyOpt: Option[Boolean]            = None,
      addRunnerDependencyOpt: Option[Boolean]            = None,
  addTestRunnerDependencyOpt: Option[Boolean]            = None,
          addJmhDependencies: Option[String]             = None,
                      runJmh: Option[BuildOptions.RunJmhOptions]      = None,
             addScalaLibrary: Option[Boolean]            = None,
         generateSemanticDbs: Option[Boolean]            = None,
           extraRepositories: Seq[String]                = Nil,
                   extraJars: Seq[os.Path]               = Nil,

  // not customizable from config or source files - move elsewhere?
                fetchSources: Option[Boolean]            = None,
             keepDiagnostics: Boolean                    = false,
                       cache: Option[FileCache[Task]]    = None
) {
  def addStubsDependency: Boolean =
    addStubsDependencyOpt.getOrElse(true)
  def addRunnerDependency: Boolean =
    !scalaJsOptions.enable && !scalaNativeOptions.enable && addRunnerDependencyOpt.getOrElse(true)
  def addTestRunnerDependency: Boolean =
    addTestRunnerDependencyOpt.getOrElse(false)

  // lazy val params = ScalaParameters(scalaVersion, scalaBinaryVersion)

  def scalaLibraryDependencies(params: ScalaParameters): Seq[AnyDependency] =
    if (addScalaLibrary.getOrElse(true)) {
      val lib =
        if (params.scalaVersion.startsWith("3."))
          dep"org.scala-lang::scala3-library:${params.scalaVersion}"
        else
          dep"org.scala-lang:scala-library:${params.scalaVersion}"
      Seq(lib)
    }
    else Nil

  def dependencies(params: ScalaParameters): Seq[AnyDependency] =
    scalaJsOptions.jsDependencies ++
      scalaNativeOptions.nativeDependencies ++
      scalaLibraryDependencies(params)

  def semanticDbPlugins(params: ScalaParameters): Seq[AnyDependency] =
    if (generateSemanticDbs.getOrElse(false) && params.scalaVersion.startsWith("2."))
      Seq(
        dep"$semanticDbPluginOrganization:::$semanticDbPluginModuleName:$semanticDbPluginVersion"
      )
    else Nil

  def compilerPlugins(params: ScalaParameters): Seq[AnyDependency] =
    scalaJsOptions.compilerPlugins ++
      scalaNativeOptions.compilerPlugins ++
      semanticDbPlugins(params)

  def allExtraJars: Seq[Path] =
    extraJars.map(_.toNIO)

  def addJvmTestRunner: Boolean = !scalaJsOptions.enable && !scalaNativeOptions.enable && addTestRunnerDependency
  def addJsTestBridge: Option[String] = if (addTestRunnerDependency) Some(scalaJsOptions.finalVersion) else None


  private lazy val finalCache = cache.getOrElse(FileCache())
  // This might download a JVM if --jvm … is passed or no system JVM is installed
  private lazy val javaCommand0: String = {
    val javaHomeOpt0 = javaHomeOpt.filter(_.nonEmpty)
      .orElse(if (jvmIdOpt.isEmpty) sys.props.get("java.home") else None)
      .map(os.Path(_, Os.pwd))
      .orElse {
        implicit val ec = finalCache.ec
        val (id, path) = javaHomeManager.getWithRetainedId(jvmIdOpt.getOrElse(JavaHome.defaultId)).unsafeRun()
        if (id == JavaHome.systemId) None
        else Some(os.Path(path))
      }
    val ext = if (Properties.isWin) ".exe" else ""

    javaHomeOpt0.fold("java")(javaHome => (javaHome / "bin" / s"java$ext").toString)
  }

  def javaHomeLocation(): os.Path = {
    implicit val ec = finalCache.ec
    val path = javaHomeManager.get(jvmIdOpt.getOrElse(JavaHome.defaultId)).unsafeRun()
    os.Path(path)
  }

  def javaCommand(): String = javaCommand0

  def javaHomeManager = {
    val jvmCache = JvmCache().withDefaultIndex.withCache(finalCache)
    JavaHome().withCache(jvmCache)
  }

  def artifacts(params: ScalaParameters, userDependencies: Seq[AnyDependency], logger: Logger): Artifacts =
    Artifacts(
             javaHomeOpt = javaHomeOpt.filter(_.nonEmpty),
                jvmIdOpt = jvmIdOpt,
                  params = params,
         compilerPlugins = compilerPlugins(params),
            dependencies = userDependencies ++ dependencies(params),
               extraJars = allExtraJars,
            fetchSources = fetchSources.getOrElse(false),
                addStubs = addStubsDependency,
            addJvmRunner = addRunnerDependency,
        addJvmTestRunner = addJvmTestRunner,
         addJsTestBridge = addJsTestBridge,
      addJmhDependencies = addJmhDependencies,
       extraRepositories = extraRepositories,
                  logger = logger
    )

  lazy val hash: Option[String] = {
    val md = MessageDigest.getInstance("SHA-1")

    var hasAnyOverride = false

    def update(s: String): Unit = {
      val bytes = s.getBytes(StandardCharsets.UTF_8)
      if (bytes.length > 0) {
        hasAnyOverride = true
        md.update(bytes)
      }
    }
    for (sv <- scalaVersion)
      update("scalaVersion=" + sv + "\n")
    for (sbv <- scalaBinaryVersion)
      update("scalaBinaryVersion=" + sbv + "\n")
    for (wrapper <- codeWrapper)
      // kind of meh to use wrapper.toString here…
      update("codeWrapper=" + wrapper.toString + "\n")
    if (scalaJsOptions.enable) {
      update("js=" + scalaJsOptions.version.getOrElse("default") + "\n")
      update("js.mode=" + scalaJsOptions.mode + "\n")
      for (moduleKindStr <- scalaJsOptions.moduleKindStr)
        update("js.moduleKind=" + moduleKindStr + "\n")
      for (checkIr <- scalaJsOptions.checkIr)
        update("js.checkIr=" + checkIr + "\n")
      update("js.emitSourceMaps=" + scalaJsOptions.emitSourceMaps + "\n")
      for (dom <- scalaJsOptions.dom)
        update("js.moduleKind=" + dom + "\n")
    }
    if (scalaNativeOptions.enable) {
      update("native=" + scalaNativeOptions.version.getOrElse("default") + "\n")

      for (version <- scalaNativeOptions.version)
        update("native.version=" + version + "\n")
      for (modeStr <- scalaNativeOptions.modeStr)
        update("native.modeStr=" + modeStr + "\n")
      for (gcStr <- scalaNativeOptions.gcStr)
        update("native.gcStr=" + gcStr + "\n")
      for (clang <- scalaNativeOptions.clang)
        update("native.clang=" + clang + "\n")
      for (clangpp <- scalaNativeOptions.clangpp)
        update("native.clangpp=" + clangpp + "\n")
      for (opt <- scalaNativeOptions.linkingOptions)
        update("native.linkingOptions+=" + opt + "\n")
      update("native.linkingDefaults=" + scalaNativeOptions.linkingDefaults + "\n")
      for (opt <- scalaNativeOptions.compileOptions)
        update("native.compileOptions+=" + opt + "\n")
      update("native.compileDefaults=" + scalaNativeOptions.compileDefaults + "\n")
    }

    for (home <- javaHomeOpt)
      update("javaHome=" + home + "\n")
    for (id <- jvmIdOpt)
      update("jvmId=" + id + "\n")

    for (add <- addStubsDependencyOpt)
      update("addStubsDependency=" + add.toString + "\n")
    for (add <- addRunnerDependencyOpt)
      update("addRunnerDependency=" + add.toString + "\n")
    for (add <- addTestRunnerDependencyOpt)
      update("addTestRunnerDependency=" + add.toString + "\n")

    for (dep <- addJmhDependencies)
      update("addJmhDependencies=" + dep + "\n")
    for (add <- addScalaLibrary)
      update("addScalaLibrary=" + add.toString + "\n")
    for (generate <- generateSemanticDbs)
      update("generateSemanticDbs=" + generate.toString + "\n")

    for (jar <- extraJars)
      update("jars+=" + jar.toString + "\n")

    if (hasAnyOverride) {
      val digest = md.digest()
      val calculatedSum = new BigInteger(1, digest)
      val hash = String.format(s"%040x", calculatedSum).take(10)
      Some(hash)
    }
    else None
  }

  def orElse(other: BuildOptions): BuildOptions =
    BuildOptions(
                scalaVersion = scalaVersion.orElse(other.scalaVersion),
          scalaBinaryVersion = scalaBinaryVersion.orElse(other.scalaBinaryVersion),
                 codeWrapper = codeWrapper.orElse(other.codeWrapper),
              scalaJsOptions = scalaJsOptions.orElse(other.scalaJsOptions),
          scalaNativeOptions = scalaNativeOptions.orElse(other.scalaNativeOptions),
                 javaHomeOpt = javaHomeOpt.orElse(other.javaHomeOpt),
                    jvmIdOpt = jvmIdOpt.orElse(other.jvmIdOpt),
       addStubsDependencyOpt = addStubsDependencyOpt.orElse(other.addStubsDependencyOpt),
      addRunnerDependencyOpt = addRunnerDependencyOpt.orElse(other.addRunnerDependencyOpt),
  addTestRunnerDependencyOpt = addTestRunnerDependencyOpt.orElse(other.addTestRunnerDependencyOpt),
          addJmhDependencies = addJmhDependencies.orElse(other.addJmhDependencies),
                      runJmh = runJmh.orElse(other.runJmh),
             addScalaLibrary = addScalaLibrary.orElse(other.addScalaLibrary),
         generateSemanticDbs = generateSemanticDbs.orElse(other.generateSemanticDbs),
           extraRepositories = extraRepositories ++ other.extraRepositories,
                   extraJars = extraJars ++ other.extraJars,
                fetchSources = fetchSources.orElse(other.fetchSources),
             keepDiagnostics = keepDiagnostics || other.keepDiagnostics,
                       cache = cache.orElse(other.cache)
    )
}

object BuildOptions {

  final case class ScalaJsOptions(
    enable: Boolean = false,
    version: Option[String] = None,
    mode: Option[String] = None,
    moduleKindStr: Option[String] = None,
    checkIr: Option[Boolean] = None,
    emitSourceMaps: Boolean = false,
    dom: Option[Boolean] = None
  ) {
    def platformSuffix: Option[String] =
      if (enable) Some("sjs" + ScalaVersion.jsBinary(finalVersion).getOrElse(finalVersion))
      else None
    def jsDependencies: Seq[AnyDependency] =
      if (enable) Seq(dep"org.scala-js::scalajs-library:$finalVersion")
      else Nil
    def compilerPlugins: Seq[AnyDependency] =
      if (enable) Seq(dep"org.scala-js:::scalajs-compiler:$finalVersion")
      else Nil

    def orElse(other: ScalaJsOptions): ScalaJsOptions =
      ScalaJsOptions(
        enable = enable || other.enable,
        version = version.orElse(other.version),
        mode = mode.orElse(other.mode),
        moduleKindStr = moduleKindStr.orElse(other.moduleKindStr),
        checkIr = checkIr.orElse(other.checkIr),
        emitSourceMaps = emitSourceMaps || other.emitSourceMaps,
        dom = dom.orElse(other.dom)
      )

    private def moduleKind: ModuleKind =
      moduleKindStr.map(_.trim.toLowerCase(Locale.ROOT)).getOrElse("") match {
        case "commonjs" | "common" => ModuleKind.CommonJSModule
        case "esmodule" | "es"     => ModuleKind.ESModule
        case "nomodule" | "none"   => ModuleKind.NoModule
        case _                     => ModuleKind.CommonJSModule
      }
    private def moduleKindName: String =
      moduleKind match {
        case ModuleKind.CommonJSModule => "commonjs"
        case ModuleKind.ESModule => "esmodule"
        case ModuleKind.NoModule => "nomodule"
      }

    def finalVersion = version.map(_.trim).filter(_.nonEmpty).getOrElse(Constants.scalaJsVersion)

    private def configUnsafe: BloopConfig.JsConfig = {
      val kind = moduleKind match {
        case ModuleKind.CommonJSModule => BloopConfig.ModuleKindJS.CommonJSModule
        case ModuleKind.ESModule       => BloopConfig.ModuleKindJS.ESModule
        case ModuleKind.NoModule       => BloopConfig.ModuleKindJS.NoModule
      }
      BloopConfig.JsConfig(
             version = finalVersion,
                mode = if (mode.contains("release")) BloopConfig.LinkerMode.Release else BloopConfig.LinkerMode.Debug,
                kind = kind,
      emitSourceMaps = emitSourceMaps,
               jsdom = dom,
              output = None,
            nodePath = None,
           toolchain = Nil
      )
    }

    def config: Option[BloopConfig.JsConfig] =
      if (enable) Some(configUnsafe)
      else None

    def linkerConfig: StandardConfig = {
      var config = StandardConfig()

      config = config
        .withModuleKind(moduleKind)

      for (checkIr <- checkIr)
        config = config.withCheckIR(checkIr)

      val release = mode.contains("release")

      config = config
        .withSemantics(Semantics.Defaults)
        .withESFeatures(ESFeatures.Defaults)
        .withOptimizer(release)
        .withParallel(true)
        .withSourceMap(emitSourceMaps)
        .withRelativizeSourceMapBase(None)
        .withClosureCompiler(release)
        .withPrettyPrint(false)
        .withBatchMode(true)

      config
    }

  }

  final case class ScalaNativeOptions(
    enable: Boolean = false,
    version: Option[String] = None,
    modeStr: Option[String] = None,
    gcStr: Option[String] = None,
    clang: Option[String] = None,
    clangpp: Option[String] = None,
    linkingOptions: List[String] = Nil,
    linkingDefaults: Boolean = true,
    compileOptions: List[String] = Nil,
    compileDefaults: Boolean = true
  ) {

    def orElse(other: ScalaNativeOptions): ScalaNativeOptions =
      ScalaNativeOptions(
        enable = enable || other.enable,
        version = version.orElse(other.version),
        modeStr = modeStr.orElse(other.modeStr),
        gcStr = gcStr.orElse(other.gcStr),
        clang = clang.orElse(other.clang),
        clangpp = clangpp.orElse(other.clangpp),
        linkingOptions = linkingOptions ++ other.linkingOptions,
        linkingDefaults = linkingDefaults || other.linkingDefaults,
        compileOptions = compileOptions ++ other.compileOptions,
        compileDefaults = compileDefaults || other.compileDefaults
      )

    def finalVersion = version.map(_.trim).filter(_.nonEmpty).getOrElse(Constants.scalaNativeVersion)

    private def gc: sn.GC =
      gcStr.map(_.trim).filter(_.nonEmpty) match {
        case Some("default") | None => sn.GC.default
        case Some(other) => sn.GC(other)
      }
    private def mode: sn.Mode =
      modeStr.map(_.trim).filter(_.nonEmpty) match {
        case Some("default") | None => sn.Discover.mode()
        case Some(other) => sn.Mode(other)
      }

    private def clangPath = clang.filter(_.nonEmpty).map(Paths.get(_)).getOrElse(sn.Discover.clang())
    private def clangppPath = clangpp.filter(_.nonEmpty).map(Paths.get(_)).getOrElse(sn.Discover.clangpp())
    private def finalLinkingOptions =
      linkingOptions ++ (if (linkingDefaults) sn.Discover.linkingOptions() else Nil)
    private def finalCompileOptions =
      compileOptions ++ (if (compileDefaults) sn.Discover.compileOptions() else Nil)

    def platformSuffix: Option[String] =
      if (enable) Some("native" + ScalaVersion.nativeBinary(finalVersion).getOrElse(finalVersion))
      else None
    def nativeDependencies: Seq[AnyDependency] =
      if (enable)
        Seq("nativelib", "javalib", "auxlib", "scalalib")
          .map(name => dep"org.scala-native::$name::$finalVersion")
      else
        Nil
    def compilerPlugins: Seq[AnyDependency] =
      if (enable) Seq(dep"org.scala-native:::nscplugin:$finalVersion")
      else Nil

    private def bloopConfigUnsafe: BloopConfig.NativeConfig =
      BloopConfig.NativeConfig(
             version = finalVersion,
                       // there are more modes than bloop allows, but that setting here shouldn't end up being used anyway
                mode = if (mode.name == "release") BloopConfig.LinkerMode.Release else BloopConfig.LinkerMode.Debug,
                  gc = gc.name,
        targetTriple = None,
               clang = clangPath,
             clangpp = clangppPath,
           toolchain = Nil,
             options = BloopConfig.NativeOptions(
                 linker = finalLinkingOptions,
               compiler = finalCompileOptions
             ),
           linkStubs = false,
               check = false,
                dump = false,
              output = None
      )

    def bloopConfig: Option[BloopConfig.NativeConfig] =
      if (enable) Some(bloopConfigUnsafe)
      else None

    private def configUnsafe: sn.NativeConfig =
      sn.NativeConfig.empty
        .withGC(gc)
        .withMode(mode)
        .withLinkStubs(false)
        .withClang(clangPath)
        .withClangPP(clangppPath)
        .withLinkingOptions(linkingOptions)
        .withCompileOptions(compileOptions)

    def config: Option[sn.NativeConfig] =
      if (enable) Some(configUnsafe)
      else None

  }

  final case class RunJmhOptions(
    preprocess: Boolean
  )

}
