//> using scala "3"
//> using lib "com.lihaoyi::os-lib:0.8.0"

val toParse: Seq[os.Path] = 
  if args.nonEmpty then args.map(p => os.pwd / os.RelPath(p))
  else os.list(os.pwd / "examples").filterNot(_.last.startsWith("."))


val failed = toParse.filter { path =>
  val args = 
    if !os.exists(path / ".opts") then Nil
    else os.read(path / ".opts").split(" ").toSeq


  println(s"Running $path")
  os.proc(os.pwd / "scala-cli-src", "--jvm", "temurin:17", args, path)
    .call(check = false).exitCode != 0
}

if failed.isEmpty then println("Success!") else 
  println("Failed examples:")
  failed.foreach(path => println(s"  $path failed"))