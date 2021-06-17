package scala.build.options

import dependency._

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest

import scala.build.{Artifacts, Logger, Os}
import scala.build.internal.Constants._
import coursier.cache.FileCache
import coursier.jvm.JvmCache
import coursier.jvm.JavaHome
import scala.util.Properties

final case class BuildOptions(
                scalaOptions: ScalaOptions                = ScalaOptions(),
              scalaJsOptions: ScalaJsOptions              = ScalaJsOptions(),
          scalaNativeOptions: ScalaNativeOptions          = ScalaNativeOptions(),
        internalDependencies: InternalDependenciesOptions = InternalDependenciesOptions(),
                 javaOptions: JavaOptions                 = JavaOptions(),
                  jmhOptions: JmhOptions                  = JmhOptions(),
            classPathOptions: ClassPathOptions            = ClassPathOptions(),
               scriptOptions: ScriptOptions               = ScriptOptions(),
         generateSemanticDbs: Option[Boolean]             = None,
                    internal: InternalOptions             = InternalOptions()
) {
  def addRunnerDependency: Boolean =
    !scalaJsOptions.enable && !scalaNativeOptions.enable && internalDependencies.addRunnerDependencyOpt.getOrElse(true)

  private def scalaLibraryDependencies(params: ScalaParameters): Seq[AnyDependency] =
    if (scalaOptions.addScalaLibrary.getOrElse(true)) {
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
      scalaLibraryDependencies(params) ++
      classPathOptions.extraDependencies

  private def semanticDbPlugins(params: ScalaParameters): Seq[AnyDependency] =
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
    classPathOptions.extraJars.map(_.toNIO)

  private def addJvmTestRunner: Boolean = !scalaJsOptions.enable && !scalaNativeOptions.enable && internalDependencies.addTestRunnerDependency
  private def addJsTestBridge: Option[String] = if (internalDependencies.addTestRunnerDependency) Some(scalaJsOptions.finalVersion) else None


  private lazy val finalCache = internal.cache.getOrElse(FileCache())
  // This might download a JVM if --jvm … is passed or no system JVM is installed
  private lazy val javaCommand0: String = {
    val javaHomeOpt0 = javaOptions.javaHomeOpt.filter(_.nonEmpty)
      .orElse(if (javaOptions.jvmIdOpt.isEmpty) sys.props.get("java.home") else None)
      .map(os.Path(_, Os.pwd))
      .orElse {
        implicit val ec = finalCache.ec
        val (id, path) = javaHomeManager.getWithRetainedId(javaOptions.jvmIdOpt.getOrElse(JavaHome.defaultId)).unsafeRun()
        if (id == JavaHome.systemId) None
        else Some(os.Path(path))
      }
    val ext = if (Properties.isWin) ".exe" else ""

    javaHomeOpt0.fold("java")(javaHome => (javaHome / "bin" / s"java$ext").toString)
  }

  def javaHomeLocation(): os.Path = {
    implicit val ec = finalCache.ec
    val path = javaHomeManager.get(javaOptions.jvmIdOpt.getOrElse(JavaHome.defaultId)).unsafeRun()
    os.Path(path)
  }

  def javaCommand(): String = javaCommand0

  private def javaHomeManager = {
    val jvmCache = JvmCache().withDefaultIndex.withCache(finalCache)
    JavaHome().withCache(jvmCache)
  }

  private def finalRepositories: Seq[String] =
    classPathOptions.extraRepositories ++ internal.localRepository.toSeq

  def artifacts(params: ScalaParameters, userDependencies: Seq[AnyDependency], logger: Logger): Artifacts =
    Artifacts(
             javaHomeOpt = javaOptions.javaHomeOpt.filter(_.nonEmpty),
                jvmIdOpt = javaOptions.jvmIdOpt,
                  params = params,
         compilerPlugins = compilerPlugins(params),
            dependencies = userDependencies ++ dependencies(params),
               extraJars = allExtraJars,
            fetchSources = classPathOptions.fetchSources.getOrElse(false),
                addStubs = internalDependencies.addStubsDependency,
            addJvmRunner = addRunnerDependency,
        addJvmTestRunner = addJvmTestRunner,
         addJsTestBridge = addJsTestBridge,
      addJmhDependencies = jmhOptions.addJmhDependencies,
       extraRepositories = finalRepositories,
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
    scalaOptions.addHashData(update)
    scalaJsOptions.addHashData(update)
    scalaNativeOptions.addHashData(update)
    javaOptions.addHashData(update)
    internalDependencies.addHashData(update)
    jmhOptions.addHashData(update)
    classPathOptions.addHashData(update)
    scriptOptions.addHashData(update)

    for (generate <- generateSemanticDbs)
      update("generateSemanticDbs=" + generate.toString + "\n")

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
                scalaOptions = scalaOptions.orElse(other.scalaOptions),
              scalaJsOptions = scalaJsOptions.orElse(other.scalaJsOptions),
          scalaNativeOptions = scalaNativeOptions.orElse(other.scalaNativeOptions),
                 javaOptions = javaOptions.orElse(other.javaOptions),
        internalDependencies = internalDependencies.orElse(other.internalDependencies),
                  jmhOptions = jmhOptions.orElse(other.jmhOptions),
         generateSemanticDbs = generateSemanticDbs.orElse(other.generateSemanticDbs),
            classPathOptions = classPathOptions.orElse(other.classPathOptions),
               scriptOptions = scriptOptions.orElse(other.scriptOptions),
                    internal = internal.orElse(other.internal)
    )
}
