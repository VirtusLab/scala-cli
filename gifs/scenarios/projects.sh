#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  echo "println(1)" | scala-cli -S 2.13.6 -
  echo "//> using lib \"com.softwaremill.sttp.client3::core:3.3.18\" " | scala-cli -S 2.13.6  -
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clearConsole

  # Put your stuff here
  cat <<EOF | updateFile Post.scala
//> using lib "com.softwaremill.sttp.client3::core:3.3.18"
import sttp.client3._

// https://sttp.softwaremill.com/en/latest/quickstart.html

object Post extends App {
  val backend = HttpURLConnectionBackend()
  val response = basicRequest
    .body("Hello, world!")  
    .post(uri"https://httpbin.org/post?hello=world").send(backend)

  println(response.body)  
}
EOF

  doSleep 2

  pe "scala-cli Post.scala -S 2.13.6"

  # Wait a bit to read output of last command
  doSleep 5
  echo " " && echo "ok" > status.txt
fi