package scala.build.tests

import java.util.zip.ZipEntry

import scala.build.options.{BuildOptions, PostBuildOptions}
import scala.build.postprocessing.SlothPatcher

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

  test("transformClassPath passes through non-jar entries even when sloth enabled"):
    TestInputs.withTmpDir("sloth-test-"): root =>
      val logger   = TestLogger()
      val classDir = root / "classes"
      val txtFile  = root / "readme.txt"
      os.makeDir.all(classDir)
      os.write(txtFile, "hello")
      val classPath = Seq(classDir, txtFile)
      val options   = optionsWithSloth(enabled = true)
      val result    = SlothPatcher.transformClassPath(classPath, options, logger)
      assert(result.isRight)
      assert(result.toOption.get == classPath)
