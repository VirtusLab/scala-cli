//> using scala "2.13"
//> using dep "com.thesamet.scalapb::scalapbc:0.11.13"

import scalapb.ScalaPBC

class ProtobufGenerator {
  def metadataJson: String =
    s"""{
       |  "name" : "Protobuf generator",
       |  "version" : "1.0.0",
       |  "supportedExtensions" : ["proto"],
       |  "isReferentiallyTransparent" : true
       |}""".stripMargin

  def generate(inputLocation: String, outputLocation: String): Unit = {
    ScalaPBC.main(Array(
        s"--scala_out=${outputLocation}",
        "-I", "/Users/mgajek/Projects/scala-cli-sandbox/source-generators/",
        inputLocation
    ))
  }
}

object Main {
    def main(args: Array[String]) = {
        val outputDir = args.headOption.getOrElse(throw new Exception("Output dir not specified"))
        val sources = args.tail.toList
        val generator = new ProtobufGenerator()

        sources.foreach { source =>
            println(s"Running protobuf generation from [$source] to [$outputDir]")
            generator.generate(source, outputDir)
        }
    }
}
