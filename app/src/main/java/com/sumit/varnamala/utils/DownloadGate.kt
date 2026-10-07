package com.sumit.varnamala.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sumit.varnamala.R
import com.sumit.varnamala.ui.components.ConfirmBottomSheet
import com.sumit.varnamala.ui.components.InfoBottomSheet

/** Checks run before any large model download: free space first, then the kind of connection. */
object DownloadChecks {
    sealed interface Verdict {
        data object Ok : Verdict
        data object Offline : Verdict
        data object Metered : Verdict
        data class LowStorage(val neededBytes: Long, val freeBytes: Long) : Verdict
    }

    /** Extra room beyond the file itself: the partial file and the verified copy briefly coexist. */
    private const val STORAGE_HEADROOM_FACTOR = 1.15
    private const val STORAGE_HEADROOM_BYTES = 50L * 1024 * 1024

    fun check(context: Context, bytes: Long, allowMetered: Boolean = false): Verdict {
        val needed = (bytes * STORAGE_HEADROOM_FACTOR).toLong() + STORAGE_HEADROOM_BYTES
        val free = context.filesDir.usableSpace
        if (free < needed) return Verdict.LowStorage(needed, free)

        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return Verdict.Offline
        if (!allowMetered && manager.isActiveNetworkMetered) return Verdict.Metered
        return Verdict.Ok
    }
}

/**
 * Wraps a download button: [request] runs the download straight away on Wi-Fi, asks first on
 * mobile data, and explains instead when storage is too low or there is no connection.
 * Put [Dialogs] once in the screen.
 */
@Stable
class DownloadGate internal constructor(private val context: Context) {
    private var pending by mutableStateOf<Pending?>(null)
    private var blocked by mutableStateOf<DownloadChecks.Verdict?>(null)

    private class Pending(val bytes: Long, val onProceed: () -> Unit)

    fun request(bytes: Long, onProceed: () -> Unit) {
        when (val verdict = DownloadChecks.check(context, bytes)) {
            DownloadChecks.Verdict.Ok -> onProceed()
            DownloadChecks.Verdict.Metered -> pending = Pending(bytes, onProceed)
            else -> blocked = verdict
        }
    }

    @Composable
    fun Dialogs() {
        pending?.let { ask ->
            ConfirmBottomSheet(
                title = stringResource(R.string.mobile_data_title),
                message = stringResource(R.string.mobile_data_message, Formatter.formatShortFileSize(context, ask.bytes)),
                confirmText = stringResource(R.string.download),
                onDismiss = { pending = null },
                onConfirm = {
                    pending = null
                    ask.onProceed()
                },
            )
        }
        when (val verdict = blocked) {
            is DownloadChecks.Verdict.LowStorage -> InfoBottomSheet(
                title = stringResource(R.string.storage_low_title),
                message = stringResource(
                    R.string.storage_low_message,
                    Formatter.formatShortFileSize(context, verdict.neededBytes),
                    Formatter.formatShortFileSize(context, verdict.freeBytes),
                ),
                onDismiss = { blocked = null },
            )
            DownloadChecks.Verdict.Offline -> InfoBottomSheet(
                title = stringResource(R.string.offline_title),
                message = stringResource(R.string.offline_message),
                onDismiss = { blocked = null },
            )
            else -> Unit
        }
    }
}

@Composable
fun rememberDownloadGate(): DownloadGate {
    val context = LocalContext.current
    return remember(context) { DownloadGate(context) }
}
