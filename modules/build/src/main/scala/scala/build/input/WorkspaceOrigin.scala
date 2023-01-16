package scala.build.input

sealed abstract class WorkspaceOrigin extends Product with Serializable

object WorkspaceOrigin {
  case object Forced extends WorkspaceOrigin

  case object SourcePaths extends WorkspaceOrigin

  case object ResourcePaths extends WorkspaceOrigin

  case object HomeDir       extends WorkspaceOrigin
  case object VirtualForced extends WorkspaceOrigin
}
