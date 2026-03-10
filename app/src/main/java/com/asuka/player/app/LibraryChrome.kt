package com.asuka.player.app

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asuka.player.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryPageScaffold(
    title: String,
    showBack: Boolean,
    showSettingsAction: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    speedDialActions: List<SpeedDialAction> = emptyList(),
    content: @Composable (PaddingValues) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 1.dp,
                ) {
                    TopAppBar(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        title = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            if (showBack) {
                                Row {
                                    IconButton(
                                        modifier = Modifier.size(36.dp),
                                        onClick = onBack,
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(id = R.string.back_to_folders),
                                        )
                                    }
                                    Spacer(modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        actions = {
                            if (showSettingsAction) {
                                IconButton(
                                    modifier = Modifier.size(36.dp),
                                    onClick = {
                                        if (hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        }
                                        onOpenSettings()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = stringResource(id = R.string.settings_title),
                                    )
                                }
                            }
                        },
                        windowInsets = TopAppBarDefaults.windowInsets,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            },
            content = content,
        )

        if (speedDialActions.isNotEmpty()) {
            LibrarySpeedDial(
                actions = speedDialActions,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LibrarySpeedDial(
    actions: List<SpeedDialAction>,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    val transition = updateTransition(targetState = expanded, label = "LibrarySpeedDial")
    val scrimAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = if (targetState) 170 else 90) },
        label = "LibrarySpeedDialScrim",
    ) { isExpanded -> if (isExpanded) 0.08f else 0f }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val endPadding = 18.dp
    val bottomPadding = 18.dp + bottomInset

    if (scrimAlpha > 0f) {
        Box(
            modifier = modifier
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(Unit) { detectTapGestures { expanded = false } },
        )
    }

    Box(modifier = modifier) {
        FloatingActionButtonMenu(
            expanded = expanded,
            button = {
                val collapsedContainerColor = lerp(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    0.30f,
                )
                ToggleFloatingActionButton(
                    checked = expanded,
                    onCheckedChange = { nextExpanded ->
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        }
                        expanded = nextExpanded
                    },
                    containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                        collapsedContainerColor,
                        MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    val progress = checkedProgress.coerceIn(0f, 1f)
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = lerp(
                            MaterialTheme.colorScheme.onPrimaryContainer,
                            MaterialTheme.colorScheme.onPrimary,
                            progress,
                        ),
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = endPadding, bottom = bottomPadding),
            horizontalAlignment = Alignment.End,
        ) {
            actions.forEach { action ->
                FloatingActionButtonMenuItem(
                    onClick = {
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        }
                        expanded = false
                        action.onClick()
                    },
                    icon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                        )
                    },
                    text = {
                        Text(text = action.label)
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
