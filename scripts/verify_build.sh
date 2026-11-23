#!/usr/bin/env bash
set -euo pipefail

echo "=============================================="
echo "🚀 Varnamala v1 Verification & Build Script"
echo "=============================================="

export ANDROID_USER_HOME="${ANDROID_USER_HOME:-/tmp/.android}"
mkdir -p "$ANDROID_USER_HOME"

echo "1. Checking repository for confidential files / leaks..."
bash scripts/check_secrets.sh

echo "2. Running Transliteration & Aksharamukha unit tests..."
./gradlew :transliteration:jvmTest --stacktrace

echo "3. Compiling SDK and Android App..."
./gradlew compileDebugKotlin --stacktrace

echo "=============================================="
echo "✅ All verification checks passed successfully!"
echo "=============================================="
