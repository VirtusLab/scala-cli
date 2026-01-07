package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class RunTests3NextRc extends RunTestDefinitions with Test3NextRc {
  test("Scala 3.nightly & 3.<latest-minor>.nightly point to the same version") {
    TestInputs.empty.fromRoot { root =>
      val version1     = getScalaVersion("3.nightly", root)
      val nightlyMinor = version1.split('.').take(2).last
      val version2     = getScalaVersion(s"3.$nightlyMinor.nightly", root)
      expect(version1 == version2)
    }
  }

  for {
    label <- List("rc", "3.rc", "3.lts.rc", "lts.rc", s"${Constants.scala3LtsPrefix}.rc", "3.7.rc")
  }
    test(s"$label is valid and works as expected") {
      TestInputs.empty.fromRoot { root =>
        val latestRcVersion = getScalaVersion(label, root)
        if latestRcVersion == actualScalaVersion then {
          expect(
            label == "rc" || label == "3.rc"
          ) // this should only be the case for *latest* labels
          System.err.println(s"RC version $latestRcVersion is the same as the hardcoded latest RC")
        }
        expect(latestRcVersion.startsWith("3."))
        expect(latestRcVersion.contains("-RC"))
        expect(!latestRcVersion.contains("SNAPSHOT"))
        expect(!latestRcVersion.contains("NIGHTLY"))
      }
    }

  for { label <- List("2.rc", "2.12.rc", "2.13.rc") }
    test(s"$label produces a reasonable error") {
      TestInputs.empty.fromRoot { root =>
        val result = getScalaVersion(label, root, check = false, mergeErrIntoOut = true)
        expect(result.contains("Invalid Scala version"))
        expect(result.contains(
          "In the case of Scala 2, a particular nightly version serves as a release candidate."
        ))
      }
    }
}
