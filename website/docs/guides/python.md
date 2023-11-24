---
title: Python/ScalaPy ⚡️
sidebar_position: 70
---

:::caution
ScalaPy support is an experimental feature.

Please bear in mind that non-ideal user experience should be expected.
If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team
on [GitHub](https://github.com/VirtusLab/scala-cli).
:::

ScalaPy is a library that allows you to access the Python interpreter from Scala code. It boasts a simple API, automatic conversion between Scala and Python types, and optional static typing.
It makes it possible to integrate Python libraries into Scala CLI projects.

Scala CLI allows to configure the ScalaPy library with the `--python` flag and `//> using python` directive.

More information about ScalaPy can be found [here](https://scalapy.dev).

## Example usage

Some configuration might be needed before running the examples below:

```bash ignore
# install Python 3.11 (e.g. via an installer from the official Python website)
# then download the packages with
pip3 install numpy matplotlib python-config
```

```scala
//> using python
//> using scala 2.13

import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import py.PyQuote

py.local {
  val np = py.module("numpy")

  val rng = np.random.default_rng()

  val randoms = rng.standard_normal(10).as[Seq[Double]]

  randoms.foreach(println(_))
}

val numbers = py"[x * 2 for x in ${Iterator.from(3).take(10).toList.toPythonCopy}]"
  .as[Seq[Int]]

println(numbers)
```

You can also use Scala Native to create a native binary with direct bindings to CPython. 

```scala
//> using python

import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters

import scala.util.Random
import scala.math.{Pi, sin, random}

object PlotDemo {
  @main
  def plot = {
    val sequences = generate3DataSeqs

    py.local {
      val plt = py.module("matplotlib.pyplot")

      for {
        (seq, color) <- sequences.zip(Seq("b", "r", "g"))
      } {
        plt.plot(seq.toPythonProxy, color = color)
        plt.show()
      }
    }
  }

  def generate3DataSeqs: Seq[Seq[Double]] = {
    val amplitude = 1.0 // Amplitude of the sine wave
    val numSamples = 1000
    val numSequences = 3
    val noiseAmplitude = 0.2 // Amplitude of noise

    // Generate three sequences with varying numbers of cycles
    val sequences = (1 to numSequences).map { seqIdx =>
      val frequency = seqIdx // Varying frequency for each sequence
      (1 to numSamples).map { sampleIdx =>
        val noise = (random * 2 - 1) * noiseAmplitude // Generate random noise
        val phase = 2 * Pi * frequency * sampleIdx / numSamples
        amplitude * sin(phase) + noise
      }
    }
    sequences
  }
}

```
Run:
```bash ignore
scala-cli --power package --native PlotDemo.scala -o plot
./plot
```

