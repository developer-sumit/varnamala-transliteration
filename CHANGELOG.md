# Changelog

All notable changes to Varnamala are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.4.0] - 2026-11-23

### 📸 Camera & Image Viewer Enhancements

- **Interactive Pinch-to-Zoom & Pan**: Added double-tap and two-finger pinch-to-zoom (1x to 6x) on captured photos with 1:1 mapped OCR bounding box overlays.
- **Smart Back Navigation**: Back gesture or button on camera screen safely dismisses the captured image and returns to live preview instead of exiting the app.
- **Full OCR Text Display**: Dynamic font auto-scaling and proportional bounds clipping prevent text truncation ("...") and ensure words fit cleanly inside bounding boxes.

### 💾 Session State Persistence

- **Screen State Retention**: Preserves user input and transliteration results on the text screen across bottom navigation tabs, settings, and screen transitions.
- **Camera Session Preservation**: Retains captured photo, recognized text, and zoom/pan state when changing transliteration source/target languages or app UI language.
- **Live Transliteration Switching**: Changing target transliteration language on a captured photo dynamically re-transliterates all bounding boxes without needing to retake the photo.
- **Duplicate History Prevention**: Prevents redundant history entries and duplicate thumbnail files when re-entering or switching between screens.

### 🔤 Aksharamukha Transliteration Engine

- **Aksharamukha Integration**: Complete Kotlin Multiplatform script conversion engine supporting 12+ Indic scripts and international Roman schemes (Readable English, IAST, ISO 15919, Harvard-Kyoto).
- **Clean English Mapping**: Added Readable ASCII transliteration (`READABLE_MAP`), Sinhala script support, and bidirectional Urdu conversion.
- **Natural Dental Mapping**: Disambiguated reverse transliteration tables to prioritize natural dental consonants over retroflex consonants (e.g., "namaste" converts cleanly to "नमस्ते").

### 🛠️ Open Source & DevOps

- **Automated Multi-ABI Release Workflow**: GitHub Actions workflow (`build-release.yml`) compiles signed APKs (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`, `universal`) and creates GitHub Releases upon pushing version tags.
- **Community Governance**: Added `CONTRIBUTING.md`, `SECURITY.md`, PR template, and GitHub issue templates.
- **Modern Build Tooling**: Upgraded to AGP 9.0 built-in Kotlin, Android SDK 37, Compose 1.13, etc

---

## [1.3.0] - 2026-01-27

### 🎨 Major UI Overhaul

- **Complete UI Redesign** - Modern, polished interface with improved visual consistency across all screens
- **Bottom Navigation Bar** - New navigation component for seamless screen transitions
- **Enhanced Layouts** - Refined spacing, alignment, and visual hierarchy throughout the app

### ✨ Added

- **Information Dialogs** - Helpful tip dialogs across all screens (Camera, Gallery, Text Input, Models, Language, History, Settings)
- **Micro Animations** - Smooth entry animations for list items providing better visual feedback
- **New Icons** - Beautiful new drawable resources including camera icons, switch camera, and enhanced keyboard icon
- **Model Delete Dialog** - Confirmation dialog with option to delete downloaded OCR models

### 🐛 Fixed

- Various bug fixes for improved stability
- Code optimization for better performance
- Cleaned up unnecessary code and improved code quality

### 🔧 Technical

- Updated versionCode to 5 and versionName to "1.3.0"
- Simplified flow mapping for app language and camera tutorial checks
- Refactored clipboard labels to use string resources consistently
- Enhanced drawable resources with new vector graphics
- Extended string resources for dialogs and tutorials in all 12 supported languages

---

## [1.2.0] - 2026-01-17

### ✨ Added

- **Text Input Mode** - New screen for manual text entry transliteration without camera
- **History & Favorites** - Track transliteration history with ability to star favorites
- **Text-to-Speech (TTS)** - Listen to transliterated text with built-in TTS support
- **Adjustable Text Size** - Choose between Small, Normal, Large, and Extra-Large display sizes
- **Haptic Feedback** - Subtle vibrations for better tactile response on actions
- **Auto-Copy** - Option to automatically copy transliterated text to clipboard
- **Quick Access to History** - Navigate to history from Settings

### 🎨 Improved

- **Settings Organization** - New "User Experience" section for personalization options
- **Navigation** - Smoother slide animations between screens
- **Code Architecture** - Better separation of user preferences into dedicated repository
- **Documentation** - Comprehensive README with installation guide and feature descriptions

### 🔧 Technical

- Added `UserPreferencesRepository` for managing UX settings
- Added `TransliterationHistoryRepository` for history management
- New drawable assets for heart, copy, volume, keyboard, and history icons
- Extended string resources for all new features (English localization)

---

## [1.1.0] - 2026-01-10

### ✨ Added

- Offline OCR model management for on-device text recognition
- Camera OCR settings with saved preferences
- Settings options for overlay and screen capture
- Privacy policy link in Settings
- Onboarding/tutorial strings (localized)

### 🎨 Improved

- OCR results UI: toggle between original text and transliterated text
- Transliteration rules and dictionary updates (including Gujarati and Kannada)
- Multilingual string coverage and general UI polish (including launcher icon updates)

---

## [1.0.0] - 2026-01-01

### 🎉 Initial Release

- Camera-based transliteration for Indian languages
- Real-time transliteration using the device camera
- Image-based transliteration from gallery
- Supports multiple Indian scripts and English
- Offline-first functionality after setup
- Simple and minimal user interface

### 🌐 Supported Languages (12)

- Hindi (हिन्दी)
- Bengali (বাংলা)
- Tamil (தமிழ்)
- Telugu (తెలుగు)
- Marathi (मराठी)
- Gujarati (ગુજરાતી)
- Kannada (ಕನ್ನಡ)
- Malayalam (മലയാളം)
- Punjabi (ਪੰਜਾਬੀ)
- Odia (ଓଡ଼ିଆ)
- Assamese (অসমীয়া)
- English

---

## Version History Summary

| Version | Date       | Highlights                                |
| ------- | ---------- | ----------------------------------------- |
| 1.4.0   | 2026-11-23 | Pinch-to-zoom, State persistence, Aksharamukha engine |
| 1.3.0   | 2026-01-27 | Complete UI redesign, Info dialogs, Animations |
| 1.2.0   | 2026-01-17 | Text input, History, TTS, UX improvements |
| 1.1.0   | 2026-01-10 | Offline OCR, Settings, Tutorial           |
| 1.0.0   | 2026-01-01 | Initial release                           |
