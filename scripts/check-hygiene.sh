#!/usr/bin/env bash
# Fails when the repo would publish something it shouldn't: AI session links or attribution
# trailers, local working notes, extra terms from the HYGIENE_EXTRA_PATTERNS variable (one
# extended regex, kept out of the code), or a GIF over 10 MB. A pull request's title and
# description, given in PR_TEXT, are held to the same patterns. Also fails when the README
# renders wrong: a code block left open swallows the sections after it, and a link to a heading
# that isn't there goes nowhere.
# Usage: scripts/check-hygiene.sh [<commit range>]   (the range's messages are checked too)
set -euo pipefail
cd "$(dirname "$0")/.."

patterns='claude\.ai/code/session|Co-Authored-By: Claude|Claude-Session:|Generated with \[Claude Code\]'
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

if [[ -n "${PR_TEXT:-}" ]] && grep -iE "$patterns" <<< "$PR_TEXT"; then
  echo "::error::The pull request's title or description contains text that shouldn't be published (matches above)."
  failed=1
fi
# The README as GitHub renders it: every ``` block closes before the next heading, and every
# link to a heading in it (#…) matches one, with GitHub's anchors (lower case, punctuation but
# - and _ dropped, spaces as -, a repeat getting -1, -2…).
if ! python3 - README.md <<'PY'
import re, sys
text = open(sys.argv[1], encoding="utf-8").read()
inside, anchors, seen, ok = False, set(), {}, True
for n, line in enumerate(text.split("\n"), 1):
    if line.startswith("```"):
        inside = not inside
        continue
    heading = re.match(r"#{1,6} (.*)", line)
    if heading and inside:
        print(f"::error file=README.md,line={n}::a code block is still open at this heading")
        ok, inside = False, False
    if heading and not inside:
        slug = re.sub(r"[^\w\- ]", "", heading.group(1).strip().lower()).replace(" ", "-")
        count = seen.get(slug, 0)
        seen[slug] = count + 1
        anchors.add(slug if count == 0 else f"{slug}-{count}")
if inside:
    print("::error file=README.md::the last code block never closes")
    ok = False
for link in sorted(set(re.findall(r"\]\(#([^)]+)\)", text)) - anchors):
    print(f"::error file=README.md::#{link} matches no heading")
    ok = False
sys.exit(0 if ok else 1)
PY
then
  failed=1
fi
if (( failed == 0 )); then echo "Hygiene check passed."; fi
exit "$failed"
