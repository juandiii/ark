#!/usr/bin/env bash
set -euo pipefail

# Determines the release version based on PR labels from a milestone.
# Env vars: BUMP_OVERRIDE (auto/patch/minor/major), MILESTONE (milestone name)
# Outputs: RELEASE (version string) or SKIP=true if no release labels found.

BUMP=${BUMP_OVERRIDE:-auto}
MILESTONE=${MILESTONE:-}

PREV_TAG=$(git tag --sort=-v:refname | grep '^v' | head -1 || echo "")

if [ -z "$PREV_TAG" ]; then
  echo "RELEASE=1.0.0" >> "$GITHUB_OUTPUT"
  echo "SKIP=false" >> "$GITHUB_OUTPUT"
  echo "First release: 1.0.0"
  exit 0
fi

LAST_VERSION=${PREV_TAG#v}
IFS='.' read -r MAJOR MINOR PATCH <<< "$LAST_VERSION"

# Manual override — skip label detection
if [ "$BUMP" != "auto" ]; then
  case "$BUMP" in
    patch) RELEASE_VERSION="$MAJOR.$MINOR.$((PATCH + 1))" ;;
    minor) RELEASE_VERSION="$MAJOR.$((MINOR + 1)).0" ;;
    major) RELEASE_VERSION="$((MAJOR + 1)).0.0" ;;
  esac
  echo "RELEASE=$RELEASE_VERSION" >> "$GITHUB_OUTPUT"
  echo "SKIP=false" >> "$GITHUB_OUTPUT"
  echo "Manual override ($BUMP): $RELEASE_VERSION"
  exit 0
fi

# Auto: detect from PR labels in milestone
if [ -z "$MILESTONE" ]; then
  echo "❌ Milestone is required for auto bump detection."
  echo "SKIP=true" >> "$GITHUB_OUTPUT"
  exit 0
fi

LABELS=$(gh pr list --state merged --base main --search "milestone:\"$MILESTONE\"" \
  --json labels --limit 200 | \
  jq -r '.[].labels[].name' 2>/dev/null || echo "")

echo "Labels found in milestone '$MILESTONE': $LABELS"

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
  echo "No release labels (fix/feat/perf/breaking change) found in milestone '$MILESTONE'. Skipping."
  echo "SKIP=true" >> "$GITHUB_OUTPUT"
  exit 0
fi

echo "RELEASE=$RELEASE_VERSION" >> "$GITHUB_OUTPUT"
echo "SKIP=false" >> "$GITHUB_OUTPUT"
echo "Release version: $RELEASE_VERSION"
