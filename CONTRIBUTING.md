# Contributing to Varnamala

Thank you for your interest in contributing to Varnamala. This project provides on-device, offline OCR and script transliteration across Indic languages.

---

## 1. Guiding Principles

- **100% On-Device & Offline**: Varnamala performs all text detection, recognition, and transliteration entirely on the user device. No remote telemetry, analytics, or network inference calls are permitted.
- **Privacy First**: User camera frames, scans, and inputs stay local. Sensitive features like scan history can be protected with biometric authentication.
- **Zero Secrets**: Never commit keystores, signing credentials, API tokens, or secrets. Every contribution must pass `bash scripts/check_secrets.sh`.
- **Clean Formatting**: Maintain consistent Kotlin and Jetpack Compose style without em-dash punctuation.

---

## 2. Setting Up Your Development Environment

- **JDK**: Java 17 or Java 21 recommended.
- **Android SDK**: Compile SDK 37 (Android 15+).
- **IDE**: Android Studio Ladybug or newer.

Clone the repository and build debug artifacts:
```bash
./gradlew assembleDebug
```

---

## 3. Architecture Overview

- **`:app`**: Android application module containing the Jetpack Compose user interface, CameraX integration, gallery viewer, DataStore preferences, and biometric security gates.
- **`:ppocr-sdk`**: Android library encapsulating the PaddleOCR ONNX Runtime pipeline (text detection via DBNet, polygon processing, and CTC recognition).
- **`:transliteration`**: Multiplatform Kotlin module implementing the Aksharamukha script conversion engine and Indic transliteration catalogs.

---

## 4. Testing & Verification

Before submitting changes, run all test suites and security checks locally:

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run transliteration multiplatform tests
./gradlew :transliteration:jvmTest

# Verify no secrets or credentials exist in the tree
bash scripts/check_secrets.sh
```

---

## 5. Pull Request Guidelines

1. Create a focused branch for your bug fix or feature (`git checkout -b feature/your-feature-name`).
2. Keep pull requests scoped to a single logical improvement.
3. Ensure all Gradle tasks and unit tests pass before opening a PR.
4. Provide a clear description of the problem solved and steps to test.
