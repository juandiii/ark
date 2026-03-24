#!/usr/bin/env bash
set -euo pipefail

# Determines the release version based on merged PR labels since the last tag.
# Outputs: RELEASE (version string) or SKIP=true if no release labels found.

PREV_TAG=$(git tag --sort=-v:refname | grep '^v' | head -1)

if [ -z "$PREV_TAG" ]; then
  # First release ever
  echo "RELEASE=1.0.0" >> "$GITHUB_OUTPUT"
  echo "SKIP=false" >> "$GITHUB_OUTPUT"
  echo "First release: 1.0.0"
  exit 0
fi

LAST_VERSION=${PREV_TAG#v}
IFS='.' read -r MAJOR MINOR PATCH <<< "$LAST_VERSION"
SINCE_DATE=$(git log -1 --format=%cI "$PREV_TAG")

# Get all merged PR labels since last tag
LABELS=$(gh pr list --state merged --base main --json labels,mergedAt --limit 200 | \
  jq -r --arg since "$SINCE_DATE" '.[] | select(.mergedAt > $since) | .labels[].name' 2>/dev/null || echo "")

echo "Labels found since $PREV_TAG: $LABELS"

# Priority: breaking change > feat > fix/perf
HAS_BREAKING=$(echo "$LABELS" | grep -c "breaking change" || true)
HAS_FEAT=$(echo "$LABELS" | grep -c "feat" || true)
HAS_PATCH=$(echo "$LABELS" | grep -cE "^(fix|perf)$" || true)

if [ "$HAS_BREAKING" -gt 0 ]; then
  RELEASE_VERSION="$((MAJOR + 1)).0.0"
elif [ "$HAS_FEAT" -gt 0 ]; then
  RELEASE_VERSION="$MAJOR.$((MINOR + 1)).0"
elif [ "$HAS_PATCH" -gt 0 ]; then
  RELEASE_VERSION="$MAJOR.$MINOR.$((PATCH + 1))"
else
  echo "No release labels (fix/feat/perf/breaking change) found. Skipping."
  echo "SKIP=true" >> "$GITHUB_OUTPUT"
  exit 0
fi

echo "RELEASE=$RELEASE_VERSION" >> "$GITHUB_OUTPUT"
echo "SKIP=false" >> "$GITHUB_OUTPUT"
echo "Release version: $RELEASE_VERSION"