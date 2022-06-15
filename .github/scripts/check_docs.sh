#!/usr/bin/env bash
set -e

dest="$(pwd)/out/sclicheck/bin"

./mill copyTo cli.launcher "$dest/scala-cli.sh"

# work around sh issues (sh ignores PATH entries with '+' or '%', and the cs-provided Java 17 entry has one)
# so we try to run the scala-cli launcher with bash instead
cat > "$dest/scala-cli" << EOF
#!/usr/bin/env bash
exec bash "$dest/scala-cli.sh" "\$@"
EOF
chmod +x "$dest/scala-cli"

echo "Adding $dest to PATH"
export PATH="$dest:$PATH"

if [ $# -eq 0 ] 
then
    toCheck=("website/docs/cookbooks" "website/docs/commands")
else
    toCheck=("$@")
fi

statusFile="$(pwd)/out/sclicheck/.status"
scala-cli sclicheck/sclicheck.scala -- --status-file "$statusFile" "${toCheck[@]}" || (
  echo "Checking documentation failed. To run tests locally run '.github/scripts/check_docs.sh <failing_file>'"
  echo "You can find more about automatic documentaiton testing in sclicheck/Readme.md file."
  exit 1
)

test -f "$statusFile" || ( 
  echo "Fatal error. Status file: $statusFile does not exists what signal problem with running tests."
  exit 1
)
