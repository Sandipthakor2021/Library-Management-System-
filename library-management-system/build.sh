#!/usr/bin/env bash
# Builds and tests the project with plain javac (JDK 17+). No dependencies required.
#   ./build.sh          compile + run tests
#   ./build.sh demo     compile + run the demo
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out out-test && mkdir out out-test
javac -d out $(find src/main -name '*.java')
javac -cp out -d out-test $(find src/test -name '*.java')
if [ "${1:-}" = "demo" ]; then
  java -cp out com.library.Main "${@:2}"
else
  java -cp out:out-test com.library.LibrarySystemTest
fi
