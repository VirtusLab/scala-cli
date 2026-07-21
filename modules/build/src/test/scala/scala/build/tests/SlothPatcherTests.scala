package scala.build.tests

import java.util.zip.{ZipEntry, ZipFile}

import scala.build.options.{BuildOptions, PostBuildOptions}
import scala.build.postprocessing.SlothPatcher
import scala.jdk.CollectionConverters.*

class SlothPatcherTests extends TestUtil.ScalaCliBuildSuite:

  private def optionsWithSloth(enabled: Boolean): BuildOptions =
    BuildOptions(notForBloopOptions = PostBuildOptions(slothOpt = Some(enabled)))

  test("transformClassPath returns unchanged when sloth disabled"):
    val logger    = TestLogger()
    val classPath = Seq(os.pwd / "a.jar", os.pwd / "b.jar")
    val options   = optionsWithSloth(enabled = false)
    val result    = SlothPatcher.transformClassPath(classPath, options, logger)
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

  test("patchJarFile passes through non-jar files even when sloth enabled"):
    TestInputs.withTmpDir("sloth-test-"): root =>
      val logger   = TestLogger()
      val classDir = root / "classes"
      os.makeDir.all(classDir)
      val options = optionsWithSloth(enabled = true)
      val result  = SlothPatcher.patchJarFile(classDir, options, logger)
      assert(result.isRight)
      assert(result.toOption.get == classDir)

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
        patchProjectClassDirs = false
      )
      assert(result.isRight)
      assert(result.toOption.get == classPath)

  test("transformClassPath transforms directories to jars when patchProjectClassDirs is true"):
    TestInputs.withTmpDir("sloth-test-"): root =>
      val logger   = TestLogger()
      val classDir = root / "classes"
      os.makeDir.all(classDir)
      os.write(classDir / "resource.txt", "test content")
      os.write(classDir / "sub" / "nested.txt", "nested content", createFolders = true)
      val classPath = Seq(classDir)
      val options   = optionsWithSloth(enabled = true)
      val result    = SlothPatcher.transformClassPath(
        classPath,
        options,
        logger,
        patchProjectClassDirs = true
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
