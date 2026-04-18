#!/usr/bin/env bash

# Bash Strict Mode
set -eEuo pipefail
trap 'CODE=$?; echo "$0: Error on line $LINENO: $BASH_COMMAND"; exit $CODE' ERR
IFS=$'\n\t'

# Grep all todo or fixme containing files
if ./search.sh "TODO|FIXME"; then
    exit 0
else
    exit 1
fi

