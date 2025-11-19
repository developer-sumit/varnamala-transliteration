package com.sumit.varnamala.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sumit.varnamala.data.ocr.OCRResult
import com.sumit.varnamala.ocr.mlkit.MLKitOCRRepository
import com.sumit.varnamala.data.ocr.OcrEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OCRViewModel(private val context: Context) : ViewModel() {
    // OCR repositories
    private val mlKitRepository = MLKitOCRRepository(context)
    
    // State
    private val _ocrState = MutableStateFlow<OCRState>(OCRState.Idle)
    val ocrState: StateFlow<OCRState> = _ocrState
    
    // Result
    private val _ocrResult = MutableStateFlow<OCRResult?>(null)
    val ocrResult: StateFlow<OCRResult?> = _ocrResult
    
    // Current engine
    private val _currentEngine = MutableStateFlow(OcrEngine.ML_KIT)
    val currentEngine: StateFlow<OcrEngine> = _currentEngine
    
    // Current language
    private val _currentLanguage = MutableStateFlow("auto")
    val currentLanguage: StateFlow<String> = _currentLanguage
    
    /**
     * Initialize any necessary resources (placeholder for future use)
     */
    fun initialize() {
        // No initialization needed for ML Kit
    }
    
    /**
     * Process an image for OCR
     */
    fun processImage(uri: Uri, language: String = "auto") {
        _ocrState.value = OCRState.Processing
        _currentLanguage.value = language
        
        viewModelScope.launch {
            try {
                val result = if (language == "auto") {
                    mlKitRepository.processImageWithAutoDetection(uri)
                } else {
                    mlKitRepository.processImageFromUri(uri, language)
                }
                
                _ocrResult.value = result
                _ocrState.value = OCRState.Success
            } catch (e: Exception) {
                _ocrState.value = OCRState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Get available languages for ML Kit
     */
    fun getAvailableLanguages(): List<String> {
        return mlKitRepository.getAvailableLanguages()
    }
    
    /**
     * Get display name for language code
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return mlKitRepository.getLanguageDisplayName(languageCode)
    }
    
    /**
     * Process an image for OCR with specific engine parameters
     * For backward compatibility with existing screens
     */
    fun processImage(uri: Uri, engine: OcrEngine, language: String) {
        // Always use ML Kit regardless of the engine parameter
        processImage(uri, language)
    }
    
    /**
     * Change language
     */
    fun setLanguage(language: String) {
        viewModelScope.launch {
            _currentLanguage.value = language
        }
    }
    
    /**
     * Reset OCR state
     */
    fun reset() {
        _ocrState.value = OCRState.Idle
        _ocrResult.value = null
    }
    
    /**
     * Clean up resources
     */
    override fun onCleared() {
        // No cleanup needed for ML Kit
        super.onCleared()
    }
}

/**
 * Factory for creating OCRViewModel
 */
class OCRViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OCRViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OCRViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * OCR processing state
 */
sealed class OCRState {
    object Idle : OCRState()
    object Processing : OCRState()
    object Success : OCRState()
    data class Error(val message: String) : OCRState()
}