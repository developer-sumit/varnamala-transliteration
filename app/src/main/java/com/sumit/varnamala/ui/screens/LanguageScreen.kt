package com.sumit.varnamala.ui.screens

import com.sumit.varnamala.R
import com.sumit.varnamala.data.Language
import com.sumit.varnamala.ui.components.GlassButton
import com.sumit.varnamala.data.AppLanguageRepository
import com.sumit.varnamala.data.preferences.AppLanguageRepository as AppLanguagePrefsRepository

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun LanguageScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val languageRepository = remember { AppLanguagePrefsRepository(context) }
    val currentLanguage by languageRepository.appLanguage.collectAsState(initial = "en")
    val availableLanguages = remember { AppLanguageRepository.getSupportedLanguages() }
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
        // Background pattern
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp, topInset + 5.dp, 16.dp, 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassButton(
                    icon = painterResource(R.drawable.ic_chevron_left),
                    onClick = { navController.popBackStack() }
                )

                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.width(45.dp))
            }

            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(20.dp)),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(availableLanguages) { language ->
                    LanguageItem(
                        language = language,
                        isSelected = currentLanguage == language.code,
                        onLanguageSelected = {
                            scope.launch {
                                languageRepository.setAppLanguage(language.code)
                                navController.popBackStack()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onLanguageSelected: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .clickable { onLanguageSelected() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Flag Emoji
            Text(
                text = language.flagEmoji,
                fontSize = 32.sp,
                modifier = Modifier.size(40.dp)
            )

            // Language Names
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = language.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                if (language.name != language.nativeName) {
                    Text(
                        text = language.nativeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Selection Indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = stringResource(R.string.selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

