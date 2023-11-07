---
title: Scala CLI within Emacs
sidebar_position: 14
---

Emacs users can make it easier to use Scala CLI from within their editor by
loading an extension: <https://github.com/ag91/scala-cli-repl>.

That lets you send Scala code directly from your buffer to the Scala REPL.

![scala-cli-repl-demo](/img/scala-cli-repl.jpg)

The extension also facilitates [literate
programming](https://en.wikipedia.org/wiki/Literate_programming) using
[Org Mode](https://orgmode.org/), by letting the user experiment with
source blocks looking like the following.

``` org
#+begin_src scala :scala-version 3.0.0 :dep '("com.lihaoyi::os-lib:0.9.0")
println("This is:" + os.pwd)
#+end_src
#+end_src
```

In the above you can see that you can select the Scala version and
dependencies you need for your code.

The users who use [lsp-metals](https://github.com/emacs-lsp/lsp-metals)
can also enable lsp support within a source block to access utilities as
completion and navigation from within the Org Mode file.
