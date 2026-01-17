# Changelog

All notable changes to Varnamala are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

- Offline OCR model management for on-device text recognition (Tesseract)
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
| 1.2.0   | 2026-01-17 | Text input, History, TTS, UX improvements |
| 1.1.0   | 2026-01-10 | Offline Tesseract, Settings, Tutorial     |
| 1.0.0   | 2026-01-01 | Initial release                           |
