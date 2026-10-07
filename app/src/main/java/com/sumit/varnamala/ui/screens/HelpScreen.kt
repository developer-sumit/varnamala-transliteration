package com.sumit.varnamala.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sumit.varnamala.R
import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.data.preferences.OnboardingRepository
import com.sumit.varnamala.ui.components.GlassButton
import kotlinx.coroutines.launch

/** A short written tour, shown in the app language, plus a way to replay the camera coach marks. */
@Composable
fun HelpScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val onboarding = remember { OnboardingRepository(context) }
    val steps = listOf(
        R.string.help_scan_title to R.string.help_scan_body,
        R.string.help_languages_title to R.string.help_languages_body,
        R.string.help_tap_title to R.string.help_tap_body,
        R.string.help_voice_title to R.string.help_voice_body,
        R.string.help_type_title to R.string.help_type_body,
        R.string.help_keyboard_title to R.string.help_keyboard_body,
        R.string.help_private_title to R.string.help_private_body,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp, topInset + 5.dp, 16.dp, 0.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            GlassButton(description = stringResource(R.string.back), icon = painterResource(R.drawable.ic_chevron_left), onClick = { navController.popBackStack() })
            Text(stringResource(R.string.help_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(45.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            steps.forEachIndexed { index, (title, body) ->
                val titleText = stringResource(title)
                val bodyText = stringResource(body)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "${index + 1}. $titleText. $bodyText" },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(titleText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(bodyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
            ) { Text(stringResource(R.string.help_keyboard_enable)) }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        onboarding.setHasSeenCameraTutorial(false)
                        navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                    }
                },
            ) { Text(stringResource(R.string.help_replay)) }
        }
    }
}
