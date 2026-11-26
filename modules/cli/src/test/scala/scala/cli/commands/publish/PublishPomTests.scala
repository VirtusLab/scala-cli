package scala.cli.commands.publish

import com.eed3si9n.expecty.Expecty.expect
import coursier.core.{ModuleName, Organization}

import scala.publish.Pom

class PublishPomTests extends munit.FunSuite {

  private val emptyProject = Pom.Project(
    organization = Organization("org.example"),
    moduleName = ModuleName("demo_3"),
    version = "1.0.0"
  )

  test("every missing Maven Central requirement points to the directive setting it") {
    val diagnostics = PublishUtils.centralRequirementDiagnostics(emptyProject)
    expect(diagnostics.length == Pom.centralRequirementErrors(emptyProject).length)
    expect(diagnostics.nonEmpty)
    for (diagnostic <- diagnostics)
      expect(diagnostic.contains("//> using publish."))
    expect(diagnostics.exists(_.contains("'//> using publish.url'")))
    expect(diagnostics.exists(_.contains("'//> using publish.scm'")))
  }

  test("no Maven Central diagnostics for a complete project") {
    val project = emptyProject.copy(
      description = Some("A demo project"),
      url = Some("https://github.com/example/demo"),
      name = Some("demo"),
      licenses = Seq(Pom.License("Apache-2.0", "https://spdx.org/licenses/Apache-2.0.html")),
      scm = Some(Pom.Scm(
        "https://github.com/example/demo.git",
        "scm:git:https://github.com/example/demo.git",
        "scm:git:ssh://git@github.com/example/demo.git"
      )),
      developers = Seq(Pom.Developer("someone", "Someone", "https://github.com/someone", None))
    )
    expect(PublishUtils.centralRequirementDiagnostics(project).isEmpty)
  }
}
