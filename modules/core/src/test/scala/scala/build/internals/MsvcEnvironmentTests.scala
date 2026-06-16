package scala.build.internals

import com.eed3si9n.expecty.Expecty.expect

class MsvcEnvironmentTests extends munit.FunSuite {
  test("vcVarsCandidatePaths includes VS 2026 Enterprise path") {
    val paths = MsvcEnvironment.vcVarsCandidatePaths(vcVarsAllOverride = None)
    expect(
      paths.contains(
        """C:\Program Files\Microsoft Visual Studio\18\Enterprise\VC\Auxiliary\Build\vcvars64.bat"""
      )
    )
  }

  test("selectVcvars picks the newest candidate lexicographically") {
    val older =
      os.Path(
        """C:\Program Files\Microsoft Visual Studio\2019\Enterprise\VC\Auxiliary\Build\vcvars64.bat""",
        os.pwd
      )
    val newer =
      os.Path(
        """C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat""",
        os.pwd
      )
    expect(MsvcEnvironment.selectVcvars(Seq(older, newer)) == Some(newer))
  }
}
