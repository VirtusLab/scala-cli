package scala.build.tests

import java.io.File
import java.net.URLClassLoader
import java.nio.file.attribute.FileTime
import java.util.concurrent.{Callable, CyclicBarrier, Executors}
import java.util.jar.{Attributes as JarAttributes, JarOutputStream, Manifest as JarManifest}
import java.util.zip.{ZipEntry, ZipFile}

import scala.build.internal.Constants
import scala.build.internal.util.WarningMessages
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.build.options.{BuildOptions, PostBuildOptions}
import scala.build.postprocessing.SlothPatcher
import scala.jdk.CollectionConverters.*
import scala.util.Using

class SlothPatcherTests extends TestUtil.ScalaCliBuildSuite:

  private def optionsWithSloth(enabled: Boolean, strict: Boolean = false): BuildOptions =
    BuildOptions(notForBloopOptions =
      PostBuildOptions(slothOpt = Some(enabled), slothStrictOpt = Some(strict))
    )

  test("transformClassPath returns unchanged when sloth disabled"):
    val logger    = TestLogger()
    val classPath = Seq(os.pwd / "a.jar", os.pwd / "b.jar")
    val options   = optionsWithSloth(enabled = false)
    val result    = SlothPatcher.transformClassPath(
      classPath,
      options,
      logger,
      patchProjectClassDirs = false,
      projectClassDirs = Set.empty
    )
    assert(result.isRight)
    assert(result.toOption.get == classPath)

  test("patchJarFile returns unchanged when sloth disabled"):
    val logger  = TestLogger()
    val jarPath = os.pwd / "test.jar"
    val options = optionsWithSloth(enabled = false)
    val result  = SlothPatcher.patchJarFile(jarPath, options, logger)
    assert(result.isRight)
    assert(result.toOption.get == jarPath)

  test("patchByteCodeZipEntries returns unchanged when sloth disabled"):
    val logger  = TestLogger()
    val entries = Seq((ZipEntry("Test.class"), Array[Byte](1, 2, 3)))
    val options = optionsWithSloth(enabled = false)
    val result  = SlothPatcher.patchByteCodeZipEntries(entries, options, logger)
    assert(result.isRight)
    assert(result.toOption.get == entries)

  test("patchByteCodeZipEntries returns empty when input is empty"):
    val logger       = TestLogger()
    val emptyEntries = Seq.empty[(ZipEntry, Array[Byte])]
    val options      = optionsWithSloth(enabled = true)
    val result       = SlothPatcher.patchByteCodeZipEntries(emptyEntries, options, logger)
    assert(result.isRight)
    assert(result.toOption.get.isEmpty)

  test("patchByteCodeZipEntries does not leak temp files or dirs"):
    val logger  = TestLogger()
    val tmpRoot = os.Path(sys.props("java.io.tmpdir"))
    val entries = Seq((ZipEntry("Test.class"), Array[Byte](1, 2, 3)))
    val options = optionsWithSloth(enabled = true)

    val before = os.list(tmpRoot).toSet
    val result = SlothPatcher.patchByteCodeZipEntries(entries, options, logger)
    assert(result.isRight)

    val leaked = (os.list(tmpRoot).toSet -- before).filter { p =>
      p.last.startsWith("sloth-entries-") ||
      (os.isDir(p) && os.list(p).exists(_.last.startsWith("sloth-entries-")))
    }
    assert(leaked.isEmpty, s"Leaked temp entries: $leaked")

  test("patchJarFile passes through non-jar files even when sloth enabled"):
    TestInputs.withTmpDir("sloth-test-"): root =>
      val logger   = TestLogger()
      val classDir = root / "classes"
      os.makeDir.all(classDir)
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.patchJarFile(classDir, options, logger)
      assert(result.isRight)
      assert(result.toOption.get == classDir)

  private def createEmptyJar(jarPath: os.Path): Unit =
    val manifest = JarManifest()
    manifest.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")
    Using.resource(JarOutputStream(os.write.outputStream(jarPath), manifest)): jos =>
      val entry = ZipEntry("resource.txt")
      jos.putNextEntry(entry)
      jos.write("hello".getBytes(java.nio.charset.StandardCharsets.UTF_8))
      jos.closeEntry()

  private def createPreambleLauncher(dest: os.Path, jarBytes: Array[Byte], preamble: String)
    : Array[Byte] =
    val preambleBytes = preamble.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    os.write(dest, preambleBytes ++ jarBytes, createFolders = true)
    preambleBytes

  test("zipStartOffset returns Some(0) for extension-less archive"):
    TestInputs.withTmpDir("sloth-zip-offset-"): root =>
      val archive = root / "lib_no_ext"
      createEmptyJar(archive)
      assert(SlothPatcher.zipStartOffset(archive).contains(0L))

  test("zipStartOffset returns preamble length for preamble-carrying archive"):
    TestInputs.withTmpDir("sloth-zip-offset-"): root =>
      val plainJar = root / "plain.jar"
      createEmptyJar(plainJar)
      val jarBytes = os.read.bytes(plainJar)
      val preamble =
        """#!/usr/bin/env sh
          |exec java -jar "$0" "$@"
          |""".stripMargin
      val launcher      = root / "app.jar"
      val preambleBytes = createPreambleLauncher(launcher, jarBytes, preamble)
      val offsetOpt     = SlothPatcher.zipStartOffset(launcher)
      assert(
        offsetOpt.contains(preambleBytes.length.toLong),
        s"Expected ${preambleBytes.length}, got $offsetOpt"
      )
      val payload = root / "payload.jar"
      os.write(payload, jarBytes)
      Using.resource(ZipFile(payload.toIO)): zf =>
        assert(zf.getEntry("resource.txt") != null)

  test("zipStartOffset returns None for non-archives"):
    TestInputs.withTmpDir("sloth-zip-offset-"): root =>
      val fakeJar = root / "fake.jar"
      os.write(fakeJar, "not a jar")
      val dir = root / "classes"
      os.makeDir.all(dir)
      assert(SlothPatcher.zipStartOffset(fakeJar).isEmpty)
      assert(SlothPatcher.zipStartOffset(dir).isEmpty)
      assert(SlothPatcher.zipStartOffset(root / "missing").isEmpty)

  test("patchJarFile attempts patching extension-less archive when sloth enabled"):
    TestInputs.withTmpDir("sloth-extless-"): root =>
      val logger  = RecordingLogger()
      val archive = root / "lib_no_ext"
      createEmptyJar(archive)
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.patchJarFile(archive, options, logger)
      assert(result.isRight)
      assert(
        logger.debugMessages.exists(_.contains("No lazy vals to patch")),
        s"Expected patch attempt debug log, got messages=${logger.messages} debug=${logger.debugMessages}"
      )

  test("patchJarFile attempts patching preamble-carrying archive when sloth enabled"):
    TestInputs.withTmpDir("sloth-preamble-"): root =>
      val logger   = RecordingLogger()
      val plainJar = root / "plain.jar"
      createEmptyJar(plainJar)
      val preamble =
        """#!/usr/bin/env sh
          |exec java -jar "$0" "$@"
          |""".stripMargin
      val launcher      = root / "app.jar"
      val preambleBytes = createPreambleLauncher(launcher, os.read.bytes(plainJar), preamble)
      assert(
        SlothPatcher.zipStartOffset(launcher).contains(preambleBytes.length.toLong),
        s"Expected zipStartOffset=${preambleBytes.length}, got ${SlothPatcher.zipStartOffset(launcher)}"
      )
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.patchJarFile(launcher, options, logger)
      assert(result.isRight)
      val patched = result.toOption.get
      val head    = os.read.bytes(patched, offset = 0, count = 2)
      assert(
        head.sameElements("#!".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        s"Expected patched launcher to keep preamble, got head=${head.toSeq}"
      )
      assert(
        logger.debugMessages.exists(_.contains("No lazy vals to patch")) ||
        logger.debugMessages.exists(_.contains("Patched lazy vals")),
        s"Expected patch attempt debug log, got messages=${logger.messages} debug=${logger.debugMessages}"
      )

  test("patchJarFile warns for non-archive file when sloth enabled"):
    TestInputs.withTmpDir("sloth-not-archive-"): root =>
      val logger  = RecordingLogger()
      val fakeJar = root / "fake.jar"
      os.write(fakeJar, "not a jar")
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.patchJarFile(fakeJar, options, logger)
      assert(result.isRight)
      assert(result.toOption.get == fakeJar)
      assert(
        logger.messages.exists(_.contains(WarningMessages.slothNotAnArchive(fakeJar))),
        s"Expected slothNotAnArchive warning, got: ${logger.messages}"
      )
      assert(
        logger.messages.exists(_.startsWith(warnPrefix)),
        s"Expected warnPrefix on warning, got: ${logger.messages}"
      )

  test("transformClassPath stays silent for plain text classpath entry"):
    TestInputs.withTmpDir("sloth-silent-"): root =>
      val logger  = RecordingLogger()
      val txtFile = root / "readme.txt"
      os.write(txtFile, "hello")
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.transformClassPath(
        Seq(txtFile),
        options,
        logger,
        patchProjectClassDirs = false,
        projectClassDirs = Set.empty
      )
      assert(result.isRight)
      assert(result.toOption.get == Seq(txtFile))
      assert(logger.messages.isEmpty, s"Expected no message-level output, got: ${logger.messages}")

  test("transformClassPath passes through directories when patchProjectClassDirs is false"):
    TestInputs.withTmpDir("sloth-test-"): root =>
      val logger   = TestLogger()
      val classDir = root / "classes"
      val txtFile  = root / "readme.txt"
      os.makeDir.all(classDir)
      os.write(txtFile, "hello")
      val classPath = Seq(classDir, txtFile)
      val options   = optionsWithSloth(enabled = true)
      val result    = SlothPatcher.transformClassPath(
        classPath,
        options,
        logger,
        patchProjectClassDirs = false,
        projectClassDirs = Set(classDir)
      )
      assert(result.isRight)
      assert(result.toOption.get == classPath)

  test("transformClassPath transforms directories to jars when patchProjectClassDirs is true"):
    TestInputs.withTmpDir("sloth-test-"): root =>
      val logger   = TestLogger()
      val classDir = root / "classes"
      os.makeDir.all(classDir)
      writeRealClassFile(classDir)
      os.write(classDir / "resource.txt", "test content")
      os.write(classDir / "sub" / "nested.txt", "nested content", createFolders = true)
      val classPath = Seq(classDir)
      val options   = optionsWithSloth(enabled = true)
      val result    = SlothPatcher.transformClassPath(
        classPath,
        options,
        logger,
        patchProjectClassDirs = true,
        projectClassDirs = Set(classDir)
      )
      assert(result.isRight)
      val transformed = result.toOption.get
      assert(transformed.size == 1)
      val patchedPath = transformed.head
      assert(patchedPath.ext == "jar", s"Expected jar, got: $patchedPath")
      assert(
        patchedPath.toString.contains("sloth"),
        s"Expected sloth cache path, got: $patchedPath"
      )
      val zf      = ZipFile(patchedPath.toIO)
      val entries = zf.entries().asScala.map(_.getName).toSet
      zf.close()
      assert(entries.contains("resource.txt"), s"Missing resource.txt in $entries")
      assert(entries.contains("sub/nested.txt"), s"Missing sub/nested.txt in $entries")

  test("transformClassPath preserves user META-INF/MANIFEST.MF in class dirs"):
    TestInputs.withTmpDir("sloth-manifest-test-"): root =>
      val logger   = TestLogger()
      val classDir = root / "classes"
      os.makeDir.all(classDir / "META-INF")
      os.write(classDir / "resource.txt", "test content")
      os.write(
        classDir / "META-INF" / "MANIFEST.MF",
        """Manifest-Version: 1.0
          |X-Custom: yes
          |""".stripMargin
      )
      writeRealClassFile(classDir)
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.transformClassPath(
        Seq(classDir),
        options,
        logger,
        patchProjectClassDirs = true,
        projectClassDirs = Set(classDir)
      )
      assert(result.isRight, s"Expected Right, got: $result")
      val patchedPath = result.toOption.get.head
      assert(patchedPath.ext == "jar", s"Expected jar, got: $patchedPath")
      Using.resource(ZipFile(patchedPath.toIO)): zf =>
        val entries = zf.entries().asScala.map(_.getName).toSeq
        assert(
          entries.count(_ == "META-INF/MANIFEST.MF") == 1,
          s"Expected exactly one META-INF/MANIFEST.MF, got: $entries"
        )
        assert(entries.contains("resource.txt"), s"Missing resource.txt in $entries")
        val manifestEntry = zf.getEntry("META-INF/MANIFEST.MF")
        val manifest      = JarManifest(zf.getInputStream(manifestEntry))
        assert(
          manifest.getMainAttributes.getValue("X-Custom") == "yes",
          s"Expected X-Custom=yes in manifest"
        )

  test("transformClassPath patches external class dirs into cached directory copies"):
    TestInputs.withTmpDir("sloth-external-dir-"): root =>
      val logger      = TestLogger()
      val externalDir = root / "cp"
      val classFile   = writeRealClassFile(externalDir)
      val before      = os.read.bytes(classFile)
      val options     = optionsWithSloth(enabled = true)
      val result      = SlothPatcher.transformClassPath(
        Seq(externalDir),
        options,
        logger,
        patchProjectClassDirs = false,
        projectClassDirs = Set.empty
      )
      assert(result.isRight, s"Expected Right, got: $result")
      val transformed = result.toOption.get
      assert(transformed.size == 1)
      val patched = transformed.head
      // Either left as-is (nothing to patch) or a cached directory under external-dirs
      assert(os.isDir(patched), s"Expected directory, got: $patched")
      assert(
        java.util.Arrays.equals(os.read.bytes(classFile), before),
        s"Expected original $classFile to be left untouched"
      )
      if patched != externalDir then
        assert(
          patched.toString.contains("external-dirs"),
          s"Expected external-dirs cache path, got: $patched"
        )

  test("transformClassPath skips resource directories without class files"):
    TestInputs.withTmpDir("sloth-resources-"): root =>
      val logger      = TestLogger()
      val resourceDir = root / "resources"
      os.write(resourceDir / "reference.conf", "answer = 42", createFolders = true)
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.transformClassPath(
        Seq(resourceDir),
        options,
        logger,
        patchProjectClassDirs = true,
        projectClassDirs = Set(resourceDir)
      )
      assert(result.isRight)
      assert(result.toOption.get == Seq(resourceDir))

  test("patchClassPathDirsInPlace leaves non-project directories alone"):
    TestInputs.withTmpDir("sloth-inplace-external-"): root =>
      val logger       = TestLogger()
      val projectDir   = root / "project"
      val externalDir  = root / "external"
      val projectFile  = writeRealClassFile(projectDir)
      val externalFile = writeRealClassFile(externalDir)
      val before       = os.read.bytes(externalFile)
      val classPath    = Seq(projectDir, externalDir)
      val result       = SlothPatcher.patchClassPathDirsInPlace(
        classPath,
        optionsWithSloth(enabled = true),
        logger,
        shouldPatch = true,
        projectClassDirs = Set(projectDir)
      )
      assert(result.isRight, s"Expected Right, got: $result")
      assert(result.toOption.get == classPath)
      assert(SlothPatcher.wasPatchedInThisProcess(projectDir))
      assert(!SlothPatcher.wasPatchedInThisProcess(externalDir))
      assert(java.util.Arrays.equals(os.read.bytes(externalFile), before))
      assert(os.isFile(projectFile))

  test("classPathEntryAsJar jars directories and patches when sloth is enabled"):
    TestInputs.withTmpDir("sloth-as-jar-"): root =>
      val logger = TestLogger()
      val dir    = root / "cp"
      writeRealClassFile(dir)
      val result = SlothPatcher.classPathEntryAsJar(dir, optionsWithSloth(enabled = true), logger)
      assert(result.isRight, s"Expected Right, got: $result")
      val jar = result.toOption.get
      assert(os.isFile(jar) && jar.ext == "jar", s"Expected jar file, got: $jar")
      assert(jar.last == "cp.jar", s"Expected stable name cp.jar, got: ${jar.last}")
      assert(
        !jar.last.startsWith("sloth-cp-entry-"),
        s"Expected content-addressed path, not temp name: $jar"
      )
      val again = SlothPatcher.classPathEntryAsJar(dir, optionsWithSloth(enabled = true), logger)
      assert(again.isRight, s"Expected Right, got: $again")
      assert(again.toOption.get == jar, s"Expected same cached path across calls, got: $again")

  test("classPathEntryAsJar jars directories without sloth"):
    TestInputs.withTmpDir("sloth-as-jar-off-"): root =>
      val logger = TestLogger()
      val dir    = root / "cp"
      writeRealClassFile(dir)
      val result = SlothPatcher.classPathEntryAsJar(dir, optionsWithSloth(enabled = false), logger)
      assert(result.isRight, s"Expected Right, got: $result")
      val jar = result.toOption.get
      assert(os.isFile(jar) && jar.ext == "jar", s"Expected jar file, got: $jar")
      assert(jar.last == "cp.jar", s"Expected stable name cp.jar, got: ${jar.last}")
      assert(
        !jar.last.startsWith("sloth-cp-entry-"),
        s"Expected content-addressed path, not temp name: $jar"
      )
      val again =
        SlothPatcher.classPathEntryAsJar(dir, optionsWithSloth(enabled = false), logger)
      assert(again.isRight, s"Expected Right, got: $again")
      assert(again.toOption.get == jar, s"Expected same cached path across calls, got: $again")

  /** Bytecode of a class from the test class path, so patching operates on real input. */
  private def realClassFileBytes(className: String): Array[Byte] =
    val resourceName = className.replace('.', '/') + ".class"
    Using.resource(getClass.getClassLoader.getResourceAsStream(resourceName))(_.readAllBytes())

  private def writeRealClassFile(classDir: os.Path): os.Path =
    val className = "scala.build.postprocessing.SlothPatcher"
    val dest      = classDir / os.RelPath(className.replace('.', '/') + ".class")
    os.write(dest, realClassFileBytes(className), createFolders = true)
    dest

  test("patchClassPathDirsInPlace keeps class directories as directories"):
    TestInputs.withTmpDir("sloth-inplace-cp-"): root =>
      val logger    = TestLogger()
      val classDir  = root / "classes"
      val classFile = writeRealClassFile(classDir)
      val classPath = Seq(classDir)
      val result    = SlothPatcher.patchClassPathDirsInPlace(
        classPath,
        optionsWithSloth(enabled = true),
        logger,
        shouldPatch = true,
        projectClassDirs = Set(classDir)
      )
      assert(result.isRight, s"Expected Right, got: $result")
      assert(result.toOption.get == classPath, s"Expected $classPath, got: ${result.toOption.get}")
      assert(os.isDir(classDir), s"Expected $classDir to stay a directory")
      assert(os.isFile(classFile), s"Expected $classFile to stay in place")
      assert(
        SlothPatcher.wasPatchedInThisProcess(classDir),
        s"Expected $classDir to be registered as patched"
      )

  test("patchClassPathDirsInPlace leaves jars on the classpath untouched"):
    TestInputs.withTmpDir("sloth-inplace-cp-"): root =>
      val logger = TestLogger()
      val jar    = root / "lib.jar"
      createEmptyJar(jar)
      val jarBytesBefore = os.read.bytes(jar)
      val classPath      = Seq(jar)
      val result         = SlothPatcher.patchClassPathDirsInPlace(
        classPath,
        optionsWithSloth(enabled = true),
        logger,
        shouldPatch = true,
        projectClassDirs = Set.empty
      )
      assert(result.isRight, s"Expected Right, got: $result")
      assert(result.toOption.get == classPath, s"Expected $classPath, got: ${result.toOption.get}")
      assert(
        java.util.Arrays.equals(os.read.bytes(jar), jarBytesBefore),
        s"Expected $jar to be left untouched"
      )

  test("patchClassPathDirsInPlace skips directories without class files"):
    TestInputs.withTmpDir("sloth-inplace-cp-"): root =>
      val logger      = TestLogger()
      val resourceDir = root / "resources"
      os.write(resourceDir / "reference.conf", "answer = 42", createFolders = true)
      val classPath = Seq(resourceDir)
      val result    = SlothPatcher.patchClassPathDirsInPlace(
        classPath,
        optionsWithSloth(enabled = true),
        logger,
        shouldPatch = true,
        projectClassDirs = Set.empty
      )
      assert(result.isRight, s"Expected Right, got: $result")
      assert(result.toOption.get == classPath, s"Expected $classPath, got: ${result.toOption.get}")
      assert(
        !SlothPatcher.wasPatchedInThisProcess(resourceDir),
        s"Expected $resourceDir to be skipped"
      )

  test("patchClassPathDirsInPlace is a no-op when sloth is disabled"):
    TestInputs.withTmpDir("sloth-inplace-cp-"): root =>
      val logger   = TestLogger()
      val classDir = root / "classes"
      writeRealClassFile(classDir)
      val classPath = Seq(classDir)
      val result    = SlothPatcher.patchClassPathDirsInPlace(
        classPath,
        optionsWithSloth(enabled = false),
        logger,
        shouldPatch = true,
        projectClassDirs = Set(classDir)
      )
      assert(result.isRight, s"Expected Right, got: $result")
      assert(result.toOption.get == classPath, s"Expected $classPath, got: ${result.toOption.get}")
      assert(
        !SlothPatcher.wasPatchedInThisProcess(classDir),
        s"Expected $classDir to be left alone when sloth is disabled"
      )

  test("patchClassPathDirsInPlace is a no-op when shouldPatch is false"):
    TestInputs.withTmpDir("sloth-inplace-cp-"): root =>
      val logger   = TestLogger()
      val classDir = root / "classes"
      writeRealClassFile(classDir)
      val classPath = Seq(classDir)
      val result    = SlothPatcher.patchClassPathDirsInPlace(
        classPath,
        optionsWithSloth(enabled = true),
        logger,
        shouldPatch = false,
        projectClassDirs = Set(classDir)
      )
      assert(result.isRight, s"Expected Right, got: $result")
      assert(result.toOption.get == classPath, s"Expected $classPath, got: ${result.toOption.get}")
      assert(
        !SlothPatcher.wasPatchedInThisProcess(classDir),
        s"Expected $classDir to be left alone when shouldPatch is false"
      )

  test("shouldPatchProjectClasses returns false for pure Java project"):
    val result = SlothPatcher.shouldPatchProjectClasses(
      hasJava = true,
      hasScala = false,
      scalaVersions = Nil
    )
    assert(!result, "Pure Java project should not patch class dirs")

  test("shouldPatchProjectClasses returns true for Scala < 3.8"):
    val result = SlothPatcher.shouldPatchProjectClasses(
      hasJava = false,
      hasScala = true,
      scalaVersions = Seq("3.3.8")
    )
    assert(result, "Scala < 3.8 project should patch class dirs")

  test("shouldPatchProjectClasses returns false for Scala >= 3.8"):
    val result = SlothPatcher.shouldPatchProjectClasses(
      hasJava = false,
      hasScala = true,
      scalaVersions = Seq("3.8.0")
    )
    assert(!result, "Scala >= 3.8 project should not patch class dirs")

  test("shouldPatchProjectClasses returns true for mixed Java+Scala < 3.8"):
    val result = SlothPatcher.shouldPatchProjectClasses(
      hasJava = true,
      hasScala = true,
      scalaVersions = Seq("3.3.8")
    )
    assert(result, "Mixed Java+Scala < 3.8 project should patch class dirs")

  test("shouldPatchProjectClasses returns false for no sources and no versions"):
    val result = SlothPatcher.shouldPatchProjectClasses(
      hasJava = false,
      hasScala = false,
      scalaVersions = Nil
    )
    assert(!result, "No sources/versions should not patch class dirs")

  test("captureStdio restores System.out/err under concurrent access"):
    val threadCount = 16
    val iterations  = 200
    val originalOut = System.out
    val originalErr = System.err
    val executor    = Executors.newFixedThreadPool(threadCount)
    val barrier     = CyclicBarrier(threadCount)
    val logger      = TestLogger()

    try
      val futures = (1 to threadCount).map: _ =>
        val task: Callable[Unit] = () =>
          barrier.await()
          for _ <- 1 to iterations do
            SlothPatcher.captureStdio(logger):
              Thread.`yield`()
              42
        executor.submit(task)

      futures.foreach(_.get())

      assert(
        System.out eq originalOut,
        s"System.out was corrupted: expected original stream but got ${System.out}"
      )
      assert(
        System.err eq originalErr,
        s"System.err was corrupted: expected original stream but got ${System.err}"
      )
    finally
      executor.shutdown()
      System.setOut(originalOut)
      System.setErr(originalErr)

  // --- Signature detection and stripping tests ---

  private def createJarWithSignatureFiles(jarPath: os.Path, signatureFiles: Seq[String]): Unit =
    val manifest = JarManifest()
    manifest.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")
    Using.resource(JarOutputStream(os.write.outputStream(jarPath), manifest)): jos =>
      // Add a regular class file
      val classEntry = ZipEntry("com/example/Test.class")
      jos.putNextEntry(classEntry)
      jos.write(Array[Byte](0xca.toByte, 0xfe.toByte, 0xba.toByte, 0xbe.toByte))
      jos.closeEntry()
      // Add signature files
      signatureFiles.foreach: name =>
        val entry = ZipEntry(name)
        jos.putNextEntry(entry)
        jos.write(s"content of $name".getBytes)
        jos.closeEntry()

  test("signatureEntryNames detects META-INF signature files"):
    TestInputs.withTmpDir("sloth-sig-test-"): root =>
      val jar = root / "test.jar"
      createJarWithSignatureFiles(
        jar,
        Seq(
          "META-INF/TEST.SF",
          "META-INF/TEST.DSA",
          "META-INF/TEST.RSA",
          "META-INF/TEST.EC",
          "META-INF/SIG-FOO",
          "META-INF/services/java.sql.Driver", // should NOT be detected
          "META-INF/sub/nested.SF"             // should NOT be detected (not direct child)
        )
      )
      val sigFiles = SlothPatcher.signatureEntryNames(jar)
      assert(sigFiles.contains("META-INF/TEST.SF"), s"Missing TEST.SF in $sigFiles")
      assert(sigFiles.contains("META-INF/TEST.DSA"), s"Missing TEST.DSA in $sigFiles")
      assert(sigFiles.contains("META-INF/TEST.RSA"), s"Missing TEST.RSA in $sigFiles")
      assert(sigFiles.contains("META-INF/TEST.EC"), s"Missing TEST.EC in $sigFiles")
      assert(sigFiles.contains("META-INF/SIG-FOO"), s"Missing SIG-FOO in $sigFiles")
      assert(
        !sigFiles.contains("META-INF/services/java.sql.Driver"),
        s"Should not include services file"
      )
      assert(!sigFiles.contains("META-INF/sub/nested.SF"), s"Should not include nested SF file")
      assert(sigFiles.size == 5, s"Expected 5 signature files, got ${sigFiles.size}: $sigFiles")

  test("signatureEntryNames is case-insensitive for extensions"):
    TestInputs.withTmpDir("sloth-sig-test-"): root =>
      val jar = root / "test.jar"
      createJarWithSignatureFiles(
        jar,
        Seq(
          "META-INF/TEST.sf",
          "META-INF/OTHER.Dsa",
          "META-INF/ANOTHER.rSa"
        )
      )
      val sigFiles = SlothPatcher.signatureEntryNames(jar)
      assert(sigFiles.size == 3, s"Expected 3 signature files, got ${sigFiles.size}: $sigFiles")

  test("stripSignatures removes signature entries and digest attributes"):
    TestInputs.withTmpDir("sloth-strip-test-"): root =>
      val jar = root / "signed.jar"
      // Create a jar with signature files and digest attributes in manifest
      val manifest = JarManifest()
      manifest.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")
      manifest.getMainAttributes.putValue("Created-By", "Test")
      // Add per-entry digest attribute (simulating a signed jar)
      val entryAttrs = JarAttributes()
      entryAttrs.putValue("SHA-256-Digest", "abc123...")
      manifest.getEntries.put("com/example/Test.class", entryAttrs)

      Using.resource(JarOutputStream(os.write.outputStream(jar), manifest)): jos =>
        val classEntry = ZipEntry("com/example/Test.class")
        jos.putNextEntry(classEntry)
        jos.write(Array[Byte](0xca.toByte, 0xfe.toByte, 0xba.toByte, 0xbe.toByte))
        jos.closeEntry()
        // Add signature files
        Seq("META-INF/TEST.SF", "META-INF/TEST.DSA").foreach: name =>
          val entry = ZipEntry(name)
          jos.putNextEntry(entry)
          jos.write(s"content of $name".getBytes)
          jos.closeEntry()

      val stripped = root / "stripped.jar"
      SlothPatcher.stripSignatures(jar, stripped)

      // Verify signature files are removed
      val zf      = ZipFile(stripped.toIO)
      val entries = zf.entries().asScala.map(_.getName).toSet
      zf.close()
      assert(!entries.contains("META-INF/TEST.SF"), "SF file should be removed")
      assert(!entries.contains("META-INF/TEST.DSA"), "DSA file should be removed")
      assert(entries.contains("com/example/Test.class"), "Class file should be preserved")
      assert(entries.contains("META-INF/MANIFEST.MF"), "Manifest should be preserved")

      // Verify digest attributes are removed from manifest
      Using.resource(ZipFile(stripped.toIO)): zf =>
        val manifestEntry   = zf.getEntry("META-INF/MANIFEST.MF")
        val manifestContent = new String(zf.getInputStream(manifestEntry).readAllBytes())
        assert(
          !manifestContent.contains("SHA-256-Digest"),
          s"Digest should be removed from manifest: $manifestContent"
        )
        assert(manifestContent.contains("Created-By"), "Non-digest attributes should be preserved")

  test("isSigned returns true for jars with signature files"):
    TestInputs.withTmpDir("sloth-signed-test-"): root =>
      val signedJar = root / "signed.jar"
      createJarWithSignatureFiles(signedJar, Seq("META-INF/TEST.SF", "META-INF/TEST.DSA"))
      assert(SlothPatcher.isSigned(signedJar), "Jar with .SF/.DSA should be detected as signed")

      val unsignedJar = root / "unsigned.jar"
      createJarWithSignatureFiles(unsignedJar, Seq.empty)
      assert(
        !SlothPatcher.isSigned(unsignedJar),
        "Jar without signature files should not be detected as signed"
      )

  test("WarningMessages.slothStrippedJarSignatures mentions jar path"):
    val jarPath = os.pwd / "some" / "path" / "signed.jar"
    val message = WarningMessages.slothStrippedJarSignatures(jarPath)
    assert(message.contains(jarPath.toString), s"Message should contain jar path: $message")
    assert(message.contains("signature"), s"Message should mention signature: $message")

  // --- Corrupt / malformed archive degradation ---

  /** Truncated JAR that still passes zipStartOffset (plausible PK header + EOCD). */
  private def createCorruptJarWithPlausibleEocd(dest: os.Path): Unit =
    val valid = dest / os.up / s"${dest.last}.valid"
    createEmptyJar(valid)
    val bytes = os.read.bytes(valid)
    // Keep local-file header bytes and the trailing EOCD (22 bytes) so the sniff
    // succeeds, but drop the middle so ZipFile / central directory reads fail.
    os.write.over(dest, bytes.take(40) ++ bytes.takeRight(22), createFolders = true)
    os.remove(valid)

  test("patchJarFile degrades gracefully on corrupt archive"):
    TestInputs.withTmpDir("sloth-corrupt-jar-"): root =>
      val logger     = RecordingLogger()
      val corruptJar = root / "corrupt.jar"
      createCorruptJarWithPlausibleEocd(corruptJar)
      assert(
        SlothPatcher.zipStartOffset(corruptJar).contains(0L),
        s"Corrupt fixture must pass zipStartOffset sniff, got ${SlothPatcher.zipStartOffset(corruptJar)}"
      )
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.patchJarFile(corruptJar, options, logger)
      assert(result.isRight, s"Expected Right, got: $result")
      assert(
        result.toOption.get == corruptJar,
        s"Expected original path, got: ${result.toOption.get}"
      )
      assert(
        logger.messages.exists(_.contains("using original")),
        s"Expected 'using original' warning, got: ${logger.messages}"
      )
      assert(
        logger.messages.exists(_.startsWith(warnPrefix)),
        s"Expected warnPrefix on warning, got: ${logger.messages}"
      )

  test("transformClassPath degrades gracefully on corrupt classpath entry"):
    TestInputs.withTmpDir("sloth-corrupt-cp-"): root =>
      val logger     = RecordingLogger()
      val goodJar    = root / "good.jar"
      val corruptJar = root / "corrupt.jar"
      createEmptyJar(goodJar)
      createCorruptJarWithPlausibleEocd(corruptJar)
      assert(SlothPatcher.zipStartOffset(corruptJar).contains(0L))
      val options   = optionsWithSloth(enabled = true)
      val classPath = Seq(goodJar, corruptJar)
      val result    = SlothPatcher.transformClassPath(
        classPath,
        options,
        logger,
        patchProjectClassDirs = false,
        projectClassDirs = Set.empty
      )
      assert(result.isRight, s"Expected Right, got: $result")
      val transformed = result.toOption.get
      assert(transformed.size == 2, s"Expected 2 entries, got: $transformed")
      assert(
        transformed(1) == corruptJar,
        s"Corrupt entry should be left unchanged, got: ${transformed(1)}"
      )
      assert(
        logger.messages.exists(_.contains("using original")),
        s"Expected 'using original' warning, got: ${logger.messages}"
      )
      assert(
        logger.messages.exists(_.startsWith(warnPrefix)),
        s"Expected warnPrefix on warning, got: ${logger.messages}"
      )

  test("patchByteCodeZipEntries degrades gracefully on unwritable entries"):
    val logger  = RecordingLogger()
    val tmpRoot = os.Path(sys.props("java.io.tmpdir"))
    // Duplicate entry names make ZipOutputStream.putNextEntry throw ZipException
    val entries = Seq(
      (ZipEntry("Dup.class"), Array[Byte](1, 2, 3)),
      (ZipEntry("Dup.class"), Array[Byte](4, 5, 6))
    )
    val options = optionsWithSloth(enabled = true)
    val before  = os.list(tmpRoot).toSet
    val result  = SlothPatcher.patchByteCodeZipEntries(entries, options, logger)
    assert(result.isRight, s"Expected Right, got: $result")
    assert(
      result.toOption.get == entries,
      s"Expected original entries returned, got: ${result.toOption.get.map(_._1.getName)}"
    )
    assert(
      logger.messages.exists(_.contains("using original")),
      s"Expected 'using original' warning, got: ${logger.messages}"
    )
    assert(
      logger.messages.exists(_.startsWith(warnPrefix)),
      s"Expected warnPrefix on warning, got: ${logger.messages}"
    )
    val leaked = (os.list(tmpRoot).toSet -- before).filter { p =>
      p.last.startsWith("sloth-entries-") ||
      (os.isDir(p) && os.list(p).exists(_.last.startsWith("sloth-entries-")))
    }
    assert(leaked.isEmpty, s"Leaked temp entries: $leaked")

  // --- Hierarchy classpath resolution (native-image-safe getCommonSuperClass) ---

  /** Sources for a jar whose types are *not* on the test JVM classpath. lib-b holds Base/SubA/SubB;
    * lib-a holds a lazy val whose initializer merges SubA and SubB, forcing ASM COMPUTE_FRAMES to
    * call getCommonSuperClass(SubA, SubB) while patching.
    */
  private val hierarchyFixtureInputs: TestInputs = TestInputs(
    os.rel / "libb" / "Base.scala" ->
      """package sloth.hierarchy
        |
        |class Base
        |class SubA extends Base
        |class SubB extends Base
        |""".stripMargin,
    os.rel / "liba" / "Lazies.scala" ->
      """package sloth.hierarchy
        |
        |object Lazies:
        |  // Lazy val so Sloth rewrites the class (COMPUTE_FRAMES for *all* methods).
        |  lazy val x: Base =
        |    if sys.props.contains("sloth.hierarchy.useA") then new SubA else new SubB
        |  def get: Base = x
        |  // Non-lazy method returning Base with a SubA/SubB merge into a local (not
        |  // early-return). When ASM cannot resolve SubA/SubB, getCommonSuperClass
        |  // falls back to Object and this method VerifyErrors on areturn of Base.
        |  def pick(useA: Boolean): Base =
        |    val chosen: Base = if useA then new SubA else new SubB
        |    chosen
        |""".stripMargin
  )

  private lazy val scala33CompilerClasspath: Seq[os.Path] =
    val out = os.proc(
      "cs",
      "fetch",
      s"org.scala-lang:scala3-compiler_3:${Constants.scala3Lts}"
    ).call().out.trim()
    out.split(System.lineSeparator()).toSeq.filter(_.nonEmpty).map(os.Path(_))

  private def jarDirectory(dir: os.Path, dest: os.Path): Unit =
    val manifest = JarManifest()
    manifest.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")
    Using.resource(JarOutputStream(os.write.outputStream(dest), manifest)): jos =>
      for
        path <- os.walk(dir)
        if os.isFile(path)
      do
        val relativePath = path.relativeTo(dir).toString.replace('\\', '/')
        val entry        = ZipEntry(relativePath)
        entry.setLastModifiedTime(FileTime.fromMillis(os.mtime(path)))
        val content = os.read.bytes(path)
        entry.setSize(content.length)
        jos.putNextEntry(entry)
        jos.write(content)
        jos.closeEntry()

  /** Compile `srcDir` with Scala 3 LTS (pre-3.8 lazy vals) against `extraCp`, jar into `destJar`.
    */
  private def compileScala33ToJar(
    root: os.Path,
    srcDir: os.Path,
    destJar: os.Path,
    extraCp: Seq[os.Path] = Nil
  ): Unit =
    val outDir = root / s"${srcDir.last}-classes"
    os.makeDir.all(outDir)
    val sources = os.walk(srcDir).filter(p => p.ext == "scala" && os.isFile(p))
    assert(sources.nonEmpty, s"Expected sources under $srcDir")
    // Compiler boot CP and user -classpath both need scala3-library; extraCp is layered on top.
    val compilerCp = scala33CompilerClasspath.mkString(File.pathSeparator)
    val userCp     = (scala33CompilerClasspath ++ extraCp).mkString(File.pathSeparator)
    val res        = os.proc(
      "java",
      "-cp",
      compilerCp,
      "dotty.tools.dotc.Main",
      "-d",
      outDir.toString,
      "-classpath",
      userCp,
      sources.map(_.toString)
    ).call(cwd = root, check = false, mergeErrIntoOut = true)
    assert(
      res.exitCode == 0,
      s"Failed to compile $srcDir with Scala ${Constants.scala3Lts}:\n${res.out.text()}"
    )
    jarDirectory(outDir, destJar)

  /** Build lib-a.jar (lazy vals) and lib-b.jar (hierarchy types) under `root`. */
  private def buildHierarchyFixtureJars(root: os.Path): (os.Path, os.Path) =
    val libB = root / "lib-b.jar"
    val libA = root / "lib-a.jar"
    compileScala33ToJar(root, root / "libb", libB)
    compileScala33ToJar(root, root / "liba", libA, extraCp = Seq(libB))
    (libA, libB)

  /** Load `sloth.hierarchy.Lazies$` from the given jars and force lazy-val initialization. */
  private def forceInitLazies(jars: Seq[os.Path]): Unit =
    // scala3-library (and scala-library) come from the compiler fetch; platform CL has neither.
    val scalaLibs = scala33CompilerClasspath.filter { p =>
      val n = p.last
      n.startsWith("scala3-library") || n.startsWith("scala-library")
    }
    val urls = (jars ++ scalaLibs).map(_.toNIO.toUri.toURL).toArray
    Using.resource(URLClassLoader(urls, ClassLoader.getPlatformClassLoader)): cl =>
      // Class.forName verifies all methods; pick()'s SubA/SubB merge is the VerifyError trigger
      // when those types were invisible to Sloth's COMPUTE_FRAMES.
      val cls    = Class.forName("sloth.hierarchy.Lazies$", true, cl)
      val module = cls.getField("MODULE$").get(null)
      cls.getMethod("get").invoke(module)
      cls.getMethod("pick", classOf[Boolean]).invoke(module, java.lang.Boolean.FALSE)
      ()

  test("transformClassPath keeps hierarchy frames valid across jars"):
    hierarchyFixtureInputs.fromRoot: root =>
      val (libA, libB) = buildHierarchyFixtureJars(root)
      // Precondition: unpatched jars load and initialize fine.
      forceInitLazies(Seq(libA, libB))

      val logger  = TestLogger()
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.transformClassPath(
        Seq(libA, libB),
        options,
        logger,
        patchProjectClassDirs = false,
        projectClassDirs = Set.empty
      )
      assert(result.isRight, s"Expected Right, got: $result")
      val transformed = result.toOption.get
      assert(transformed.size == 2, s"Expected 2 entries, got: $transformed")
      val patchedA = transformed.head
      assert(
        patchedA != libA,
        s"Expected lib-a.jar to be rewritten (it has lazy vals), got unchanged: $patchedA"
      )
      // Loading the patched lib-a against lib-b must not throw VerifyError. Today JarProcessor
      // only sees lib-a, so getCommonSuperClass(SubA, SubB) falls back to Object and this fails.
      forceInitLazies(Seq(patchedA, libB))

  test("dependency jar without hierarchy warns and still patches"):
    hierarchyFixtureInputs.fromRoot: root =>
      val (libA, _) = buildHierarchyFixtureJars(root)
      val logger    = RecordingLogger()
      val options   = optionsWithSloth(enabled = true)
      // Dependency source, hierarchy classpath omits lib-b so SubA/SubB are unresolvable.
      val result = SlothPatcher.patchJarFile(
        libA,
        options,
        logger,
        hierarchyClassPath = Nil,
        source = SlothPatcher.SlothSource.Dependency
      )
      assert(result.isRight, s"Expected Right, got: $result")
      val patched = result.toOption.get
      assert(patched != libA, s"Expected lib-a to be rewritten, got unchanged: $patched")
      assert(
        logger.messages.exists(_.contains("could not resolve class hierarchies")),
        s"Expected hierarchy warning, got: ${logger.messages}"
      )
      assert(
        logger.messages.exists(_.contains("--sloth-strict")),
        s"Expected --sloth-strict hint, got: ${logger.messages}"
      )

  test("dependency jar without hierarchy fails under slothStrict"):
    hierarchyFixtureInputs.fromRoot: root =>
      val (libA, _) = buildHierarchyFixtureJars(root)
      val logger    = RecordingLogger()
      val options   = optionsWithSloth(enabled = true, strict = true)
      val result    = SlothPatcher.patchJarFile(
        libA,
        options,
        logger,
        hierarchyClassPath = Nil,
        source = SlothPatcher.SlothSource.Dependency
      )
      assert(result.isLeft, s"Expected Left under slothStrict, got: $result")
      val err = result.swap.toOption.get.getMessage
      assert(
        err.contains("could not resolve class hierarchies") || err.contains("Cannot resolve class"),
        s"Expected hierarchy error message, got: $err"
      )

  test("project bytecode without hierarchy is left unpatched"):
    hierarchyFixtureInputs.fromRoot: root =>
      val (libA, _) = buildHierarchyFixtureJars(root)
      val logger    = RecordingLogger()
      val options   = optionsWithSloth(enabled = true)
      val result    = SlothPatcher.patchJarFile(
        libA,
        options,
        logger,
        hierarchyClassPath = Nil,
        source = SlothPatcher.SlothSource.Project
      )
      assert(result.isRight, s"Expected Right, got: $result")
      val patched = result.toOption.get
      assert(
        patched == libA,
        s"Expected project jar to stay unpatched when hierarchy is incomplete, got: $patched"
      )
      assert(
        logger.messages.exists(m =>
          m.contains("could not resolve class hierarchies") || m.contains("using original")
        ),
        s"Expected hierarchy / using-original warning, got: ${logger.messages}"
      )

  test("transformClassPath keeps ZipFile frames in scala3-compiler openZipFile"):
    // FileZipArchive.openZipFile merges JarFile and ZipFile; a broken JDK Class.forName path
    // under native image collapses that merge to Object and VerifyErrors. Assert the patched
    // frames stay ZipFile when hierarchy includes scala3-library.
    TestInputs.withTmpDir("sloth-compiler-frames-"): _ =>
      val compilerJar = scala33CompilerClasspath
        .find(_.last.startsWith("scala3-compiler"))
        .getOrElse(sys.error("scala3-compiler jar not found on compiler classpath"))
      val logger  = TestLogger()
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.transformClassPath(
        scala33CompilerClasspath,
        options,
        logger,
        patchProjectClassDirs = false,
        projectClassDirs = Set.empty
      )
      assert(result.isRight, s"Expected Right, got: $result")
      val patchedCompiler = result.toOption.get
        .find(_.last.startsWith("scala3-compiler"))
        .getOrElse(sys.error(s"No scala3-compiler in ${result.toOption.get}"))
      assert(
        patchedCompiler != compilerJar,
        s"Expected scala3-compiler to be rewritten, got unchanged: $patchedCompiler"
      )
      val openZipFrames = stackMapTypesAtMerge(
        patchedCompiler,
        "dotty/tools/io/FileZipArchive.class",
        "openZipFile"
      )
      assert(
        openZipFrames.exists(_.contains("java/util/zip/ZipFile")),
        s"Expected ZipFile on openZipFile stack map frames, got: $openZipFrames"
      )
      assert(
        !openZipFrames.exists(f => f.contains("java/lang/Object") && !f.contains("ZipFile")),
        s"openZipFile frames must not collapse JarFile/ZipFile merge to Object: $openZipFrames"
      )

  /** Stack-map stack types from frames of `methodNameSubstring` inside a class entry of `jar`. */
  private def stackMapTypesAtMerge(
    jar: os.Path,
    classEntry: String,
    methodNameSubstring: String
  ): Seq[String] =
    import org.objectweb.asm.ClassReader
    import org.objectweb.asm.tree.{ClassNode, FrameNode, LabelNode}
    Using.resource(ZipFile(jar.toIO)): zf =>
      val entry = zf.getEntry(classEntry)
      assert(entry != null, s"Missing $classEntry in $jar")
      val bytes = Using.resource(zf.getInputStream(entry))(_.readAllBytes())
      val cn    = ClassNode()
      ClassReader(bytes).accept(cn, 0)
      val method = cn.methods.asScala
        .find(_.name.contains(methodNameSubstring))
        .getOrElse(sys.error(s"No method *$methodNameSubstring* in $classEntry"))
      method.instructions.toArray.toSeq.collect {
        case f: FrameNode =>
          Option(f.stack).map(_.asScala.map {
            case null         => "null"
            case s: String    => s
            case n: Integer   => s"int($n)"
            case _: LabelNode => "label"
            case other        => String.valueOf(other)
          }.mkString(",")).getOrElse("-")
      }
