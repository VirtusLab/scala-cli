package scala.cli.util

object SeqHelpers {
  implicit class StringSeqOpt(val seq: Seq[String]) extends AnyVal {
    def appendOnInit(s: String): Seq[String] =
      if seq.isEmpty || seq.tail.isEmpty then seq
      else (seq.head + s) +: seq.tail.appendOnInit(s)
  }
}
