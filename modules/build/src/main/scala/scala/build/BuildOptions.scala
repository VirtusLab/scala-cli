package scala.build

import _root_.bloop.config.{Config => BloopConfig}
import dependency._

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest

import scala.build.internal.CodeWrapper
import scala.build.internal.Constants._

final case class BuildOptions(
                scalaVersion: Option[String],
          scalaBinaryVersion: Option[String],
                 codeWrapper: Option[CodeWrapper]        = None,
              scalaJsOptions: Option[BuildOptions.ScalaJsOptions]     = None,
          scalaNativeOptions: Option[BuildOptions.ScalaNativeOptions] = None,
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

  // not customizable from config or source files
                fetchSources: Option[Boolean]            = None,
             keepDiagnostics: Boolean                    = false
) {
  def addStubsDependency: Boolean =
    addStubsDependencyOpt.getOrElse(true)
  def addRunnerDependency: Boolean =
    scalaJsOptions.isEmpty && scalaNativeOptions.isEmpty && addRunnerDependencyOpt.getOrElse(true)
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
    scalaJsOptions.map(_.jsDependencies).getOrElse(Nil) ++
      scalaNativeOptions.map(_.nativeDependencies).getOrElse(Nil) ++
      scalaLibraryDependencies(params)

  def semanticDbPlugins(params: ScalaParameters): Seq[AnyDependency] =
    if (generateSemanticDbs.getOrElse(false) && params.scalaVersion.startsWith("2."))
      Seq(
        dep"$semanticDbPluginOrganization:::$semanticDbPluginModuleName:$semanticDbPluginVersion"
      )
    else Nil

  def compilerPlugins(params: ScalaParameters): Seq[AnyDependency] =
    scalaJsOptions.map(_.compilerPlugins).getOrElse(Nil) ++
      scalaNativeOptions.map(_.compilerPlugins).getOrElse(Nil) ++
      semanticDbPlugins(params)

  def allExtraJars: Seq[Path] =
    extraJars.map(_.toNIO)

  def addJvmTestRunner: Boolean = scalaJsOptions.isEmpty && scalaNativeOptions.isEmpty && addTestRunnerDependency
  def addJsTestBridge: Option[String] = if (addTestRunnerDependency) scalaJsOptions.map(_.config.version) else None

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
    for (jsOpts <- scalaJsOptions) {
      update("js=" + jsOpts.config.version + "\n")
      update("js.suffix=" + jsOpts.platformSuffix + "\n")
      for (plugin <- jsOpts.compilerPlugins)
        update("js.compilerPlugin+=" + plugin.render + "\n")
      for (dep <- jsOpts.jsDependencies)
        update("js.jsDeps+=" + dep.render + "\n")
    }
    for (nativeOpts <- scalaNativeOptions) {
      update("native=" + nativeOpts.config.version + "\n")
      update("native.suffix=" + nativeOpts.platformSuffix + "\n")
      for (plugin <- nativeOpts.compilerPlugins)
        update("native.compilerPlugin+=" + plugin.render + "\n")
      for (dep <- nativeOpts.nativeDependencies)
        update("native.nativeDeps+=" + dep.render + "\n")
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
}

object BuildOptions {

  final case class ScalaJsOptions(
    platformSuffix: String,
    jsDependencies: Seq[AnyDependency],
    compilerPlugins: Seq[AnyDependency],
    config: BloopConfig.JsConfig
  )

  final case class ScalaNativeOptions(
    platformSuffix: String,
    nativeDependencies: Seq[AnyDependency],
    compilerPlugins: Seq[AnyDependency],
    config: BloopConfig.NativeConfig
  )

  final case class RunJmhOptions(
    preprocess: Boolean,
    javaCommand: String
  )

}
