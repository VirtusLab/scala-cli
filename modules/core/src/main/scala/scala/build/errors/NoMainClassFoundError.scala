package scala.build.errors

final class NoMainClassFoundError(note: Option[String] = None)
    extends MainClassError(
      ("No main class found" +: note.toSeq).mkString(System.lineSeparator()),
      Nil
    )
