package scala.build.bsp.protocol

import ch.epfl.scala.bsp4j as b

/** Representing the various types of TextEdits
  */
sealed trait TextEdits

object TextEdits {

  /** A TextEdit meant to mimic the LSP TextEdit.
    *
    * https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textEdit
    */
  case class TextEdit(range: b.Range, newText: String) extends TextEdits

  /** An AnnotatedTextEdit meant to mimic the LSP AnnotatedTextEdit.
    * https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textEdit
    */
  case class AnnotatedTextEdit(range: b.Range, newText: String, annotationId: String)
}
