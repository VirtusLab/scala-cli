package scala.cli.tests

import munit.FunSuite

import scala.cli.commands.shared.ScalacOptions

final class ScalacOptionsPrintTest extends FunSuite {

  test("isColonHelpPrintOption: :help suffix (single segment)") {
    assert(ScalacOptions.isColonHelpPrintOption("opt-inline:help"))
    assert(ScalacOptions.isColonHelpPrintOption("Xlint:help"))
    assert(ScalacOptions.isColonHelpPrintOption("opt:help"))
  }

  test("isColonHelpPrintOption: :help suffix (multi-level colons)") {
    assert(ScalacOptions.isColonHelpPrintOption("opt:l:inline:help"))
    assert(ScalacOptions.isColonHelpPrintOption("a:b:help"))
    assert(ScalacOptions.isColonHelpPrintOption("foo:bar:baz:help"))
  }

  test("isColonHelpPrintOption: reject non-suffix") {
    assert(!ScalacOptions.isColonHelpPrintOption("help"))
    assert(!ScalacOptions.isColonHelpPrintOption("Xlint:infer-any"))
    assert(!ScalacOptions.isColonHelpPrintOption("help:foo"))
    assert(!ScalacOptions.isColonHelpPrintOption("something:helpme"))
  }

  test("isScalacPrintOption: combines explicit set and :help rule") {
    assert(ScalacOptions.isScalacPrintOption("Xshow-phases"))
    assert(ScalacOptions.isScalacPrintOption("help"))
    assert(ScalacOptions.isScalacPrintOption("Xsource:help"))
    assert(ScalacOptions.isScalacPrintOption("opt:l:inline:help"))
    assert(!ScalacOptions.isScalacPrintOption("random-flag"))
  }
}
