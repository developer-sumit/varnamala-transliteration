# 📝 Varnamala

<div align="center">

![Varnamala Banner](https://img.shields.io/badge/Varnamala-Transliteration%20App-6366f1?style=for-the-badge&logo=android&logoColor=white)

**Transform Indian scripts into readable English text,. completely offline!**

[![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat-square&logo=jetpack-compose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![PaddleOCR](https://img.shields.io/badge/PaddleOCR-0052CC?style=flat-square)](https://github.com/PaddlePaddle/PaddleOCR)

---

[Features](#-features) • [Installation](#-installation) • [How It Works](#-how-it-works) • [Supported Languages](#-supported-languages) • [Screenshots](#-screenshots) • [Contributing](#-contributing)

</div>

---

### Download

<div align="center">

[![Google Play](https://img.shields.io/badge/Google%20Play-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.sumit.varnamala)
[![Indus App Store](https://img.shields.io/badge/Indus%20App%20Store-FF6B00?style=for-the-badge&logo=android&logoColor=white)](https://www.indusappstore.com/app/com.sumit.varnamala)

</div>

---

## ✨ Features

### 🎯 Core Features

| Feature              | Description                                                                          |
| -------------------- | ------------------------------------------------------------------------------------ |
| 📷 **Camera OCR**    | Point your camera at any Indian language text and see it transliterated in real-time |
| 🖼️ **Gallery OCR**   | Select images from your gallery to extract and transliterate text                    |
| ⌨️ **Text Input**    | Type or paste text directly for instant transliteration                              |
| 🔄 **Bidirectional** | Transliterate from Indian scripts to English and vice versa                          |
| 📴 **Fully Offline** | Works without internet after initial setup. Your data stays on your device           |

### 🎨 User Experience

- 🌙 **Dark/Light Mode** - Choose your preferred theme
- 📏 **Adjustable Text Size** - Small, Normal, Large, or Extra Large
- 📋 **Auto-Copy** - Automatically copy transliterated text to clipboard
- 🔊 **Text-to-Speech** - Listen to the transliterated text
- 📳 **Haptic Feedback** - Subtle vibrations for better tactile response
- 📜 **History & Favorites** - Save and revisit your transliterations

### ⚙️ Advanced Features

- 🔦 **Flash Support** - Use camera flash in low-light conditions
- 🎛️ **Overlay Mode** - OCR any screen with floating button overlay
- 🗃️ **On-device OCR** - PaddleOCR detection and recognition models
- 💾 **Smart Caching** - Fast results for previously scanned images
- 📦 **Downloadable Models** - Download only the languages you need

---

## 💾 Installation

### Requirements

- Android 7.0 (API 24) or higher
- Camera permission for live OCR
- Storage permission for gallery access

### Build from Source

```bash
# Clone the repository
git clone https://github.com/developer-sumit/varnamala.git
cd varnamala

# Build debug APK
./gradlew assembleDebug

# Or build release APK (requires signing config)
./gradlew assembleRelease
```

---

### Technology Stack

| Component        | Technology                      |
| ---------------- | ------------------------------- |
| **UI Framework** | Jetpack Compose with Material 3 |
| **Language**     | Kotlin                          |
| **OCR Engine**   | PaddleOCR with ONNX Runtime     |
| **Camera**       | CameraX                         |
| **Storage**      | DataStore Preferences           |
| **Architecture** | MVVM with Compose               |

---

## 🌐 Supported Languages

Varnamala supports **12 major Indian languages**:

| Language     | Script     | Code | Native Name |
| ------------ | ---------- | ---- | ----------- |
| 🇮🇳 Hindi     | Devanagari | `hi` | हिन्दी      |
| 🇮🇳 Bengali   | Bengali    | `bn` | বাংলা       |
| 🇮🇳 Tamil     | Tamil      | `ta` | தமிழ்       |
| 🇮🇳 Telugu    | Telugu     | `te` | తెలుగు      |
| 🇮🇳 Marathi   | Devanagari | `mr` | मराठी       |
| 🇮🇳 Gujarati  | Gujarati   | `gu` | ગુજરાતી     |
| 🇮🇳 Kannada   | Kannada    | `kn` | ಕನ್ನಡ       |
| 🇮🇳 Malayalam | Malayalam  | `ml` | മലയാളം      |
| 🇮🇳 Punjabi   | Gurmukhi   | `pa` | ਪੰਜਾਬੀ      |
| 🇮🇳 Odia      | Odia       | `or` | ଓଡ଼ିଆ       |
| 🇮🇳 Assamese  | Bengali    | `as` | অসমীয়া     |
| 🇺🇸 English   | Latin      | `en` | English     |

---

## 📱 Screenshots

<div align="center">

|   Camera Mode   |     Gallery     |       Text Input        |    Settings     |
| :-------------: | :-------------: | :---------------------: | :-------------: |
|       📷        |       🖼️        |           ⌨️            |       ⚙️        |
| _Real-time OCR_ | _Browse images_ | _Type to transliterate_ | _Customize app_ |

</div>

---

## 🚀 Getting Started

### First Launch

1. **Choose Language** - Select your preferred app language
2. **Tutorial** - Follow the quick tutorial to learn the controls
3. **Grant Permissions** - Allow camera and storage access

### Basic Usage

1. **Camera Mode** - Point at text, tap capture or hold for continuous scan
2. **Gallery Mode** - Select an image to extract text
3. **Text Mode** - Type or paste text to transliterate

### Tips & Tricks

- 💡 **Long-press** on text to copy
- 💡 **Pinch** to zoom images
- 💡 **Swipe** between original and transliterated text
- 💡 **Star** important transliterations to save as favorites

---

## 📄 Documentation

- [Changelog](CHANGELOG.md) - Version history and changes
- [Future Updates](UPDATES.md) - Planned features and roadmap
- [Privacy Policy](https://sumitrathore.web.app/privacy/varnamala) - How we handle your data

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Report Bugs** - Open an issue with details
2. **Request Features** - Suggest improvements
3. **Submit PRs** - Fix bugs or add features
4. **Improve Translations** - Help localize the app

### Development Setup

```bash
# Prerequisites
- Android Studio Hedgehog or later
- JDK 11+
- Android SDK 36

# Clone and open in Android Studio
git clone https://github.com/developer-sumit/varnamala.git
```

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR) for offline OCR
- [Material Design 3](https://m3.material.io/) for design guidelines
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI

---

<div align="center">

⭐ Star this repo if you find it useful!

</div>
