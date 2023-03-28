package scala.build.bsp.protocol

import ch.epfl.scala.bsp4j as b
import com.google.gson.{Gson, JsonElement}

/** Representation for the data field in a bsp.Diagnostic.
  *
  * For now this only contains an edit, but there is nothing from keeping it having more field in
  * the future.
  * @param edit
  *   The Workspace edit attatched to the Diagnostic.
  */
case class DiagnosticData(edits: Array[WorkspaceEdit]) {

  /** Go from the DiagnosticData to Json. This will result in each param being a field in the
    * object.
    *
    * @return
    *   the JsonElement
    */
  def toJsonTree(): JsonElement = new Gson().toJsonTree(this)
}

/** A WorkspaceEdit meant to mimic the LSP WorkspaceEdit.
  *
  * https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#workspaceEdit
  *
  * For now this only utilizes changes, hence the default empty for both documentChanges and
  * changeAnnotations. In the future when we support more operations like, CreateFile, RenameFile,
  * etc we can expand on this and change the type of documentChanges to actually be an
  * Array[TextDocumentEdit] | Array[TextDocumentEditTypes].
  *
  * @param changes
  *   A Map of the URI that contains the diagnostic to the TextEdits that it contains.
  * @param documentChanges
  *   NOT CURRENTLY USED
  * @param changeAnnotations
  *   NOT CURRENTLY USED
  */
case class WorkspaceEdit(
  changes: Map[String, Array[TextEdit]],
  documentChanges: Array[Object] = Array.empty,
  changeAnnotations: Map[String, Object] = Map.empty
)

/** A TextEdit meant to mimic the LSP TextEdit.
  *
  * https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textEdit
  */
case class TextEdit(range: b.Range, newText: String)
