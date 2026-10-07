package com.sumit.varnamala.ime

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.sumit.varnamala.R
import com.sumit.varnamala.data.TransliterationLanguageCatalog
import com.sumit.varnamala.utils.Transliterator

/**
 * Offline transliteration keyboard. The user types English letters; the word being typed is
 * shown in the chosen Indian script inside the text field and is replaced live as letters are
 * added. Conversion uses the same engine as the rest of the app, so nothing leaves the phone.
 */
class VarnamalaKeyboardService : InputMethodService() {
    private val languages = TransliterationLanguageCatalog.supportedLanguages.filter { it.localeCode != "en" }
    private var languageIndex = 0

    /** Letters typed for the current word, not yet committed. */
    private val latin = StringBuilder()
    private var shifted = false

    /** Password and similar fields get plain letters: converting them would make them unreadable to the site. */
    private var plainMode = false

    private lateinit var suggestionConverted: TextView
    private lateinit var suggestionRaw: TextView
    private lateinit var languageKey: TextView
    private val letterKeys = mutableListOf<TextView>()
    private val handler = Handler(Looper.getMainLooper())

    private val dark get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private val keyboardBackground get() = if (dark) Color.parseColor("#1C1B1F") else Color.parseColor("#E8E8EC")
    private val keyColor get() = if (dark) Color.parseColor("#3A3A40") else Color.WHITE
    private val actionKeyColor get() = if (dark) Color.parseColor("#55555C") else Color.parseColor("#C9CAD2")
    private val textColor get() = if (dark) Color.WHITE else Color.BLACK

    override fun onCreate() {
        super.onCreate()
        val saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_LANGUAGE, "hi")
        languageIndex = languages.indexOfFirst { it.localeCode == saved }.coerceAtLeast(0)
    }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(keyboardBackground)
            setPadding(dp(4), dp(4), dp(4), dp(6))
        }
        root.addView(buildSuggestionBar())
        root.addView(row("1234567890", digits = true))
        root.addView(row("qwertyuiop"))
        root.addView(row("asdfghjkl"))
        root.addView(bottomLetterRow())
        root.addView(lastRow())
        refreshSuggestions()
        return root
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        latin.clear()
        val variation = (attribute?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION
        val isText = ((attribute?.inputType ?: 0) and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
        plainMode = !isText || variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_URI
        if (::suggestionConverted.isInitialized) refreshSuggestions()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        // What is on screen stays; it just stops being editable by the keyboard.
        currentInputConnection?.finishComposingText()
        latin.clear()
    }

    // ---- Typing ----

    private fun typeLetter(letter: Char) {
        val c = if (shifted) letter.uppercaseChar() else letter
        if (shifted) setShift(false)
        if (plainMode) {
            currentInputConnection?.commitText(c.toString(), 1)
            return
        }
        latin.append(c)
        updateComposing()
    }

    private fun updateComposing() {
        val ic = currentInputConnection ?: return
        ic.setComposingText(convert(latin.toString()), 1)
        refreshSuggestions()
    }

    private fun convert(word: String): String =
        if (word.isEmpty()) "" else Transliterator.transliterate(word, "en", languages[languageIndex].localeCode)

    /** Ends the current word, keeping the converted text that is already shown. */
    private fun finishWord() {
        if (latin.isNotEmpty()) {
            currentInputConnection?.finishComposingText()
            latin.clear()
            refreshSuggestions()
        }
    }

    private fun typeSeparator(text: String) {
        finishWord()
        currentInputConnection?.commitText(text, 1)
    }

    private fun backspace() {
        val ic = currentInputConnection ?: return
        if (latin.isNotEmpty()) {
            latin.deleteCharAt(latin.length - 1)
            if (latin.isEmpty()) {
                ic.setComposingText("", 1)
                ic.finishComposingText()
                refreshSuggestions()
            } else {
                updateComposing()
            }
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
    }

    private fun enter() {
        finishWord()
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = (currentInputEditorInfo?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED && !noEnterAction) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    /** Keep the word exactly as typed in English letters instead of the converted form. */
    private fun keepLatin() {
        if (latin.isEmpty()) return
        currentInputConnection?.setComposingText(latin.toString(), 1)
        finishWord()
    }

    private fun nextLanguage() {
        languageIndex = (languageIndex + 1) % languages.size
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_LANGUAGE, languages[languageIndex].localeCode).apply()
        if (latin.isNotEmpty()) updateComposing() else refreshSuggestions()
    }

    private fun setShift(on: Boolean) {
        shifted = on
        letterKeys.forEach { key ->
            val letter = key.tag as? Char ?: return@forEach
            key.text = (if (on) letter.uppercaseChar() else letter).toString()
        }
    }

    private fun refreshSuggestions() {
        if (!::suggestionConverted.isInitialized) return
        val language = languages[languageIndex]
        languageKey.text = language.nativeName.take(4)
        languageKey.contentDescription = getString(R.string.keyboard_language, language.name)
        val word = latin.toString()
        suggestionConverted.text = if (word.isEmpty()) language.nativeName else convert(word)
        suggestionConverted.alpha = if (word.isEmpty()) 0.5f else 1f
        suggestionRaw.text = word
        suggestionRaw.visibility = if (word.isEmpty()) View.GONE else View.VISIBLE
    }

    // ---- Layout ----

    private fun buildSuggestionBar(): View {
        languageKey = key("", weight = 0f, width = dp(64), color = actionKeyColor, description = null) { nextLanguage() }
        suggestionConverted = TextView(this).apply {
            textSize = 20f
            setTextColor(textColor)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            // Tapping the converted word finishes it, like pressing space without adding a space.
            setOnClickListener { finishWord() }
        }
        suggestionRaw = TextView(this).apply {
            textSize = 16f
            setTextColor(textColor)
            alpha = 0.7f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            contentDescription = getString(R.string.keyboard_keep_latin)
            setOnClickListener { keepLatin() }
        }
        val scroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(suggestionConverted)
                addView(suggestionRaw)
            })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            addView(languageKey)
            addView(scroller, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun row(letters: String, digits: Boolean = false): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
        letters.forEach { c ->
            val k = key(c.toString(), weight = 1f, description = c.toString()) {
                if (digits) typeSeparator(c.toString()) else typeLetter(c)
            }
            if (!digits) { k.tag = c; letterKeys += k }
            addView(k)
        }
    }

    private fun bottomLetterRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
        addView(key("⇧", weight = 1.4f, color = actionKeyColor, description = getString(R.string.keyboard_shift)) { setShift(!shifted) })
        "zxcvbnm".forEach { c ->
            val k = key(c.toString(), weight = 1f, description = c.toString()) { typeLetter(c) }
            k.tag = c; letterKeys += k
            addView(k)
        }
        val back = key("⌫", weight = 1.4f, color = actionKeyColor, description = getString(R.string.keyboard_backspace)) { backspace() }
        // Holding backspace repeats, like every other keyboard.
        val repeat = object : Runnable {
            override fun run() { backspace(); handler.postDelayed(this, 55) }
        }
        back.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { v.isPressed = true; backspace(); handler.postDelayed(repeat, 400) }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.isPressed = false; handler.removeCallbacks(repeat) }
            }
            true
        }
        addView(back)
    }

    private fun lastRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
        addView(key(",", weight = 1f, description = ",") { typeSeparator(",") })
        addView(key("", weight = 5f, description = getString(R.string.keyboard_space)) { typeSeparator(" ") })
        addView(key(".", weight = 1f, description = ".") { typeSeparator(".") })
        addView(key("?", weight = 1f, description = "?") { typeSeparator("?") })
        addView(key("⏎", weight = 1.5f, color = actionKeyColor, description = getString(R.string.keyboard_enter)) { enter() })
    }

    private fun key(
        label: String,
        weight: Float,
        width: Int = 0,
        color: Int = keyColor,
        description: String?,
        onClick: () -> Unit,
    ): TextView = TextView(this).apply {
        text = label
        textSize = 18f
        typeface = Typeface.DEFAULT
        gravity = Gravity.CENTER
        setTextColor(textColor)
        contentDescription = description
        background = GradientDrawable().apply { setColor(color); cornerRadius = dp(8).toFloat() }
        layoutParams = LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
        setOnClickListener { onClick() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val PREFS = "keyboard"
        const val KEY_LANGUAGE = "language"
    }
}
