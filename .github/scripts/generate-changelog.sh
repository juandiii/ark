#!/usr/bin/env bash
set -euo pipefail

# Generates a CHANGELOG.md entry from merged PRs since the last tag.
# Usage: ./generate-changelog.sh <version> <repo>

VERSION=$1
REPO=$2
DATE=$(date +%Y-%m-%d)
PREV_TAG=$(git tag --sort=-v:refname | grep '^v' | head -1)

if [ -z "$PREV_TAG" ]; then
  SINCE_DATE="2000-01-01"
else
  SINCE_DATE=$(git log -1 --format=%cI "$PREV_TAG")
fi

# Get merged PRs grouped by label
PRS_JSON=$(gh pr list --state merged --base main --json number,title,labels,mergedAt,author --limit 200 | \
  jq --arg since "$SINCE_DATE" '[.[] | select(.mergedAt > $since)]')

ENTRY="## [v${VERSION}](https://github.com/${REPO}/releases/tag/v${VERSION}) — ${DATE}"$'\n\n'

# Categorize PRs by highest priority label (no duplicates)
# Each PR appears in only one category based on priority: breaking > feat > fix > perf > other
BREAKING=$(echo "$PRS_JSON" | jq -r '[.[] | select([.labels[].name] | any(. == "breaking change"))] | unique_by(.number)[] | "- \(.title) (#\(.number)) @\(.author.login)"' 2>/dev/null || echo "")
if [ -n "$BREAKING" ]; then
  ENTRY+=$'### ⚠️ Breaking Changes\n\n'"$BREAKING"$'\n\n'
fi

FEATS=$(echo "$PRS_JSON" | jq -r '[.[] | select(([.labels[].name] | any(. == "feat")) and ([.labels[].name] | any(. == "breaking change") | not))] | unique_by(.number)[] | "- \(.title) (#\(.number)) @\(.author.login)"' 2>/dev/null || echo "")
if [ -n "$FEATS" ]; then
  ENTRY+=$'### ✨ Features\n\n'"$FEATS"$'\n\n'
fi

FIXES=$(echo "$PRS_JSON" | jq -r '[.[] | select(([.labels[].name] | any(. == "fix")) and ([.labels[].name] | any(. == "feat" or . == "breaking change") | not))] | unique_by(.number)[] | "- \(.title) (#\(.number)) @\(.author.login)"' 2>/dev/null || echo "")
if [ -n "$FIXES" ]; then
  ENTRY+=$'### 🐛 Bug Fixes\n\n'"$FIXES"$'\n\n'
fi

PERFS=$(echo "$PRS_JSON" | jq -r '[.[] | select(([.labels[].name] | any(. == "perf")) and ([.labels[].name] | any(. == "fix" or . == "feat" or . == "breaking change") | not))] | unique_by(.number)[] | "- \(.title) (#\(.number)) @\(.author.login)"' 2>/dev/null || echo "")
if [ -n "$PERFS" ]; then
  ENTRY+=$'### ⚡ Performance\n\n'"$PERFS"$'\n\n'
fi

OTHERS=$(echo "$PRS_JSON" | jq -r '[.[] | select([.labels[].name] | any(. == "fix" or . == "feat" or . == "perf" or . == "breaking change") | not)] | unique_by(.number)[] | "- \(.title) (#\(.number)) @\(.author.login)"' 2>/dev/null || echo "")
if [ -n "$OTHERS" ]; then
  ENTRY+=$'### 📦 Other Changes\n\n'"$OTHERS"$'\n\n'
fi

ENTRY+="---"$'\n\n'

# Prepend to CHANGELOG.md
if [ -f CHANGELOG.md ]; then
  echo -e "${ENTRY}$(cat CHANGELOG.md)" > CHANGELOG.md
else
  echo -e "# Changelog\n\n${ENTRY}" > CHANGELOG.md
fi

echo "Changelog updated for v${VERSION}"