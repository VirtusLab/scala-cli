# Scala CLI

`features marked as code are still under development`

## Learn language, not a build tool

Scala-cli combines all of the feature that you need to learn and use Scala in your (simple) projects. Scala-cli is intuitive `and interactive`. We believe that you can just [install](/docs/installation) and [run scala-cli](/docs/input) for the first time and just skip reading rest of this page.

Are you still with us? Ok, let us convince you why scala-cli is worth to give it a go.

## Easy to install, responsive and fast

Our installation instructions are as simple as possible. Just pick your distribution and install scala-cli. No need to fluff with installing JVM or setting up PATH.

![Install demo - needs better gif](/img/scala-cli-install.gif)

Scala-cli is designed to be as fast and responsive as possible. One of our goals is to prove that Scala does not necessary needs ages to compile and run.

![responsiveness - needs better gif](/img/fast-scala-cli.gif)

Scala-cli commands are easy to discover, `our commands are conversational to basically remove need of learning how to use our tool`.


## All the features that you need

Scala-cli can **run**, **compile** and `format` your code. We support **REPL** and **scripts**. We can run your **tests** or even **package** your project so you can share results of your work!

// gif

If you want to use older **version of Scala** or run your code in **JS** or **Native** environments we've got you covered (some additional [setup](/TODO/additional_setup) is required).

// gif


`When you project grows, scala-cli will be able to convert it into sbt, gradle or mill project. Then, you will be still able to interact with your project using scala-cli, thanks to the magic of BSP.`


## Scala-cli is not a build tool

Scala-cli shares some similarities with build tools, but doesn't aim at supporting multi-module projects, nor to be extended via a task system. Scala ecosystem has multiple amazing build tools, there is no need to create another one. 

** Scala-cli supports one module and limited set of commands. **

Scala-cli is designed to fill the roles when complexity of a proper build tool kills productivity.


## Can scala-cli becomes simply scala?

We are thinking that scala-cli may one day replaces `scala` command. For now scala-cli is just a bit more then working prototype and we need your feedback, what do you thing about the idea, features and ergonomics of scala-cli. 

Should scala-cli also targets simple libraries so we should provide support for cross compiling?

Do you think that having a `scala-compose` or similar tool that will allow to combine multiple scala-cli projects is a good idea?

Let us know!