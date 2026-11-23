# 🔍 PaddleOCR Android SDK (`ppocr-sdk`)

An optimized on-device Android OCR library wrapping PaddlePaddle's **PP-OCRv6** / DBNet and CRNN/SVTR recognition models via Microsoft's **ONNX Runtime (Mobile)**.

---

## 🚀 Key Capabilities

- **100% Offline & Private**: Zero network calls; all neural network inference executes on-device.
- **Hardware Acceleration**: ONNX Runtime with CPU multithreading and optional NNAPI / XNNPACK execution providers.
- **Multi-Script Support**: Optimized for Indian language recognition (Devanagari, Tamil, Telugu, Bengali, etc.) alongside English.
- **Complete Pipeline**:
  - **Pre-processing**: Automatic normalization, CLAHE enhancement, and aspect-ratio preserving resizing.
  - **Text Detection**: DBNet (Real-time Scene Text Detection with Differentiable Binarization).
  - **Polygon Unclipping & Perspective Warping**: Vatti polygon unclip algorithms (`PolygonUnclip.kt`) and quad perspective cropping (`QuadTextCrop.kt`).
  - **Text Recognition**: CTC sequence decoding (`CTCDecoder.kt`) using vocabulary dictionary mappings.
  - **Image Enhancement**: Contrast balancing, sharpening, and adaptive thresholding (`ImageEnhancer.kt`).

---

## 📁 Architecture Overview

```
ppocr-sdk/src/main/java/com/paddle/ocr/
├── PaddleOCR.kt                # High-level developer entry point / facade
├── PaddleOCRConfig.kt          # Configuration options (detection threshold, max size, etc.)
├── EngineConfig.kt             # Runtime engine settings (threads, model paths)
├── engine/
│   ├── OCREngine.kt            # Core orchestrator coordinating detection & recognition
│   ├── DetectionEngine.kt      # DBNet text detector ONNX runner
│   ├── RecognitionEngine.kt    # Text recognizer ONNX runner
│   ├── ORTSessionManager.kt    # Thread-safe ONNX Runtime session manager
│   └── OCREngineResult.kt      # Detection/recognition raw output wrappers
├── preprocess/
│   ├── DetPreprocessor.kt      # Scales & normalizes inputs for DBNet (CHW tensors)
│   └── RecPreprocessor.kt      # Resizes cropped word/line images for CRNN/SVTR
├── postprocess/
│   ├── DBPostProcessor.kt      # Thresholding and contour extraction from probability map
│   ├── PolygonUnclip.kt        # Unclips detected bounding polygons
│   ├── QuadGeometry.kt         # Geometric transformations and bounding rectangle math
│   ├── QuadTextCrop.kt         # Perspective warp to extract rectified image patches
│   ├── CTCDecoder.kt           # CTC greedy decoder converting logits to text
│   └── BoxSorter.kt            # Top-to-bottom, left-to-right reading order sorter
├── model/
│   ├── OCRBox.kt               # Coordinates and polygon points for detected text box
│   ├── OCRResult.kt            # Structured result containing text, boxes, and confidence
│   └── OCRError.kt             # Error types and failure diagnostic codes
└── util/
    ├── BitmapUtils.kt          # Fast bitmap allocations, cropping, and color conversions
    ├── ImageEnhancer.kt        # CLAHE, unsharp mask, and binarization filters
    └── YamlUtils.kt            # PaddleOCR model metadata and config parser
```

---

## 🛠️ Integration Example

### 1. Initialize PaddleOCR

```kotlin
import com.paddle.ocr.PaddleOCR
import com.paddle.ocr.PaddleOCRConfig

val config = PaddleOCRConfig(
    detModelPath = "/path/to/det_model.onnx",
    recModelPath = "/path/to/rec_model.onnx",
    recDictPath = "/path/to/ppocr_keys_v1.txt",
    numThreads = 4,
    detDbThresh = 0.3f,
    detDbBoxThresh = 0.5f,
    detDbUnclipRatio = 1.6f
)

val ocr = PaddleOCR(context, config)
ocr.initialize()
```

### 2. Run Text Recognition

```kotlin
val bitmap: Bitmap = ... // Captured camera frame or gallery image

val result = ocr.recognize(bitmap)

result.boxes.forEach { box ->
    val text = box.text
    val confidence = box.confidence
    val points = box.points // 4 polygon corner points
    println("Detected '$text' ($confidence) at $points")
}

// Clean up resources when done
ocr.release()
```

---

## 📦 Model Requirements

- **Detection Model**: ONNX model accepting `[1, 3, H, W]` float input tensors with standard ImageNet mean/std normalization.
- **Recognition Model**: ONNX model taking cropped text lines `[1, 3, 48, W]` and emitting character logits.
- **Character Dictionary**: Plain UTF-8 text file listing supported characters line-by-line (e.g. `ppocr_keys_v1.txt`).
