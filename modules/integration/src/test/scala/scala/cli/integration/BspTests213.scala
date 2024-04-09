package scala.cli.integration

import ch.epfl.scala.bsp4j as b
import com.eed3si9n.expecty.Expecty.expect
import com.google.gson.{Gson, JsonElement}

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

class BspTests213 extends BspTestDefinitions with BspTests2Definitions with Test213 {
  List(".sc", ".scala").foreach { filetype =>
    test(s"bsp should report actionable diagnostic from bloop for $filetype files (Scala 2.13)") {
      val fileName = s"Hello$filetype"
      val inputs = TestInputs(
        os.rel / fileName ->
          s"""
             |object Hello {
             |  def foo: Any = {
             |    x: Int => x * 2
             |  }
             |}
             |""".stripMargin
      )
      withBsp(inputs, Seq(".", "-O", "-Xsource:3")) {
        (_, localClient, remoteServer) =>
          async {
            // prepare build
            val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
            // build code
            val targets = buildTargetsResp.getTargets.asScala.map(_.getId()).asJava
            await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)

            val visibleDiagnostics =
              localClient.diagnostics().map(_.getDiagnostics().asScala).find(!_.isEmpty).getOrElse(
                Nil
              )

            expect(visibleDiagnostics.size == 1)

            val updateActionableDiagnostic = visibleDiagnostics.head

            checkDiagnostic(
              diagnostic = updateActionableDiagnostic,
              expectedMessage = "parentheses are required around the parameter of a lambda",
              expectedSeverity = b.DiagnosticSeverity.ERROR,
              expectedStartLine = 3,
              expectedStartCharacter = 5,
              expectedEndLine = 3,
              expectedEndCharacter = 5,
              expectedSource = Some("bloop"),
              strictlyCheckMessage = false
            )

            val scalaDiagnostic = new Gson().fromJson[b.ScalaDiagnostic](
              updateActionableDiagnostic.getData().asInstanceOf[JsonElement],
              classOf[b.ScalaDiagnostic]
            )

            val actions = scalaDiagnostic.getActions().asScala.toList
            assert(actions.size == 1)
            val changes = actions.head.getEdit().getChanges().asScala.toList
            assert(changes.size == 1)
            val textEdit = changes.head

            expect(textEdit.getNewText().contains("(x: Int)"))
            expect(textEdit.getRange().getStart.getLine == 3)
            expect(textEdit.getRange().getStart.getCharacter == 4)
            expect(textEdit.getRange().getEnd.getLine == 3)
            expect(textEdit.getRange().getEnd.getCharacter == 10)
          }
      }
    }
  }
}
