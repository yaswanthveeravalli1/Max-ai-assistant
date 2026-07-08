#!/bin/bash
set -e

mkdir -p app/src/main/java/com/example/core
mkdir -p app/src/main/java/com/example/ai
mkdir -p app/src/main/java/com/example/automation/engine
mkdir -p app/src/main/java/com/example/automation/actions
mkdir -p app/src/main/java/com/example/voice

# We will move and recreate files according to the architecture.
# Let's echo a simple message for now to test bash execution.
echo "Directories created."
