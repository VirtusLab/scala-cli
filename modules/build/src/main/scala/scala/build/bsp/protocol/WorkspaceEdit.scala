package scala.build.bsp.protocol

import ch.epfl.scala.bsp4j as b

/** A WorkspaceEdit meant to mimic the LSP WorkspaceEdit.
  *
  * https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#workspaceEdit
  *
  * @param changes
  *   A Map of the URI that contains the diagnostic to the TextEdits that it contains.
  * @param documentChanges
  *   An array of resource operations that can just be an array of TextDocumentEdit[] or the various
  *   file resource operation.
  */
case class WorkspaceEdit(
  changes: Map[String, Array[TextEdits.TextEdit]],
  documentChanges: Array[ResourceOperation] = Array.empty
)
