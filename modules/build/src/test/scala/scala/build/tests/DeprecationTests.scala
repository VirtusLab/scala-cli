package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.errors.Diagnostic
import scala.build.internal.util.WarningMessages
import scala.build.internals.FeatureType
import scala.build.options.SuppressWarningOptions
import scala.build.preprocessing.DeprecatedDirectives
import scala.build.preprocessing.directives.StrictDirective
import scala.collection.mutable.ListBuffer

class DeprecationTests extends TestUtil.ScalaCliBuildSuite {

  test("deprecatedFeaturesUsed formats single feature with name prefix") {
    val msg = WarningMessages.deprecatedFeaturesUsed(
      Seq(("--some-option", "Use --other instead.", FeatureType.Option))
    )
    expect(msg.contains("`--some-option` option is deprecated."))
    expect(msg.contains("Use --other instead."))
    expect(msg.contains("Deprecated features may be removed"))
  }

  test("deprecatedFeaturesUsed formats single feature with no detail") {
    val msg = WarningMessages.deprecatedFeaturesUsed(
      Seq(("--old-alias", "", FeatureType.Option))
    )
    expect(msg.contains("`--old-alias` option is deprecated."))
    expect(!msg.contains("is deprecated. "))
    expect(msg.contains("Deprecated features may be removed"))
  }

  test("deprecatedFeaturesUsed formats multiple features with name prefix") {
    val msg = WarningMessages.deprecatedFeaturesUsed(Seq(
      ("--opt-a", "Use --opt-b.", FeatureType.Option),
      ("my-command", "Use other-command.", FeatureType.Subcommand)
    ))
    expect(msg.contains("`--opt-a` option is deprecated. Use --opt-b."))
    expect(msg.contains("`my-command` sub-command is deprecated. Use other-command."))
    expect(msg.contains("Deprecated features may be removed"))
  }

  private class DiagnosticCapturingLogger extends TestLogger() {
    val diagnostics: ListBuffer[Diagnostic]        = ListBuffer.empty
    override def log(diags: Seq[Diagnostic]): Unit =
      diagnostics ++= diags
  }

  test("DeprecatedDirectives detects deprecatedTestDirective") {
    val directive = StrictDirective("deprecatedTestDirective", Seq.empty)
    val logger    = new DiagnosticCapturingLogger()
    DeprecatedDirectives.issueWarnings(
      Left("test.scala"),
      Seq(directive),
      SuppressWarningOptions(),
      logger
    )
    expect(logger.diagnostics.exists(_.message.contains("deprecatedTestDirective")))
  }

  test("DeprecatedDirectives suppresses warnings when configured") {
    val directive = StrictDirective("deprecatedTestDirective", Seq.empty)
    val logger    = new DiagnosticCapturingLogger()
    DeprecatedDirectives.issueWarnings(
      Left("test.scala"),
      Seq(directive),
      SuppressWarningOptions(suppressDeprecatedFeatureWarning = Some(true)),
      logger
    )
    expect(logger.diagnostics.isEmpty)
  }

  test("DeprecatedDirectives deprecated for removal emits warning without TextEdit") {
    val directive = StrictDirective("deprecatedForRemovalTestDirective", Seq.empty)
    val logger    = new DiagnosticCapturingLogger()
    DeprecatedDirectives.issueWarnings(
      Left("test.scala"),
      Seq(directive),
      SuppressWarningOptions(),
      logger
    )
    val diag = logger.diagnostics.find(_.message.contains("deprecatedForRemovalTestDirective"))
    expect(diag.isDefined)
    expect(diag.get.message.contains("removed in a future version"))
    expect(diag.get.textEdit.isEmpty)
  }

  test("DeprecatedDirectives key replacement emits warning with TextEdit") {
    val directive = StrictDirective("deprecatedTestDirective", Seq.empty)
    val logger    = new DiagnosticCapturingLogger()
    DeprecatedDirectives.issueWarnings(
      Left("test.scala"),
      Seq(directive),
      SuppressWarningOptions(),
      logger
    )
    val diag = logger.diagnostics.find(_.message.contains("deprecatedTestDirective"))
    expect(diag.isDefined)
    expect(diag.get.textEdit.isDefined)
  }
}
