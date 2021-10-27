#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  echo "println(1)" | scala-cli -

  cat <<EOF > demo.test.scala | 
// using lib "org.scalameta::munit:0.7.29"
EOF
  scala-cli test demo.test.scala
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clearConsoleConsole

  cat <<EOF | updateFile demo.scala
@main def demo(args: String *) = 
  println(args.mkStrink) // Oops, a typo!
EOF

  pe "scala-cli compile demo.scala" || true

  doSleep 5

  clearConsoleConsole

  cat <<EOF | updateFile demo.scala
def niceArgs(args: String*): String = 
  args.map(_.capitalize).mkString("Hello: ", ", ", "!")

@main def demo(args: String*) = println(niceArgs(args*))
EOF

  pe "scala-cli demo.scala -- Ala jake Mike" || echo "FAILED!"
  
  doSleep 5

  clearConsoleConsole

  cat <<EOF | updateFile demo.test.scala
// using lib "org.scalameta::munit:0.7.29"

class demoTest extends munit.FunSuite {
  test("test nice args") {
    assert(clue(niceArgs("a", "b")) == "Hello: A, B!")
  }
  test("test empty arguments") {
    assert(clue(niceArgs()) == "Hello!")
  }
}
EOF

  pe "scala-cli test demo.scala demo.test.scala" || true

  # Wait a bit to read output of last command
  doSleep 5
  echo " " && echo "ok" > status.txt
fi