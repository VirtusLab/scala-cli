package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File
import java.util.zip.ZipFile

import scala.jdk.CollectionConverters.*
import scala.util.{Properties, Using}

trait LazyValTests:
  private val lazyValsLibOrg     = "test.lazyvals"
  private val lazyValsLibName    = "lazyvals-lib"
  private val lazyValsLibVersion = "0.1.0"
  protected val lazyValsLibDep   = s"$lazyValsLibOrg::$lazyValsLibName:$lazyValsLibVersion"

  protected val slothNoOpWarnPrefix: String                = "Sloth patching is not applicable to"
  protected val slothSignatureStrippedWarnFragment: String = "signature files were removed"
  protected val slothCacheSegment: String = s"${File.separator}sloth${File.separator}"
  protected val slothOptions: Seq[String] =
    Seq("--sloth", "--suppress-experimental-feature-warning")
  protected val slothAgentOptions: Seq[String] =
    Seq("--sloth-agent", "--suppress-experimental-feature-warning")

  protected val signedLibMessage: String = "Hello from signed library"

  protected def javaHome(jvm: Int): os.Path =
    os.Path(
      os.proc(TestUtil.cs, "java-home", "--jvm", jvm.toString).call().out.trim(),
      os.pwd
    )

  /** Path to a tool in a given JDK's bin directory (keytool, jarsigner, ...). */
  protected def jdkTool(jvm: Int, tool: String): os.Path =
    javaHome(jvm) / "bin" / (if Properties.isWin then s"$tool.exe" else tool)

  /** Signs `jar` in place with a throwaway self-signed certificate. */
  protected def signJar(jar: os.Path, workspace: os.Path, jvm: Int): Unit =
    val keystore = workspace / s"${jar.baseName}-keystore.p12"
    os.proc(
      jdkTool(jvm, "keytool").toString,
      "-genkeypair",
      "-alias",
      "testkey",
      "-keyalg",
      "RSA",
      "-keysize",
      "2048",
      "-validity",
      "1",
      "-keystore",
      keystore.toString,
      "-storepass",
      "changeit",
      "-dname",
      "CN=Test,OU=Test,O=Test,L=Test,ST=Test,C=US",
      "-storetype",
      "PKCS12"
    ).call(cwd = workspace)
    os.proc(
      jdkTool(jvm, "jarsigner").toString,
      "-keystore",
      keystore.toString,
      "-storepass",
      "changeit",
      jar.toString,
      "testkey"
    ).call(cwd = workspace)

  /** Returns signature entry names in a JAR (META-INF entries: .SF, .DSA, .RSA, .EC, SIG-). */
  protected def signatureEntriesIn(jar: os.Path): Seq[String] =
    Using.resource(ZipFile(jar.toIO)): zf =>
      zf.entries().asScala.map(_.getName).filter { e =>
        e.startsWith("META-INF/") && {
          val n = e.stripPrefix("META-INF/")
          !n.contains("/") &&
          (Set("sf", "dsa", "rsa", "ec").contains(n.split('.').last.toLowerCase) ||
          n.toUpperCase.startsWith("SIG-"))
        }
      }.toSeq

  protected def jarManifestMainAttributes(jar: os.Path): Map[String, String] =
    Using.resource(ZipFile(jar.toIO)): zf =>
      val entries = zf.entries().asScala.map(_.getName).toSeq
      expect(entries.count(_ == "META-INF/MANIFEST.MF") == 1)
      val manifest = new java.util.jar.Manifest(
        zf.getInputStream(zf.getEntry("META-INF/MANIFEST.MF"))
      )
      manifest.getMainAttributes.asScala.map { case (k, v) =>
        k.toString -> String.valueOf(v)
      }.toMap

  protected val userManifestResourceContent: String =
    """Manifest-Version: 1.0
      |X-Custom: yes
      |""".stripMargin

  protected def expectScaladocClasspathContains(output: String, fragment: String): Unit =
    val marker       = "dotty.tools.scaladoc.Main -classpath "
    val classpathOpt = output.split(marker).lift(1).map(_.takeWhile(c => c != ' ' && c != '\n'))
    expect(classpathOpt.exists(_.contains(fragment)))

  protected def publishLazyValsLib(
    scalaVersion: String,
    workspace: os.Path,
    buildJvm: Option[String] = None
  ): (String, os.Path) =
    val libDir  = workspace / "lazyvals-lib"
    val repoDir = workspace / "test-repo"
    os.write(
      libDir / "LazyValsLib.scala",
      """package lazyvalslib
        |object LazyValsLib {
        |  lazy val greeting: String = "Hello"
        |}
        |""".stripMargin,
      createFolders = true
    )
    os.proc(
      TestUtil.cli,
      "--power",
      "publish",
      libDir,
      "--organization",
      lazyValsLibOrg,
      "--name",
      lazyValsLibName,
      "--project-version",
      lazyValsLibVersion,
      "--scala",
      scalaVersion,
      buildJvm.toSeq.flatMap(j => Seq("--jvm", j)),
      "--publish-repo",
      repoDir.toNIO.toUri.toASCIIString
    ).call(cwd = workspace, stdin = os.Inherit, stdout = os.Inherit)
    os.remove.all(libDir)
    (lazyValsLibDep, repoDir)

  private def packageAndSignLibrary(
    workspace: os.Path,
    jvm: Int,
    name: String,
    sourceName: String,
    sourceContent: String
  ): os.Path =
    val libDir  = workspace / s"$name-src"
    val jarPath = workspace / s"$name.jar"
    os.write(libDir / sourceName, sourceContent, createFolders = true)
    os.proc(TestUtil.cli, "--power", "package", "--library", libDir, "-o", jarPath)
      .call(cwd = workspace, stdin = os.Inherit, stdout = os.Inherit)
    os.remove.all(libDir)
    signJar(jarPath, workspace, jvm)
    jarPath

  /** Publishes a library JAR with lazy vals and signs it using jarsigner. Returns the path to the
    * signed JAR.
    */
  protected def publishSignedLazyValsJar(
    scalaVersion: String,
    workspace: os.Path,
    jvm: Int
  ): os.Path =
    packageAndSignLibrary(
      workspace,
      jvm,
      "signed-lib",
      "SignedLib.scala",
      s"""//> using scala $scalaVersion
         |package signedlib
         |object SignedLib {
         |  lazy val greeting: String = "$signedLibMessage"
         |}
         |""".stripMargin
    )

  /** Packages a library with lazy vals to `dest` (may be extension-less). Returns `dest`. */
  protected def packageLazyValsLibrary(
    scalaVersion: String,
    workspace: os.Path,
    dest: os.Path,
    packageName: String = "extless-lib"
  ): os.Path =
    val libDir = workspace / s"$packageName-src"
    os.write(
      libDir / "ExtLessLib.scala",
      s"""//> using scala $scalaVersion
         |package extlesslib
         |object ExtLessLib {
         |  lazy val greeting: String = "$signedLibMessage"
         |}
         |""".stripMargin,
      createFolders = true
    )
    os.proc(TestUtil.cli, "--power", "package", "--library", libDir, "-o", dest)
      .call(cwd = workspace, stdin = os.Inherit, stdout = os.Inherit)
    os.remove.all(libDir)
    dest

  /** Publishes a library JAR without lazy vals (pure Java or Scala 3.8+) and signs it. Returns the
    * path to the signed JAR.
    */
  protected def publishSignedJavaJar(
    workspace: os.Path,
    jvm: Int
  ): os.Path =
    packageAndSignLibrary(
      workspace,
      jvm,
      "signed-java-lib",
      "JavaLib.java",
      """package javalib;
        |public class JavaLib {
        |  public static String greeting() { return "Hello from Java"; }
        |}
        |""".stripMargin
    )
