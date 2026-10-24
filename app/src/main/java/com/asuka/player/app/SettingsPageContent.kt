package com.asuka.player.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.R
import com.asuka.player.runtime.AppLanguagePreference

@Composable
internal fun SettingsPageContent(
    modifier: Modifier = Modifier,
    appVersion: String,
    hapticFeedbackEnabled: Boolean,
    onHapticFeedbackEnabledChange: (Boolean) -> Unit,
    appLanguage: AppLanguagePreference,
    onAppLanguageChange: (AppLanguagePreference) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenTheme: () -> Unit,
) {
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    if (showLanguageDialog) {
        AppLanguageDialog(
            current = appLanguage,
            onSelect = onAppLanguageChange,
            onDismiss = { showLanguageDialog = false },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_playback)) {
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.PlayCircle,
                        title = stringResource(R.string.settings_player_title),
                        description = stringResource(R.string.settings_player_desc),
                        onClick = onOpenPlayer,
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_appearance)) {
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.settings_theme_title),
                        description = stringResource(R.string.settings_theme_desc),
                        onClick = onOpenTheme,
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(R.string.settings_group_interaction)) {
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.TouchApp,
                        title = stringResource(R.string.settings_haptic_title),
                        description = stringResource(R.string.settings_haptic_desc),
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = onHapticFeedbackEnabledChange,
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(id = R.string.settings_general_title)) {
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.Translate,
                        title = stringResource(R.string.settings_language_title),
                        description = appLanguage.toOptionLabel(),
                        onClick = { showLanguageDialog = true },
                    )
                }
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.Info,
                        title = stringResource(id = R.string.settings_about_title),
                        description = stringResource(id = R.string.settings_about_desc, appVersion),
                        onClick = {},
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.size(6.dp)) }
    }
}
