package scala.build.bsp.protocol

import com.google.gson.{Gson, JsonElement}
import org.eclipse.lsp4j.WorkspaceEdit

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
