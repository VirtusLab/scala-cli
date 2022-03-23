package scala.build

import scala.quoted.*

import coursier.core.{Module, Dependency => CDependency}
import coursier.parse.{DependencyParser, ModuleParser}

import dependency.{DependencyLike, NameAttributes}

def noArgs(args: Expr[Seq[Any]])(using Quotes): Unit = {} // TODO

def extractString(cs: Expr[StringContext])(using Quotes): String =
  cs.value match {
    // without Seq it brokes compiler! - check this!
    case Some(StringContext(Seq(part: String))) =>
      part
    case _ =>
      quotes.reflect.report.error("StringContext args must be statically known")
      ???
  }

object CoursierUtils {
  def parseModule(cs: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[Module] =
    noArgs(args)
    val modString = extractString(cs)
    ModuleParser.module(modString, scala.util.Properties.versionNumberString) match {
      case Left(error) =>
        quotes.reflect.report.error(error)
        ???
      case Right(_) =>
        '{
          ModuleParser.module(
            ${ Expr(modString) },
            scala.util.Properties.versionNumberString
          ).getOrElse(???)
        }
    }

  extension (inline ctx: StringContext)
    inline def cmod(inline args: Any*): Module =
      ${ parseModule('ctx, 'args) }
}
