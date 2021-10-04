#!/usr/bin/env scala-cli

import $ivy.`com.lihaoyi::os-lib:0.7.8`

println(os.list(os.pwd).mkString(", "))