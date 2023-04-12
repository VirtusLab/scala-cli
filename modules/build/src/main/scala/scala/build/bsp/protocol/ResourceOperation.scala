package scala.build.bsp.protocol

/** The operation types that are available if using `documentChanges` in your `WorkspaceEdit`s.
  */
sealed trait ResourceOperation

object ResourceOperation {

  /** Describes textual changes on a single text document. The text document is referred to as a
    * VersionedTextDocumentIdentifier to allow clients to check the text document version before an
    * edit is applied. A TextDocumentEdit describes all changes on a version Si and after they are
    * applied move the document to version Si+1. So the creator of a TextDocumentEdit doesn’t need
    * to sort the array or do any kind of ordering. However the edits must be non overlapping.
    *
    * @param textDocument
    *   The text document to change
    * @param edits
    *   The edits to be applied
    */
  case class TextDocumentEdit(
    textDocument: TextDocumentEdit.VersionedTextDocumentIdentifier,
    edits: Array[TextEdits]
  ) extends ResourceOperation

  object TextDocumentEdit {

    /** The version number of this document. If a versioned text document identifier is sent from
      * the server to the client and the file is not open in the editor (the server has not received
      * an open notification before) the server can send `null` to indicate that the version is
      * known and the content on disk is the truth (as specced with document content ownership).
      *
      * The version number of a document will increase after each change, including undo/redo. The
      * number doesn't need to be consecutive.
      */
    case class VersionedTextDocumentIdentifier(version: Int)
  }

  /** Create file operation
    *
    * @param kind
    *   A create
    * @param uri
    *   The resource to create
    * @param options
    *   Additional create options
    */
  case class CreateFile(
    kind: "create" = "create",
    uri: String,
    options: CreateFile.CreateFileOptions
  ) extends ResourceOperation

  object CreateFile {

    /** Options to create a file
      *
      * @param overwrite
      *   Overwrite existing file. Overwrite wins over `ignoreIfExists`
      * @param ignoreIfExists
      *   Ignore if target exists
      */
    case class CreateFileOptions(overwrite: Boolean, ignoreIfExists: Boolean)
  }

  /** Rename file operation
    *
    * @param kind
    *   A rename
    * @param oldUri
    *   The old (existing) location
    * @param newUri
    *   The new location
    * @param options
    *   Additional rename options
    */
  case class RenameFile(
    kind: "rename" = "rename",
    oldUri: String,
    newUri: String,
    options: RenameFile.RenameFileOptions
  ) extends ResourceOperation

  object RenameFile {

    /** Rename file options
      *
      * @param overwrite
      *   Overwrite target if existing. Overwrite wins over `ignoreIfExists`
      * @param ignoreIfExists
      *   Ignores if target exists
      */
    case class RenameFileOptions(overwrite: Boolean, ignoreIfExists: Boolean)
  }

  /** Delete file operation
    *
    * @param kind
    *   A delete
    * @param uri
    *   The file to delete
    * @param options
    *   Additional delete options
    */
  case class DeleteFile(
    kind: "delete" = "delete",
    uri: String,
    options: DeleteFile.DeleteFileOptions
  ) extends ResourceOperation

  object DeleteFile {

    /** Delete file options
      *
      * @param recursive
      *   Delete the content recursively if a folder is denoted.
      * @param ignoreIfNotExists
      *   Ignore the operation if the file doesn't exist.
      */
    case class DeleteFileOptions(recursive: Boolean, ignoreIfNotExists: Boolean)
  }
}
