package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect
import coursier.core.{Module, ModuleName, Organization}
import coursier.version.VersionConstraint

import scala.build.Artifacts
import scala.build.Artifacts.ScalaToolchain
import scala.build.options.ScalaOptions

class ArtifactsTests extends TestUtil.ScalaCliBuildSuite {

  private val fork = "ch.epfl.lara"

  private val forkVersion = VersionConstraint("3.10.1")

  private def forkProviding(modules: String*) =
    ScalaToolchain(fork, modules.map(_ -> forkVersion).toMap)

  private def dependency(organization: String, name: String, version: String = "1.0.0") =
    coursier.Dependency(
      Module(Organization(organization), ModuleName(name), Map.empty),
      VersionConstraint(version)
    )

  private def rewriting(toolchain: ScalaToolchain) =
    Artifacts.toolchainRewrite(toolchain)
      .getOrElse(sys.error(s"expected a rewrite for $toolchain"))

  private def organizationAfter(toolchain: ScalaToolchain)(moduleName: String) =
    rewriting(toolchain)(dependency("org.scala-lang", moduleName)).module.organization.value

  test("a module the organization provides is redirected to it") {
    val swapped = organizationAfter(forkProviding("scala3-library_3", "scala3-compiler_3"))
    expect(swapped("scala3-library_3") == fork)
    expect(swapped("scala3-compiler_3") == fork)
  }

  test("a module the organization does not provide is left alone") {
    val swapped = organizationAfter(forkProviding("scala3-library_3", "scala3-compiler_3"))
    expect(swapped("scala3-tasty-inspector_3") == "org.scala-lang")
  }

  test("the standard library follows whether the organization republishes it") {
    expect(organizationAfter(forkProviding("scala3-library_3", "scala-library"))(
      "scala-library"
    ) == fork)
    expect(organizationAfter(forkProviding("scala3-library_3"))("scala-library") ==
      "org.scala-lang")
  }

  test("the platform cross-publications of the standard library follow the JVM one") {
    val swapped = organizationAfter(forkProviding("scala3-library_3"))
    expect(swapped("scala3-library_sjs1_3") == fork)
    expect(swapped("scala3-library_native0.5_3") == fork)
  }

  test("a redirected module is aligned to the toolchain version") {
    val swapped = rewriting(forkProviding("scala3-library_3"))(
      dependency("org.scala-lang", "scala3-library_3", "3.3.5")
    )
    expect(swapped.versionConstraint == VersionConstraint("3.10.1"))
  }

  test("a redirected module keeps the version the organization publishes it at") {
    val toolchain = ScalaToolchain(
      fork,
      Map(
        "scala3-library_3" -> VersionConstraint("3.10.1"),
        "scala-library"    -> VersionConstraint("2.13.16")
      )
    )
    val swapped = rewriting(toolchain)(dependency("org.scala-lang", "scala-library", "2.13.10"))
    expect(swapped.module.organization.value == fork)
    expect(swapped.versionConstraint == VersionConstraint("2.13.16"))
  }

  test("the default organization needs no rewriting") {
    expect(Artifacts.toolchainRewrite(
      ScalaToolchain(ScalaOptions.defaultOrganization, Map("scala3-library_3" -> forkVersion))
    ).isEmpty)
  }

  test("an organization with nothing discovered needs no rewriting") {
    expect(Artifacts.toolchainRewrite(ScalaToolchain(fork)).isEmpty)
  }

  test("dependency metadata rewrites the toolchain and excludes the official one elsewhere") {
    val toolchain               = forkProviding("scala3-library_3", "scala-library")
    val osLib                   = dependency("com.lihaoyi", "os-lib_3", "0.11.5")
    val upstreamLibrary         = dependency("org.scala-lang", "scala-library", "2.13.16")
    val Seq(aligned, rewritten) = Artifacts.excludeUpstreamToolchain(
      toolchain,
      toolchain.providedModules.keySet
    )(Artifacts.rewriteRootDeps(toolchain)(Seq(osLib, upstreamLibrary)))
    expect(rewritten.module.organization.value == fork)
    expect(aligned.exclusions().contains(
      Organization("org.scala-lang") -> ModuleName("scala3-library_3")
    ))
  }

  test("the exclusions cover the platform modules that were redirected") {
    val toolchain  = forkProviding("scala3-library_3")
    val Seq(munit) = Artifacts.excludeUpstreamToolchain(
      toolchain,
      toolchain.providedModules.keySet + "scala3-library_sjs1_3"
    )(Seq(dependency("org.scalameta", "munit_sjs1_3", "1.0.0")))
    expect(munit.exclusions().contains(
      Organization("org.scala-lang") -> ModuleName("scala3-library_sjs1_3")
    ))
  }

  test("the organization's own dependencies carry no exclusions") {
    val toolchain        = forkProviding("scala3-library_3")
    val Seq(forkLibrary) =
      Artifacts.excludeUpstreamToolchain(toolchain, toolchain.providedModules.keySet)(
        Seq(dependency(fork, "scala3-library_3", "3.10.1"))
      )
    expect(forkLibrary.exclusions().isEmpty)
  }

  private def module(organization: String, name: String) =
    Module(Organization(organization), ModuleName(name), Map.empty)

  test("a module provided by both the fork and the official organization is reported") {
    val duplicated = Artifacts.duplicatedScalaToolchainModules(
      fork,
      Seq(
        module(fork, "scala3-library_3"),
        module("org.scala-lang", "scala3-library_3"),
        module("com.lihaoyi", "os-lib_3")
      )
    )
    expect(duplicated == Seq("scala3-library_3"))
  }

  test("a module only the official organization provides is not reported") {
    val duplicated = Artifacts.duplicatedScalaToolchainModules(
      fork,
      Seq(module(fork, "scala3-library_3"), module("org.scala-lang", "scala3-tasty-inspector_3"))
    )
    expect(duplicated.isEmpty)
  }

  test("nothing is reported for the default organization") {
    val duplicated = Artifacts.duplicatedScalaToolchainModules(
      ScalaOptions.defaultOrganization,
      Seq(module("org.scala-lang", "scala3-library_3"))
    )
    expect(duplicated.isEmpty)
  }
}
