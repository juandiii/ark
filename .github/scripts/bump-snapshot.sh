#!/usr/bin/env bash
set -euo pipefail

# Bumps the project to the next SNAPSHOT version after a release.
# Usage: ./bump-snapshot.sh <release-version>

RELEASE_VERSION=$1
IFS='.' read -r MAJOR MINOR PATCH <<< "$RELEASE_VERSION"

git config user.name "github-actions[bot]"
git config user.email "github-actions[bot]@users.noreply.github.com"
NEXT="$MAJOR.$MINOR.$((PATCH + 1))-SNAPSHOT"

./mvnw versions:set -DnewVersion="$NEXT" -DgenerateBackupPoms=false
git add -A
git commit -m "chore(release): prepare $NEXT [no deploy]"
git push origin main

echo "Bumped to $NEXT"