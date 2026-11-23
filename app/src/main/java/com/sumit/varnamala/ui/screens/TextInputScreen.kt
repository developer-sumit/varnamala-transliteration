package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Language
import com.sumit.varnamala.data.HistoryEntry
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.data.TextInputSession
import com.sumit.varnamala.data.TransliterationLanguageCatalog
import com.sumit.varnamala.data.TransliterationHistoryRepository
import com.sumit.varnamala.data.preferences.OCRSettingsRepository
import com.sumit.varnamala.data.preferences.UserPreferencesRepository
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.ui.components.InfoBottomSheet
import com.sumit.varnamala.ui.components.LanguageSelectionRow
import com.sumit.varnamala.transliteration.LanguageDetector
import com.sumit.varnamala.utils.Transliterator
import com.sumit.varnamala.tts.SpeechPart
import com.sumit.varnamala.ui.components.HighlightedText
import com.sumit.varnamala.ui.components.SpeakButton
import com.sumit.varnamala.ui.components.rememberSpeechController
import com.sumit.varnamala.ui.components.speakableHighlight as spokenWordHighlight
import com.sumit.varnamala.ui.components.speechLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun TextInputScreen(navController: NavController) {
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    val historyRepository = remember { TransliterationHistoryRepository(context) }
    val userPrefsRepository = remember { UserPreferencesRepository(context) }
    val ocrSettingsRepository = remember { OCRSettingsRepository(context) }

    // User preferences
    val textDisplaySize by userPrefsRepository.textDisplaySize.collectAsState(initial = 1.0f)
    val hapticEnabled by userPrefsRepository.hapticFeedbackEnabled.collectAsState(initial = true)
    val autoCopyEnabled by userPrefsRepository.autoCopyEnabled.collectAsState(initial = false)

    // Input state backed by session to preserve text across navigation and language changes
    val inputText = TextInputSession.inputText
    val transliteratedText = TextInputSession.transliteratedText

    val selectedSourceCode by ocrSettingsRepository.selectedSourceTransliterationCode.collectAsState(
        initial = "auto"
    )
    val selectedTargetCode by ocrSettingsRepository.selectedTargetTransliterationCode.collectAsState(
        initial = "hi"
    )
    val sourceLanguage = remember(selectedSourceCode) {
        if (selectedSourceCode == "auto") {
            Language("auto", "Auto-detect", "Auto-detect", "", localeCode = "auto")
        } else {
            TransliterationLanguageCatalog.find(selectedSourceCode)
        }
    }
    val targetLanguage = remember(selectedTargetCode) {
        TransliterationLanguageCatalog.find(selectedTargetCode)
            ?: TransliterationLanguageCatalog.defaultTarget()
    }
    var showTextInputInfoDialog by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    // Read-aloud: Rasa ONNX voice for Indic languages once downloaded, system voice otherwise.
    val speech = rememberSpeechController()
    var resolvedSource by remember { mutableStateOf("en") }

    // Haptic feedback helper
    fun performHapticFeedback() {
        if (!hapticEnabled) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    // Live transliteration: the result follows the text as it is typed. The conversion itself is
    // quick, so only a short pause is needed; the history entry, clipboard copy and hand-off to
    // the speech engine wait for typing to settle so a half-typed word is not saved.
    LaunchedEffect(inputText, sourceLanguage, targetLanguage) {
        if (inputText.isBlank()) {
            TextInputSession.transliteratedText = ""
            speech.stop()
            return@LaunchedEffect
        }
        delay(120)
        val srcLang = if (sourceLanguage?.localeCode == "auto") {
            LanguageDetector.detectLanguage(inputText)
        } else {
            sourceLanguage?.localeCode ?: "en"
        }
        val tgtLang = targetLanguage.localeCode
        val converted = withContext(Dispatchers.Default) { Transliterator.transliterate(inputText, srcLang, tgtLang) }
        resolvedSource = speechLanguage(srcLang)
        TextInputSession.transliteratedText = converted

        delay(1200)
        if (autoCopyEnabled && converted.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText(context.getString(R.string.clipboard_label_transliterated_text), converted)
            )
        }
        if (converted.isNotBlank() && converted != inputText) {
            val historyKey = "$inputText->$converted->$srcLang->$tgtLang"
            if (TextInputSession.lastSavedHistoryKey != historyKey) {
                TextInputSession.lastSavedHistoryKey = historyKey
                historyRepository.addEntry(
                    HistoryEntry(
                        originalText = inputText,
                        transliteratedText = converted,
                        sourceLanguage = srcLang,
                        targetLanguage = tgtLang,
                        source = "text_input"
                    )
                )
            }
        }
    }

    // Copy to clipboard
    fun copyToClipboard(text: String) {
        performHapticFeedback()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                context.getString(R.string.clipboard_label_transliterated_text),
                text
            )
        )
        Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }

    // Auto-focus input on launch
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset + 10.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(
                description = stringResource(R.string.a11y_info),
                icon = painterResource(R.drawable.ic_info),
                size = 45.dp,
                background = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                onClick = { showTextInputInfoDialog = true }
            )
            Text(
                text = stringResource(R.string.text_input),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            GlassButton(
                description = stringResource(R.string.a11y_settings),
                icon = painterResource(R.drawable.ic_settings_outline),
                onClick = { navController.navigate("settings") }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LanguageSelectionRow(
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                onSourceLanguageClick = {
                    navController.navigate(
                        Screen.TransliterationLanguage.create(
                            Screen.TransliterationLanguage.ROLE_SOURCE
                        )
                    )
                },
                onTargetLanguageClick = {
                    navController.navigate(
                        Screen.TransliterationLanguage.create(
                            Screen.TransliterationLanguage.ROLE_TARGET
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Input Text Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.enter_text),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (inputText.isNotBlank()) {
                        SpeakButton(
                            controller = speech,
                            key = "typed",
                            description = stringResource(R.string.tts_speak_original_text),
                            onClick = {
                                performHapticFeedback()
                                speech.toggle("typed", listOf(SpeechPart(inputText, resolvedSource)))
                            },
                        )
                        TextButton(
                            onClick = {
                                TextInputSession.clear()
                                performHapticFeedback()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.clear),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = inputText,
                    onValueChange = { TextInputSession.inputText = it },
                    visualTransformation = spokenWordHighlight(speech, "typed", 0, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 200.dp)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (16 * textDisplaySize).sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.type_to_transliterate),
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        fontSize = (16 * textDisplaySize).sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Character count
                Text(
                    text = "${inputText.length} ${stringResource(R.string.characters)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.End
                )
            }

            // Result Area
            AnimatedVisibility(
                visible = transliteratedText.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.transliterated),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            // Copy button
                            IconButton(
                                onClick = { copyToClipboard(transliteratedText) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_copy),
                                    contentDescription = stringResource(R.string.copy),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            SpeakButton(
                                controller = speech,
                                key = "converted",
                                description = stringResource(R.string.tts_speak_converted_text),
                                onClick = {
                                    performHapticFeedback()
                                    speech.toggleConverted(
                                        "converted",
                                        SpeechPart(inputText, resolvedSource),
                                        SpeechPart(transliteratedText, targetLanguage.localeCode),
                                    )
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // With "speak the original first" on, the original is part 0 and this text is part 1.
                    HighlightedText(
                        text = transliteratedText,
                        controller = speech,
                        key = "converted",
                        partIndex = if (speech.settings.speakOriginal) 1 else 0,
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = (18 * textDisplaySize).sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showTextInputInfoDialog) {
        InfoBottomSheet(
            title = stringResource(R.string.text_input_info_title),
            message = stringResource(R.string.text_input_info_message),
            onDismiss = { showTextInputInfoDialog = false }
        )
    }
}
