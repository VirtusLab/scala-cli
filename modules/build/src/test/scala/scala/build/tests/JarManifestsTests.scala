package scala.build.tests

import java.io.ByteArrayInputStream
import java.util.jar.{Attributes as JarAttributes, Manifest as JarManifest}

import scala.build.internal.JarManifests

class JarManifestsTests extends TestUtil.ScalaCliBuildSuite:

  test("isManifestEntry matches META-INF/MANIFEST.MF exactly"):
    assert(JarManifests.isManifestEntry("META-INF/MANIFEST.MF"))
    assert(!JarManifests.isManifestEntry("META-INF/MANIFEST.MF.bak"))
    assert(!JarManifests.isManifestEntry("com/example/Foo.class"))
    assert(!JarManifests.isManifestEntry("META-INF/services/java.sql.Driver"))

  test("userManifestIn returns None when directory has no manifest"):
    TestInputs.withTmpDir("jar-manifests-"): root =>
      val dir = root / "classes"
      os.makeDir.all(dir)
      os.write(dir / "Foo.class", Array[Byte](1, 2, 3))
      assert(JarManifests.userManifestIn(dir).isEmpty)

  test("userManifestIn reads a user-supplied manifest"):
    TestInputs.withTmpDir("jar-manifests-"): root =>
      val dir = root / "classes"
      os.makeDir.all(dir / "META-INF")
      os.write(
        dir / "META-INF" / "MANIFEST.MF",
        """Manifest-Version: 1.0
          |X-Custom: yes
          |""".stripMargin
      )
      val manifest = JarManifests.userManifestIn(dir).getOrElse:
        throw new AssertionError("Expected a user manifest")
      assert(manifest.getMainAttributes.getValue("X-Custom") == "yes")

  test("merged uses user manifest as base and overlays CLI attributes"):
    val base = JarManifest()
    base.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")
    base.getMainAttributes.putValue("X-Custom", "yes")
    base.getMainAttributes.put(JarAttributes.Name.MAIN_CLASS, "UserMain")

    val merged = JarManifests.merged(
      Some(base),
      Seq(JarAttributes.Name.MAIN_CLASS -> "CliMain")
    )
    assert(merged.getMainAttributes.getValue("X-Custom") == "yes")
    assert(merged.getMainAttributes.getValue(JarAttributes.Name.MAIN_CLASS) == "CliMain")
    assert(merged.getMainAttributes.getValue(JarAttributes.Name.MANIFEST_VERSION) == "1.0")

  test("merged works with no base manifest"):
    val merged = JarManifests.merged(
      None,
      Seq(JarAttributes.Name.MAIN_CLASS -> "CliMain")
    )
    assert(merged.getMainAttributes.getValue(JarAttributes.Name.MANIFEST_VERSION) == "1.0")
    assert(merged.getMainAttributes.getValue(JarAttributes.Name.MAIN_CLASS) == "CliMain")

  test("bytes round-trips a manifest"):
    val original = JarManifests.merged(
      None,
      Seq(JarAttributes.Name.MAIN_CLASS -> "Hello")
    )
    original.getMainAttributes.putValue("X-Custom", "yes")
    val parsed = JarManifest(ByteArrayInputStream(JarManifests.bytes(original)))
    assert(parsed.getMainAttributes.getValue(JarAttributes.Name.MAIN_CLASS) == "Hello")
    assert(parsed.getMainAttributes.getValue("X-Custom") == "yes")
