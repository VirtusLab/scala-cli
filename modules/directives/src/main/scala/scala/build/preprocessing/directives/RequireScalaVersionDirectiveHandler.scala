package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, DirectiveErrors}
import scala.build.options.BuildRequirements
import scala.build.preprocessing.Scoped
import scala.build.preprocessing.directives.UsingDirectiveValueKind.UsingDirectiveValueKind
import scala.build.Positioned
import scala.meta.Decl.Val

object RequireScalaVersionDirectiveHandlers {
  import BuildRequirements.*

  abstract class Handler(suffixes: String*)(parse: String => VersionRequirement) 
    extends BuildRequirementsHandler[Positioned[String]]{

      //def name             = "Platform"
      // def description      = "Require a Scala platform for the current file"
      def usagesCode = keys.map(k => s"//> using target.scala$k (<version> [in <path>])+ ")
      override def examples = 
        Seq("3", "2.12 in \"src_2.12\", 2.13 in \"src_2.13\"", "\"2.13.8\"")
          .zip(keys ++ keys ++ keys) // make sure we do not run out of keys
          .map{ (k, v) => s"//> using target.scala$k $v"}
        
      def keys: Seq[String] = suffixes.map("target.scala" + _)
      def constrains = Single(ValueType.String, ValueType.Number) 

      def process(value: Positioned[String])(using Ctx) = 
        Right(BuildRequirements(scalaVersion =  Seq(parse(value.value))))
  }

  object Matching extends Handler(".==", "")(v => VersionEquals(v, loose = true)){
    def name = "Specific Scala Version"
    def description = "Requie specific Scala Version acording to SemVer. " +
      "This means that when `2.12` is provided all `2.12.x` version will fulfill the requirement."
  }

  object HigherThan extends Handler(".>")(v => VersionHigherThan(v, orEqual = false)){
    def name = "Scala version higher then"
    def description = "Requie Scala Version higher then provided acording to SemVer. " +
      "This means that when `2.12` is provided all `2.13.x` and newer version will fulfill the requirement."
  }

  object HigherThanOrEqual extends Handler(".>=")(v => VersionHigherThan(v, orEqual = true)){
    def name = "Scala version higher then or equal to"
    def description = "Requie Scala Version higher then or equal to provided acording to SemVer. " +
      "This means that when `2.12` is provided all `2.12.x` and newer version will fulfill the requirement."
  }

    object LowerThan extends Handler(".<")(v => VersionLowerThan(v, orEqual = false)){
    def name = "Scala version lower then"
    def description = "Requie Scala Version lower then provided acording to SemVer. " +
      "This means that when `2.13` is provided all `2.12.x` and older version will fulfill the requirement."
  }

  object LowerThanOrEqual extends Handler(".<=")(v => VersionLowerThan(v, orEqual = true)){
    def name = "Scala version lower then or equal to"
    def description = "Requie Scala Version lower then or equal to provided acording to SemVer. " +
      "This means that when `2.13` is provided all `2.13.x` and older version will fulfill the requirement."
  }

  val group = DirectiveHandlerGroup(
      "Scala Versions", 
      Seq(Matching, LowerThan, LowerThanOrEqual, HigherThan, HigherThanOrEqual)
    )
}