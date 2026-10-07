package com.sumit.varnamala.utils

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.sumit.varnamala.R

/** Whether history is unlocked in this app session. Reset when the app leaves the foreground. */
object AppLockSession {
    @Volatile var unlocked = false
}

private fun authenticators() =
    if (Build.VERSION.SDK_INT >= 30) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    }

/** True when the phone has a fingerprint, face or (Android 11+) screen lock that can be used. */
fun canAuthenticate(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(authenticators()) == BiometricManager.BIOMETRIC_SUCCESS

/** Shows the system prompt; [onSuccess] runs only when the owner is verified. */
fun authenticateOwner(context: Context, onSuccess: () -> Unit, onFailure: () -> Unit = {}) {
    val activity = context.findFragmentActivity() ?: return onFailure()
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                AppLockSession.unlocked = true
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailure()
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.app_lock_prompt_title))
        .setAllowedAuthenticators(authenticators())
        .apply { if (Build.VERSION.SDK_INT < 30) setNegativeButtonText(context.getString(R.string.cancel)) }
        .build()
    prompt.authenticate(info)
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
