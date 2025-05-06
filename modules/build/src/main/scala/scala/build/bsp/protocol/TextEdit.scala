package scala.build.bsp.protocol

import ch.epfl.scala.{bsp4j => b}
import com.google.gson.Gson

case class TextEdit(range: b.Range, newText: String) {
  def toJsonTree() = new Gson().toJsonTree(this)
}
