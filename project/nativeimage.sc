import $file.settings, settings.cs

import scala.util.Properties

def generateNativeImage(
  graalVmVersion: String,
  classPath: Seq[os.Path],
  mainClass: String,
  dest: os.Path
): Unit = {

  val graalVmHome = Option(System.getenv("GRAALVM_HOME")).getOrElse {
    import sys.process._
    Seq(cs, "java-home", "--jvm", s"graalvm-java11:$graalVmVersion", "--jvm-index", "cs").!!.trim
  }

  val ext = if (Properties.isWin) ".cmd" else ""
  val nativeImage = s"$graalVmHome/bin/native-image$ext"

  if (!os.isFile(os.Path(nativeImage))) {
    val ret = os.proc(s"$graalVmHome/bin/gu$ext", "install", "native-image").call(
      stdin = os.Inherit,
      stdout = os.Inherit,
      stderr = os.Inherit
    )
    if (ret.exitCode != 0)
      System.err.println(s"Warning: 'gu install native-image' exited with return code ${ret.exitCode}}")
    if (!os.isFile(os.Path(nativeImage)))
      System.err.println(s"Warning: $nativeImage not found, and not installed by 'gu install native-image'")
  }

  val swovalLibraryName =
    if (Properties.isWin) "swoval-files0.dll"
    else if (Properties.isMac) "libswoval-files0.dylib"
    else "libswoval-files0.so"

  val finalCp =
    if (Properties.isWin) {
      import java.util.jar.Attributes
      import java.util.jar.JarOutputStream
      import java.util.jar.Manifest
      val manifest = new Manifest
      val attributes = manifest.getMainAttributes
      attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
      attributes.put(Attributes.Name.CLASS_PATH, classPath.map(_.toIO.getAbsolutePath).mkString(" "))
      val jarFile = java.io.File.createTempFile("classpathJar", ".jar")
      val jos = new JarOutputStream(new java.io.FileOutputStream(jarFile), manifest)
      jos.close()
      jarFile.getAbsolutePath
    } else
      classPath.map(_.toIO.getAbsolutePath).mkString(java.io.File.pathSeparator)

  val command = Seq(
    nativeImage,
    "--no-fallback",
    "--enable-url-protocols=https",
    "--initialize-at-build-time=scala.Symbol",
    "--initialize-at-build-time=scala.Symbol$",
    "--initialize-at-build-time=scala.Function1",
    "--initialize-at-build-time=scala.Function2",
    "--initialize-at-build-time=scala.runtime.StructuralCallSite",
    "--initialize-at-build-time=scala.runtime.EmptyMethodCache",
    "--initialize-at-build-time=scala.collection.immutable.VM",
    "-H:IncludeResources=library.properties",
    "-H:IncludeResources=amm-dependencies.txt",
    "-H:IncludeResources=bootstrap.*.jar",
    "-H:IncludeResources=coursier/coursier.properties",
    "-H:IncludeResources=coursier/launcher/coursier.properties",
    "-H:IncludeResources=coursier/launcher/.*.bat",
    "-H:IncludeResources=org/scalajs/linker/backend/emitter/.*.sjsir",
    "-H:IncludeResources=native/x86_64/libswoval-files0.dylib",
    "--allow-incomplete-classpath",
    "--report-unsupported-elements-at-runtime",
    "-H:+ReportExceptionStackTraces",
    s"-H:Name=${dest.relativeTo(os.pwd)}",
    "-cp",
    finalCp,
    mainClass
  )

  val finalCommand =
    if (Properties.isWin) {
      // chcp 437 sometimes needed, see https://github.com/oracle/graal/issues/2522

      val vcvarsall = """"C:\Program Files (x86)\Microsoft Visual Studio\2017\Community\VC\Auxiliary\Build\vcvars64.bat""""
      val script =
       s"""chcp 437
          |@call $vcvarsall
          |@call ${command.mkString(" ")}
          |""".stripMargin
      val scriptPath = os.temp(script.getBytes, prefix = "run-native-image", suffix = ".bat")
      Seq(scriptPath.toString)
    } else
      command

  val res = os.proc(finalCommand.map(x => x: os.Shellable): _*).call(
    stdin = os.Inherit,
    stdout = os.Inherit,
    stderr = os.Inherit
  )
  if (res.exitCode != 0)
    sys.error(s"native-image command exited with ${res.exitCode}")
}
