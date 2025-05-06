package scala.build.preprocessing.directives

import dependency.AnyDependency

import scala.build.EitherCps.{either, value}
import scala.build.Positioned
import scala.build.directives._
import scala.build.errors.BuildException
import scala.build.options.WithBuildRequirements._
import scala.build.options.{
  BuildOptions,
  ClassPathOptions,
  Scope,
  ShadowingSeq,
  WithBuildRequirements
}
import scala.build.preprocessing.directives.Dependency.DependencyType
import scala.build.preprocessing.directives.DirectiveUtil._
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using dep com.lihaoyi::os-lib:0.9.1")
@DirectiveExamples(
  "//> using dep tabby:tabby:0.2.3,url=https://github.com/bjornregnell/tabby/releases/download/v0.2.3/tabby_3-0.2.3.jar"
)
@DirectiveExamples("//> using test.dep org.scalatest::scalatest:3.2.10")
@DirectiveExamples("//> using test.dep org.scalameta::munit:0.7.29")
@DirectiveExamples(
  "//> using compileOnly.dep com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:2.23.2"
)
@DirectiveExamples(
  "//> using scalafix.dep com.github.xuwei-k::scalafix-rules:0.5.1"
)
@DirectiveUsage(
  "//> using dep org:name:ver | //> using deps org:name:ver org2:name2:ver2",
  """`//> using dep` _org_`:`name`:`ver
    |
    |`//> using deps` _org_`:`name`:`ver _org_`:`name`:`ver
    |
    |`//> using dependencies` _org_`:`name`:`ver _org_`:`name`:`ver
    |
    |`//> using test.dep` _org_`:`name`:`ver
    |
    |`//> using test.deps` _org_`:`name`:`ver _org_`:`name`:`ver
    |
    |`//> using test.dependencies` _org_`:`name`:`ver _org_`:`name`:`ver
    |
    |`//> using compileOnly.dep` _org_`:`name`:`ver
    |
    |`//> using compileOnly.deps` _org_`:`name`:`ver _org_`:`name`:`ver
    |
    |`//> using compileOnly.dependencies` _org_`:`name`:`ver _org_`:`name`:`ver
    |
    |`//> using scalafix.dep` _org_`:`name`:`ver
    |
    |`//> using scalafix.deps` _org_`:`name`:`ver _org_`:`name`:`ver
    |
    |`//> using scalafix.dependencies` _org_`:`name`:`ver _org_`:`name`:`ver
    |""".stripMargin
)
@DirectiveDescription("Add dependencies")
@DirectiveLevel(SpecificationLevel.MUST)
final case class Dependency(
  @DirectiveName("lib")  // backwards compat
  @DirectiveName("libs") // backwards compat
  @DirectiveName("dep")
  @DirectiveName("deps")
  @DirectiveName("dependencies")
  dependency: List[Positioned[String]] = Nil,
  @DirectiveName("test.dependency")
  @DirectiveName("test.dep")
  @DirectiveName("test.deps")
  @DirectiveName("test.dependencies")
  testDependency: List[Positioned[String]] = Nil,
  @DirectiveName("compileOnly.lib")  // backwards compat
  @DirectiveName("compileOnly.libs") // backwards compat
  @DirectiveName("compileOnly.dep")
  @DirectiveName("compileOnly.deps")
  @DirectiveName("compileOnly.dependencies")
  compileOnlyDependency: List[Positioned[String]] = Nil,
  @DirectiveName("scalafix.dep")
  @DirectiveName("scalafix.deps")
  @DirectiveName("scalafix.dependencies")
  scalafixDependency: List[Positioned[String]] = Nil
) extends HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = List(
    Dependency.buildOptions(dependency, DependencyType.Runtime).map(_.withEmptyRequirements),
    Dependency.buildOptions(testDependency, DependencyType.Runtime).map(
      _.withScopeRequirement(Scope.Test)
    ),
    Dependency.buildOptions(compileOnlyDependency, DependencyType.CompileOnly)
      .map(_.withEmptyRequirements),
    Dependency.buildOptions(scalafixDependency, DependencyType.Scalafix)
      .map(_.withEmptyRequirements)
  )
}

object Dependency {
  val handler: DirectiveHandler[Dependency] = DirectiveHandler.derive

  sealed trait DependencyType
  object DependencyType {
    case object Runtime     extends DependencyType
    case object CompileOnly extends DependencyType
    case object Scalafix    extends DependencyType
  }

  def buildOptions(
    ds: List[Positioned[String]],
    tpe: DependencyType
  ): Either[BuildException, BuildOptions] = either {
    val dependencies: ShadowingSeq[Positioned[AnyDependency]] =
      value(ds.asDependencies.map(ShadowingSeq.from))
    val classPathOptions =
      tpe match {
        case DependencyType.Runtime => ClassPathOptions(extraDependencies = dependencies)
        case DependencyType.CompileOnly =>
          ClassPathOptions(extraCompileOnlyDependencies = dependencies)
        case DependencyType.Scalafix => ClassPathOptions(scalafixDependencies = dependencies)
      }

    BuildOptions(classPathOptions = classPathOptions)
  }
}
