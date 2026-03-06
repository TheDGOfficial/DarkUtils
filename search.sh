#!/usr/bin/env bash

# Bash Strict Mode
set -eEuo pipefail
trap 'CODE=$?; echo "$0: Error on line $LINENO: $BASH_COMMAND"; exit $CODE' ERR
IFS=$'\n\t'

if [[ $# -eq 0 ]]; then
    echo "usage: $0 <pattern>"
    exit 1
fi

PATTERN=$1
EXIT_CODE=0

EXCLUDE_DIRS=(.idea .git .gradle build .vscode run logs)
EXCLUDE_FILES=(
  '*.md' LICENSE .gitattributes .gitignore gradlew
  '*.sh' '*.bat' '*.cmd' '*.jar' '*.csv' '*.patch'
)

if command -v rg >/dev/null 2>&1; then
    CMD=(rg -n --color always -e "$PATTERN")

    for d in "${EXCLUDE_DIRS[@]}"; do
        CMD+=(-g "!$d/**")
    done

    for f in "${EXCLUDE_FILES[@]}"; do
        CMD+=(-g "!$f")
    done

elif git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    CMD=(git grep -nE --color=always --untracked --exclude-standard "$PATTERN" --)

    for d in "${EXCLUDE_DIRS[@]}"; do
        CMD+=(":(exclude)$d/**")
    done

    for f in "${EXCLUDE_FILES[@]}"; do
        CMD+=(":(exclude)$f")
    done

else
    CMD=(grep -RInE --color=always -e "$PATTERN")

    for d in "${EXCLUDE_DIRS[@]}"; do
        CMD+=(--exclude-dir="$d")
    done

    for f in "${EXCLUDE_FILES[@]}"; do
        CMD+=(--exclude="$f")
    done

    CMD+=('--')
fi

"${CMD[@]}" || EXIT_CODE=$?

if [[ $EXIT_CODE -eq 2 ]]; then
    echo "search tool returned an error"
    exit 2
elif [[ $EXIT_CODE -eq 1 ]]; then
    echo "found no matches for pattern \"$PATTERN\""
    exit 0
fi

exit 1

