#!/usr/bin/env bash
set -euo pipefail

echo "======================================"
echo "Checking repository for secret leaks..."
echo "======================================"

FORBIDDEN_FILES=(
    "key.properties"
    "release.jks"
    "keystore/release.jks"
    "local.properties"
    ".remember"
    "kotlin-js-store"
)

LEAK_FOUND=0

# Check working tree for unignored sensitive files
for file in "${FORBIDDEN_FILES[@]}"; do
    if [ -f "$file" ]; then
        # Check if git tracks it
        if git ls-files --error-unmatch "$file" 2>/dev/null; then
            echo "❌ ERROR: Sensitive file '$file' is tracked by Git!"
            LEAK_FOUND=1
        fi
    fi
done

# Check git status for staged sensitive files
STAGED_SENSITIVE=$(git diff --cached --name-only | grep -E '\.(jks|keystore)$|key\.properties|local\.properties' || true)
if [ -n "$STAGED_SENSITIVE" ]; then
    echo "❌ ERROR: Staged sensitive files detected:"
    echo "$STAGED_SENSITIVE"
    LEAK_FOUND=1
fi

# Check git history for key.properties or keystore
HISTORY_LEAKS=$(git log --all --full-history -- "**/key.properties" "**/*.jks" "**/*.keystore" 2>/dev/null || true)
if [ -n "$HISTORY_LEAKS" ]; then
    echo "❌ ERROR: Historical commits contain sensitive files!"
    LEAK_FOUND=1
fi

if [ $LEAK_FOUND -eq 0 ]; then
    echo "✅ SUCCESS: No secrets, passwords, or private keystores detected in repository or git history."
    exit 0
else
    echo "❌ FAILED: Please remove sensitive files before open-sourcing or pushing."
    exit 1
fi
