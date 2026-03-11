#!/usr/bin/env bash

# Bash Strict Mode
set -eEuo pipefail
trap 'CODE=$?; echo "$0: Error on line $LINENO: $BASH_COMMAND"; exit $CODE' ERR
IFS=$'\n\t'

if [[ $# -eq 0 ]]; then
    echo "usage: $0 <version number>"
    echo "example: $0 1.3.0"
    exit 1
fi

VERSION="v$1"

# Run todo.sh first
if ./todo.sh; then
    # no TODOs found
    echo "No TODOs/FIXMEs found."
else
    echo "WARNING: TODOs/FIXMEs exist!"
    read -p "Proceed with tag '$VERSION' anyway? [y/N] " confirm
    confirm=${confirm,,}
    if [[ "$confirm" != "y" && "$confirm" != "yes" ]]; then
        echo "Aborting tag creation."
        exit 0
    fi
fi

# Confirmation before creation
read -p "Are you sure you want to create the tag '$VERSION'? [y/N] " confirm
confirm=${confirm,,}  # convert to lowercase
if [[ "$confirm" != "y" && "$confirm" != "yes" ]]; then
    echo "Aborting tag creation."
    exit 0
fi

git tag -s "$VERSION" -m "Release version $VERSION"

# Confirmation before pushing
read -p "Are you sure you want to push the tag '$VERSION' to origin? [y/N] " confirm
confirm=${confirm,,}  # convert to lowercase
if [[ "$confirm" != "y" && "$confirm" != "yes" ]]; then
    echo "Aborting tag push."
    exit 0
fi

git push origin "$VERSION"

