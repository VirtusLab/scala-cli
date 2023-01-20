#!/usr/bin/env scala-cli

//> using lib "com.lihaoyi::os-lib:0.7.8"

/** Small and handy script to generate stubs for .svg files with nice TODO
  */

val content = os.read(os.pwd / "website" / "src" / "components" / "features.js")
val Image   = """.*image="([^ ]+)" +(title="(.+)")?.*""".r
def needStub(name: String) =
  !os.exists(os.pwd / "website" / "static" / "img" / name) && name.endsWith(".svg")

// Look for missing .svg files in out feature list
val stubs = content.linesIterator.collect {
  case Image(image, _, title) if needStub(image) =>
    image -> s"showing $title"
  case Image(image) if needStub(image) =>
    image -> ""
}.toList

// or provide data manually
// val stubs = Seq(
//   ("education", "Scala CLI within scope of learning Scala"),
//   ("scripting", "scripting with Scala CLI"),
//   ("prototyping", "Scala CLI used to prototype, experiment and debug"),
//   ("projects", "Scala CLI to manage single-module projects"),
//   ("demo", "general demo of Scala CLI"),
// )

if stubs.nonEmpty then
  val scriptBase = os.read(os.pwd / "gifs" / "example.sh")
  stubs.foreach { case (imageName, desc) =>
    val scriptName = imageName.stripSuffix(".svg") + ".sh"
    val dest       = os.pwd / "gifs" / "scenarios" / scriptName
    val fullDescr  = s"TODO: turn gifs/scenarios/$scriptName into proper scenario $desc"
    os.write.over(dest, scriptBase.replace("<description>", fullDescr))
    os.perms.set(dest, "rwxr-xr-x")
    println(s"Wrote: $dest")
  }
  val names = stubs.map(_._1.stripSuffix(".svg")).mkString(" ")
  println(s"To generate svg files run: 'gifs/generate_gif.sh $names'")
