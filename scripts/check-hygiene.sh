#!/usr/bin/env bash
# Fails when the repo would publish something it shouldn't: AI session links or attribution
# trailers, local working notes, extra terms from the HYGIENE_EXTRA_PATTERNS variable (one
# extended regex, kept out of the code), or a GIF over 10 MB.
# Usage: scripts/check-hygiene.sh [<commit range>]   (the range's messages are checked too)
set -euo pipefail
cd "$(dirname "$0")/.."

patterns='claude\.ai/code/session|Co-Authored-By: Claude|Claude-Session:'
if [[ -n "${HYGIENE_EXTRA_PATTERNS:-}" ]]; then patterns="$patterns|$HYGIENE_EXTRA_PATTERNS"; fi
failed=0

# This script and its workflow name the patterns, so they're the only files not searched.
if git grep -nIiE "$patterns" -- . ':!scripts/check-hygiene.sh' ':!.github/workflows/hygiene.yml'; then
  echo "::error::Tracked files contain text that shouldn't be published (matches above)."
  failed=1
fi

if git ls-files | grep -E '^(docs/superpowers|\.superpowers)/'; then
  echo "::error::Local working notes are tracked; remove them with git rm --cached."
  failed=1
fi

while IFS= read -r gif; do
  size=$(wc -c < "$gif")
  if (( size > 10 * 1024 * 1024 )); then
    echo "::error file=$gif::$gif is $((size / 1024 / 1024)) MB; keep README GIFs under 10 MB."
    failed=1
  fi
done < <(git ls-files '*.gif')

if [[ -n "${1:-}" ]]; then
  if git log --format='%H%n%B' "$1" | grep -iE "$patterns"; then
    echo "::error::Commit messages in $1 contain text that shouldn't be published (matches above)."
    failed=1
  fi
fi

if (( failed == 0 )); then echo "Hygiene check passed."; fi
exit "$failed"
