package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait RunLtsTagTestDefinitions { this: RunTestDefinitions =>
  protected val currentltsPrefix: String = Constants.scala3LtsPrefix
  protected val currentLts: String       = Constants.scala3Lts
  protected val legacyLtsPrefix: String  = Constants.scala3LegacyLtsPrefix
  protected val legacyLts: String        = Constants.scala3LegacyLts

  private val nonLtsPrefix: String = "3.8"

  for {
    (tag, expectedPrefix, expectedVersion) <- List(
      ("lts", currentltsPrefix, currentLts),
      ("3.lts", currentltsPrefix, currentLts),
      (s"$currentltsPrefix.lts", currentltsPrefix, currentLts),
      (s"$legacyLtsPrefix.lts", legacyLtsPrefix, legacyLts)
    ).distinct
  }
    test(s"Scala $tag points to the latest stable $expectedPrefix version") {
      TestInputs.empty.fromRoot { root =>
        val version = getScalaVersion(tag, root)
        expect(version.startsWith(s"$expectedPrefix."))
        expect(version == expectedVersion)
        expect(!version.exists(_.isLetter))
      }
    }

  for {
    (tag, expectedPrefix) <- List(
      "lts.rc"                   -> currentltsPrefix,
      "3.lts.rc"                 -> currentltsPrefix,
      s"$legacyLtsPrefix.lts.rc" -> legacyLtsPrefix
    ).distinct
  }
    test(s"Scala $tag points to the latest $expectedPrefix RC") {
      TestInputs.empty.fromRoot { root =>
        val version = getScalaVersion(tag, root)
        expect(version.startsWith(s"$expectedPrefix."))
        expect(version.contains("-RC"))
        expect(!version.contains("NIGHTLY"))
        expect(!version.contains("SNAPSHOT"))
      }
    }

  test(s"Scala lts.nightly, 3.lts.nightly & $currentltsPrefix.nightly point to the same version") {
    TestInputs.empty.fromRoot { root =>
      val version = getScalaVersion("lts.nightly", root)
      expect(version.startsWith(s"$currentltsPrefix."))
      expect(version == getScalaVersion("3.lts.nightly", root))
      expect(version == getScalaVersion(s"$currentltsPrefix.nightly", root))
    }
  }

  test(s"Scala $legacyLtsPrefix.lts.nightly & $legacyLtsPrefix.nightly point to the same version") {
    TestInputs.empty.fromRoot { root =>
      val version = getScalaVersion(s"$legacyLtsPrefix.lts.nightly", root)
      expect(version.startsWith(s"$legacyLtsPrefix."))
      val patch = version.split('.').take(3).last.takeWhile(_.isDigit).toInt
      if legacyLtsPrefix == "3.3" then expect(patch >= 8) // new nightly repo
      expect(version == getScalaVersion(s"$legacyLtsPrefix.nightly", root))
    }
  }

  test(s"Scala $nonLtsPrefix.lts is rejected, as $nonLtsPrefix is not an LTS series") {
    TestInputs.empty.fromRoot { root =>
      val output =
        getScalaVersion(s"$nonLtsPrefix.lts", root, check = false, mergeErrIntoOut = true)
      expect(output.contains(s"Cannot find matching Scala version for '$nonLtsPrefix.lts'"))
    }
  }
}
