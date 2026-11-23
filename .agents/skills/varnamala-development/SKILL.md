---
name: varnamala-development
description: Guidelines, architecture principles, and developer workflows for Varnamala Android codebase.
---

# Varnamala Development Skill

This skill provides comprehensive instructions for AI agents and human developers maintaining or extending **Varnamala (Version 1)**.

---

## 🏛️ Project Architecture & Modules

The repository is organized into three decoupled modules:

1. **`:transliteration`** (Kotlin Multiplatform):
   - Pure Kotlin with zero platform APIs or runtime network dependencies.
   - Implements phoneme/token-based transliteration between 12+ Indic scripts, Latin (readable and ISO 15919/IAST/Harvard-Kyoto), and Perso-Arabic (Urdu).
   - Contains the embedded **Aksharamukha** multi-script transliteration engine.
   - Android `:app` consumes the `jvm` target.

2. **`:ppocr-sdk`** (Android Library):
   - On-device optical character recognition using **PP-OCRv6** ONNX models via Microsoft ONNX Runtime Mobile.
   - Full pipeline: Preprocessing (CLAHE, unsharp mask), DBNet text detection, Vatti polygon unclipping, quad perspective warping, and CTC character decoding.

3. **`:app`** (Android Application):
   - Modern UI built with **Jetpack Compose** and Material Design 3.
   - CameraX real-time scanning with continuous background image analysis.
   - Full-image inspection with pinch-to-zoom and 1:1 aligned bounding boxes.
   - Biometric app lock gate (optional, strictly opt-in).
   - Local persistence via Preferences DataStore and local model cache.

---

## 🔒 Confidentiality & Open Source Compliance Rules

1. **Zero Secrets in Codebase**:
   - `key.properties`, `*.jks`, `*.keystore`, `local.properties`, and private certificates are strictly gitignored.
   - Never commit signing keys, passwords, or personal credentials.
   - For open-source release builds, use `key.properties.template` as a sanitized template.

2. **Sandbox Execution Notice**:
   - In sandboxed developer or CI environments where `/home/<user>` is read-only, always run Gradle with:
     ```bash
     ANDROID_USER_HOME=/tmp/.android ./gradlew <tasks>
     ```

---

## 🔤 Transliteration & Aksharamukha Conventions

- **Primary Entry Points**:
  - `TransliterationEngine.transliterate(text, target, source, scheme)`
  - `TransliterationEngine.aksharamukha(text, targetScript, sourceScript)`
  - `Aksharamukha.process(sourceScript, targetScript, text, preOptions, postOptions)`
  - `com.sumit.varnamala.utils.Transliterator` (Singleton bridge in `:app`)

- **Schwa Deletion & Ligatures**:
  - Indo-Aryan languages (Hindi, Marathi, Punjabi, Bengali, Gujarati, Odia) employ contextual schwa deletion when converted to Latin or Dravidian scripts.
  - Dravidian scripts (Tamil, Telugu, Kannada, Malayalam) preserve terminal viramas (*pulli*).

---

## 📷 OCR & Bounding Box Rules

- **Camera Preview Overlay**:
  - Scanning must remain interactive and live. Never freeze the camera preview with a modal bitmap.
  - Show detected text blocks dynamically via `OCROverlayCanvas`.
- **Bounding Box Font Scaling & Clipping**:
  - Calculate font sizes proportional to the detected text box height/width.
  - Never allow ellipsis (`...`) inside detected text boxes. Use `TextOverflow.Clip`.
- **Image Detail & Zoom Alignment**:
  - `AsyncImage` and `OCROverlayCanvas` must use identical `ContentScale.Fit` transformations.
  - Rotate normalized bounding box coordinates for 90°/270° EXIF orientations so boxes map 1:1 onto image pixels.

---

## 🧪 Testing & Verification

- JVM Tests for transliteration:
  ```bash
  ANDROID_USER_HOME=/tmp/.android ./gradlew :transliteration:jvmTest
  ```
- Android compilation:
  ```bash
  ANDROID_USER_HOME=/tmp/.android ./gradlew assembleDebug
  ```
- Secret audit:
  ```bash
  bash scripts/check_secrets.sh
  ```
