import $ivy.`com.lihaoyi::os-lib:0.7.8`

@main def ala(args: String*) = 
  if args.isEmpty then 
    println(os.list(os.pwd).mkString(", "))
  else 
    args.foreach { p => 
      val path = os.pwd / "gifs" / "scenarios" / s"$p.sh"
      if !os.exists(path) then
        println(s"No example named '$p' in ${path / os.up}")
        sys.exit(1)
      processScenario(path, p)
    }

val asciCinemaHash = "@sha256:4ad3f3d913f59178ca69882431c3622cf9b7cd394e4f29e5ab31d4909033bdfc"

lazy val pullDocker = 
  os.proc("docker","pull", s"asciinema/asciicast2gif$asciCinemaHash").call

val terminalWith = 80 // columns
val terminalHeight = 24 // rows

def processScenario(script: os.FilePath, name: String) =
  val ws = os.temp.dir()
  pullDocker

  val castFile = s"$name.cast"

  def asciicast2gifArgs(themeName: String, gifName: String) = 
    Seq[os.Shellable](
      "docker", "run", "--rm", "-v", s"$ws:/data", "asciinema/asciicast2gif",
          "-w", terminalWith.toString, "-h", terminalHeight.toString, "-t", themeName, 
      castFile.toString, gifName
    )

  val processes = Seq[Seq[os.Shellable]](
    Seq("asciinema", "rec",  s"--command=$script -n", castFile.toString),
    asciicast2gifArgs("monokai",  s"${name}_dark.gif"),
    asciicast2gifArgs("asciinema", s"${name}.gif")    
  )

  processes.foreach(args => 
    os.proc(args*).call(stdin = os.Inherit, stdout = os.Inherit, cwd = ws))
  

  os.copy.over(ws / (name+".gif"), os.pwd / "website" / "static" / "img" / (name+".gif"))
  os.copy.over(ws / (name+"_dark.gif"), os.pwd / "website" / "static" / "img" / "dark" / (name+".gif"))
  
  