# 🔤 Transliteration Engine (KMP)

A pure **Kotlin Multiplatform (KMP)** engine designed for high-performance, zero-dependency, 100% offline transliteration across Indic scripts, Latin romanization schemes, and Perso-Arabic (Urdu).

---

## 🌟 Highlights

- **Pure Kotlin / Zero Dependencies**: Operates entirely in-memory with zero platform or network dependencies.
- **Bi-directional Brahmic & Perso-Arabic**: Converts between 12+ Indic scripts, English/Latin, and Urdu.
- **Aksharamukha Engine Integration**: Full embedded Aksharamukha support for Indic scripts and standardized Romanization formats (IAST, ISO 15919, Harvard-Kyoto, ITRANS, Velthuis, SLP1, WX).
- **Phonetic IR (Intermediate Representation)**: Preserves linguistic nuances across language families:
  - Consonant clusters and conjuncts (`halant`/`virama`).
  - Inherent vowel retention vs. schwa deletion (Hindi vs. Sanskrit vs. Tamil).
  - Dravidian short vowels (*e*, *o*) vs. Indo-Aryan vowel systems.
  - Homorganic nasals, anusvara, and candrabindu.
  - Nukta consonants (Urdu/Hindi loanwords: *q, kh, gh, z, f*).
- **Signboard & Lexicon Caching**: In-memory dictionary support for bilingual signage, common acronyms, and place names.

---

## 📂 Architecture & Directory Structure

```
transliteration/
├── src/
│   ├── commonMain/kotlin/com/sumit/varnamala/transliteration/
│   │   ├── Aksharamukha.kt           # Aksharamukha multi-script transliteration engine
│   │   ├── TransliterationEngine.kt  # High-level orchestrator & multi-script run tokenizer
│   │   ├── Token.kt                  # Phonetic token definition & Token.Kind IR
│   │   ├── Language.kt               # Language definitions, ISO codes, and phonotactic flags
│   │   ├── LanguageDetector.kt       # Fast Unicode-range based script & language detector
│   │   ├── Script.kt                 # Unicode blocks & script character categorization
│   │   ├── IndicReader.kt            # Brahmic script parser -> Token stream
│   │   ├── IndicWriter.kt            # Token stream -> Brahmic script renderer
│   │   ├── IndicTables.kt            # Brahmic Unicode offset matrices
│   │   ├── LatinReader.kt            # Latin/English phoneme parser -> Token stream
│   │   ├── LatinWriter.kt            # Token stream -> Latin (IAST, ISO 15919, readable)
│   │   ├── UrduCodec.kt              # Perso-Arabic bidirectional converter
│   │   └── SignboardDictionary.kt    # Dictionary for domain-specific / signage transliterations
│   └── commonTest/kotlin/com/sumit/varnamala/transliteration/
│       ├── AksharamukhaTest.kt       # Aksharamukha script conversion test suite
│       └── TransliterationEngineTest.kt # Brahmic, schwa, and romanization tests
└── build.gradle.kts                  # KMP build configuration
```

---

## 🚀 Usage Guide

### 1. Unified Transliteration Engine

```kotlin
import com.sumit.varnamala.transliteration.TransliterationEngine
import com.sumit.varnamala.transliteration.Language
import com.sumit.varnamala.transliteration.RomanizationScheme

val engine = TransliterationEngine()

// Hindi (Devanagari) to Tamil
val tamil = engine.transliterate("नमस्ते", Language.TAMIL)
// -> "நமஸ்தே"

// Hindi to Romanized English (Readable scheme)
val readable = engine.transliterate("नमस्ते", Language.ENGLISH, scheme = RomanizationScheme.READABLE)
// -> "namaste"

// Hindi to Scientific ISO 15919
val scientific = engine.transliterate("कृष्ण", Language.ENGLISH, scheme = RomanizationScheme.ISO_15919)
// -> "kṛṣṇa"
```

### 2. Aksharamukha Engine

```kotlin
import com.sumit.varnamala.transliteration.Aksharamukha

// Direct script-to-script
val telugu = Aksharamukha.process(
    source = "Devanagari",
    target = "Telugu",
    text = "नमस्ते"
)
// -> "నమస్తే"

// Harvard-Kyoto (HK) Roman to Devanagari
val devanagari = Aksharamukha.process(
    source = "HK",
    target = "Devanagari",
    text = "kRSNa"
)
// -> "कृष्ण"

// Autodetect source script with post-options
val result = Aksharamukha.process(
    source = "autodetect",
    target = "Tamil",
    text = "भारत",
    postOptions = listOf("removeSchwa")
)
```

---

## 🧪 Testing

Run test targets via Gradle:

```bash
# Run JVM tests
./gradlew :transliteration:jvmTest

# Check all platforms
./gradlew :transliteration:check
```
