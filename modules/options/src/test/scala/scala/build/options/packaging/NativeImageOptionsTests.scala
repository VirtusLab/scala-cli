package scala.build.options.packaging

import com.eed3si9n.expecty.Expecty.expect

import scala.build.internal.Constants

class NativeImageOptionsTests extends munit.FunSuite {
  test("macOS x64 defaults to the last GraalVM version available for it") {
    expect(
      NativeImageOptions.defaultGraalVMVersion(isMacosX64 = true) ==
        Constants.defaultGraalVMMacosX64Version
    )
  }

  test("other platforms default to the newest GraalVM version") {
    expect(
      NativeImageOptions.defaultGraalVMVersion(isMacosX64 = false) ==
        Constants.defaultGraalVMVersion
    )
  }

  test("default JVM id uses the default GraalVM version for the current platform") {
    expect(
      NativeImageOptions().jvmId ==
        s"graalvm-java${Constants.defaultGraalVMJavaVersion}:${NativeImageOptions.defaultGraalVMVersion}"
    )
  }

  test("explicit GraalVM version takes precedence over the platform default") {
    expect(
      NativeImageOptions(graalvmVersion = Some("25.0.0")).jvmId ==
        s"graalvm-java${Constants.defaultGraalVMJavaVersion}:25.0.0"
    )
  }
}
