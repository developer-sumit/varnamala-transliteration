package com.sumit.varnamala.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sumit.varnamala.R
import com.sumit.varnamala.data.preferences.UserPreferencesRepository
import com.sumit.varnamala.utils.AppLockSession
import com.sumit.varnamala.utils.authenticateOwner

/** Shows [content] only after the owner has unlocked, when the app lock is on. */
@Composable
fun LockGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // null until the saved setting has been read: the lock must not appear (or prompt) before then.
    val locked by remember { UserPreferencesRepository(context).appLockEnabled }.collectAsState(initial = null)
    var unlocked by remember { mutableStateOf(AppLockSession.unlocked) }

    if (locked == null) return
    if (locked == false || unlocked) {
        content()
        return
    }

    fun prompt() = authenticateOwner(context, onSuccess = { unlocked = true })
    LaunchedEffect(Unit) { prompt() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.ic_lock), contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.app_lock_locked), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        Button(onClick = ::prompt) { Text(stringResource(R.string.app_lock_unlock)) }
    }
}
