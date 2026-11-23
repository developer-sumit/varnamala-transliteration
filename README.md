<div align="center">

<img src="docs/images/logo.png" alt="Varnamala Logo" width="110" style="border-radius: 22px;"/>

# Varnamala (Version 1)

<p align="center">
  <strong>Transform Indian scripts into readable English and across Indic languages - 100% offline & private on-device AI.</strong>
</p>

[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2024%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-4285F4?style=flat-square&logo=jetpack-compose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![OCR Engine](https://img.shields.io/badge/OCR%20Engine-PaddleOCR%20(PP--OCRv6)-0052CC?style=flat-square)](https://github.com/PaddlePaddle/PaddleOCR)
[![ML Runtime](https://img.shields.io/badge/ML%20Inference-ONNX%20Runtime%20Mobile-005CED?style=flat-square)](https://onnxruntime.ai)
[![Transliteration](https://img.shields.io/badge/Transliteration-KMP%20%2B%20Aksharamukha-FF6B00?style=flat-square)](https://github.com/virtualvinodh/aksharamukha)
[![Build Tool](https://img.shields.io/badge/Build%20Tool-Gradle%209.8.2-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20UDF-blueviolet?style=flat-square)](https://developer.android.com/topic/architecture)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

<br/>

<img src="https://res.cloudinary.com/duagyqzv4/image/upload/v1767075072/projects/varnamala/v1/varnamala-cover-image_rvurvy.png" alt="Varnamala Banner & Showcase" width="100%" style="border-radius: 12px;"/>

<br/><br/>

### Download

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.sumit.varnamala" target="_blank" rel="noopener noreferrer">
    <img src="https://res.cloudinary.com/duagyqzv4/image/upload/v1763109969/icons/google-play-store_uqsqry.png" alt="Get it on Google Play" height="52" style="margin-right: 14px;"/>
  </a>
  <a href="https://www.indusappstore.com/app/com.sumit.varnamala" target="_blank" rel="noopener noreferrer">
    <img src="https://res.cloudinary.com/duagyqzv4/image/upload/v1763109969/icons/indus-app-store_dit0ok.png" alt="Get it on Indus Appstore" height="52"/>
  </a>
</p>

---

[Features](#-features) • [Screenshots](#-screenshots) • [Architecture](#-architecture) • [Aksharamukha Engine](#-aksharamukha-transliteration-engine) • [Getting Started](#-getting-started) • [AI Tooling](#-ai-agents--developer-tooling) • [License](#-license)

</div>

---

> [!NOTE]
> This repository contains the complete open-source codebase for **Varnamala Version 1**. Version 1 is fully functional, sanitized, and ready for community contributions and on-device research. Version 2 is actively under development in a separate repository.

---

## ✨ Features

### 🎯 Core Capabilities

- 📷 **Live Camera OCR**: Real-time scene text detection and recognition powered by PaddleOCR PP-OCRv6 ONNX models. Bounding boxes dynamically overlay with calculated font scaling and clean text clipping (no ellipses). Continuous camera preview without modal freezing.
- 🖼️ **Gallery & Image Detail Inspection**: Interactive zoom/pan viewer with 1:1 pixel-mapped OCR bounding boxes, tap-to-copy, and text-to-speech.
- 🔤 **Aksharamukha & KMP Transliteration**: Pure Kotlin Multiplatform engine with native Aksharamukha script conversion across 12+ Indic scripts and international Romanization schemes.
- ⌨️ **Text Transliteration**: Direct typing mode converting seamlessly across Indic scripts (Devanagari, Bengali, Tamil, Telugu, Kannada, Malayalam, Gujarati, Gurmukhi, Odia, Assamese, Urdu) and Roman scripts (IAST, ISO 15919, Harvard-Kyoto).
- 🔒 **Privacy & Biometric Guard**: 100% on-device processing. Scan history can be optionally secured via Biometrics (Fingerprint / Face Unlock) using `BiometricPrompt`.
- 🔊 **Offline Text-to-Speech**: Integrated Android TTS and neural Rasa TTS for pronunciation assistance.

---

## 📱 Screenshots

<div align="center">

| 📷 Live Camera OCR | 🖼️ Gallery & Zoom | ⌨️ Text Transliteration | ⚙️ Settings & Security |
| :---: | :---: | :---: | :---: |
| Point at text with live bounding box overlay | Pinch-to-zoom with 1:1 mapped OCR boxes | Type or paste text across Indic scripts | Custom font scale, themes, biometric lock |

</div>

---

## 🏛️ Architecture & Modules

The repository is modularized for clarity and reusability:

| Module | Documentation | Description |
| :--- | :--- | :--- |
| **`:app`** | [app/README.md](app/README.md) | Jetpack Compose Android UI, CameraX scanner, gallery viewer, DataStore settings, and biometric security. |
| **`:ppocr-sdk`** | [ppocr-sdk/README.md](ppocr-sdk/README.md) | On-device OCR engine using ONNX Runtime (Mobile), DBNet detection, Vatti polygon unclipping, and CTC recognizer. |
| **`:transliteration`** | [transliteration/README.md](transliteration/README.md) | Zero-dependency Kotlin Multiplatform (KMP) transliteration engine and integrated Aksharamukha script processor. |

---

## 🔤 Aksharamukha Transliteration Engine

Varnamala integrates the **Aksharamukha** transliteration paradigm directly into the `:transliteration` Kotlin Multiplatform module:

### Supported Scripts & Schemes
- **Indic Scripts**: Devanagari, Bengali, Assamese, Gurmukhi, Gujarati, Oriya, Tamil, Telugu, Kannada, Malayalam, Sinhala, Urdu.
- **Romanization Schemes**: IAST, ISO 15919, Harvard-Kyoto (HK), ITRANS, Velthuis, SLP1, WX.

### JSON Mapping Resources
All character matrices and mapping datasets are stored as standalone JSON files under [`transliteration/src/commonMain/resources/aksharamukha/`](transliteration/src/commonMain/resources/aksharamukha/):
- [`Devanagari.json`](transliteration/src/commonMain/resources/aksharamukha/Devanagari.json)
- [`Bengali.json`](transliteration/src/commonMain/resources/aksharamukha/Bengali.json)
- [`Assamese.json`](transliteration/src/commonMain/resources/aksharamukha/Assamese.json)
- [`Tamil.json`](transliteration/src/commonMain/resources/aksharamukha/Tamil.json)
- [`Telugu.json`](transliteration/src/commonMain/resources/aksharamukha/Telugu.json)
- [`Kannada.json`](transliteration/src/commonMain/resources/aksharamukha/Kannada.json)
- [`Malayalam.json`](transliteration/src/commonMain/resources/aksharamukha/Malayalam.json)
- [`Gujarati.json`](transliteration/src/commonMain/resources/aksharamukha/Gujarati.json)
- [`Gurmukhi.json`](transliteration/src/commonMain/resources/aksharamukha/Gurmukhi.json)
- [`Oriya.json`](transliteration/src/commonMain/resources/aksharamukha/Oriya.json)
- [`IAST.json`](transliteration/src/commonMain/resources/aksharamukha/IAST.json)
- [`ISO15919.json`](transliteration/src/commonMain/resources/aksharamukha/ISO15919.json)
- [`HK.json`](transliteration/src/commonMain/resources/aksharamukha/HK.json)
- [`ITRANS.json`](transliteration/src/commonMain/resources/aksharamukha/ITRANS.json)
- [`scripts.json`](transliteration/src/commonMain/resources/aksharamukha/scripts.json) (Catalog Index)

### Code Example
```kotlin
import com.sumit.varnamala.transliteration.Aksharamukha

// Direct script-to-script
val telugu = Aksharamukha.process("Devanagari", "Telugu", "नमस्ते")
// -> "నమస్తే"

// Roman HK to Devanagari
val devanagari = Aksharamukha.process("HK", "Devanagari", "kRSNa")
// -> "कृष्ण"

// Dynamic script matrix inspection
val devaMap = Aksharamukha.getScriptMapping("Devanagari")
```

---

## 🚀 Getting Started

### Prerequisites

- **JDK 17+**
- **Android SDK Platform 36**
- **Gradle 9.8.2** (included via Gradle Wrapper `./gradlew`)

### Build from Source

```bash
# Clone the repository
git clone https://github.com/developer-sumit/varnamala-transliteration.git
cd varnamala

# Build debug APK (out-of-the-box, no secrets needed)
./gradlew assembleDebug

# Run unit tests
./gradlew :transliteration:jvmTest
```

### Building Signed Release APKs

This open-source repository contains **no proprietary keystores or passwords**:
1. Copy `key.properties.template` to `key.properties`:
   ```bash
   cp key.properties.template key.properties
   ```
2. Fill in your release keystore path and passwords.
3. Run `./gradlew assembleRelease`. When `key.properties` is absent, release builds safely fall back without breaking.

---

## 🤖 AI Agents & Developer Tooling

This repository is designed for both human developers and autonomous AI coding agents:

- **AI Agent Skill**: [`.agents/skills/varnamala-development/SKILL.md`](.agents/skills/varnamala-development/SKILL.md) & [`skills/varnamala-development/SKILL.md`](skills/varnamala-development/SKILL.md) contain the definitive guidelines, architecture rules, and testing standards.

---

## 🤝 Contributing

Contributions are welcome! Please ensure:
1. No confidential credentials or keystores are committed.
2. Code follows Kotlin and Jetpack Compose best practices.
3. Tests pass: `./gradlew :transliteration:jvmTest`.
4. Run `bash scripts/check_secrets.sh` prior to submitting pull requests.

---

## 📜 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
