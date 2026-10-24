package com.asuka.player.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.R
import com.asuka.player.runtime.AppLanguagePreference

@Composable
internal fun AppLanguageDialog(
    current: AppLanguagePreference,
    onSelect: (AppLanguagePreference) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_language_title)) },
        text = {
            Column {
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.selectableGroup(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(AppLanguagePreference.entries) { preference ->
                        DoubleTapActionOptionRow(
                            text = preference.toOptionLabel(),
                            selected = preference == current,
                            onClick = {
                                onSelect(preference)
                                onDismiss()
                            },
                        )
                    }
                }
                HorizontalDivider()
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {},
    )
}

@Composable
internal fun AppLanguagePreference.toOptionLabel(): String {
    if (this != AppLanguagePreference.System) return nativeDisplayName
    return stringResource(R.string.language_system_default)
}
