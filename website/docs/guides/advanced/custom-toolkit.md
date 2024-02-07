title: Custom Toolkit
sidebar_position: 44
---
Similar to the Scala Toolkit and Typelevel toolkit, it is possible to create your own, custom toolkit. 
Having a custom toolkit with common libraries can speed up the development using scala-cli. 

Let's look at how we can create a new toolkit. 

For example, to create a LiHaoyi ecosystem toolkit, we can name the file as `LiHaoyiToolkit.scala` and add the required libraries as dependency directives:

```
//> using scala 2.13, 3
//> using publish.name toolkit
//> using dep com.lihaoyi::upickle::3.1.3
//> using dep com.lihaoyi::os-lib::0.9.2
//> using dep com.lihaoyi::requests::0.8.0
//> using dep com.lihaoyi::fansi::0.4.0
``` 
This toolkit is a combination of 4 libraries from `com.lihaoyi` organization as defined before. The key `publish.name` must have the value `toolkit` to be used as a toolkit. 

Similarly, define the scalajs version of toolkit in `LiHaoyiToolkit.js.scala` file. Notice the `js.scala` extension. It should also have `publish.name` as `toolkit`. 

If testkit is supported, it can also be added as another file, `LiHaoyiToolkitTest.scala` with `publish.name` as `toolkit-test`:
```
//> using scala 2.13, 3
//> using publish.name toolkit-test
//> using dep com.lihaoyi::utest::0.8.2
```

Additionally, more configurations needed for publishing the toolkit can be kept in a conf file, for example, `publish-conf.scala`:
```
//> using publish.organization com.yadavan88
//> using publish.version 0.1.0
//> using publish.url https://github.com/yadavan88/lihaoyi-toolkit
//> using publish.license Apache-2.0
//> using publish.repository central
//> using publish.developer "yadavan88|Yadu Krishnan|https://github.com/yadavan88"
//> using repository sonatype:public
```

The toolkit can be published locally using the command:
```
scala-cli --power publish local --cross LiHaoyiToolkit.scala publish-conf.scala
```

In the similar way, it is also possible to publish to a central repository. Refer to the [GitHub Action workflow](https://github.com/scala/toolkit/blob/main/.github/workflows/deploy.yaml) for more details.

Once it is published, it can be accessed using the org-name with which it got published. For example, with the published toolkit under the organization `com.yadavan88`, it can be accessed as:

```
//> using toolkit com.yadavan88:0.1.0

@main
def main() = {
  println(fansi.Color.Blue("Hello world!"))
  println("path is : " + os.pwd)
}

```
This brings in all the dependencies mentioned in the custom toolkit file.
