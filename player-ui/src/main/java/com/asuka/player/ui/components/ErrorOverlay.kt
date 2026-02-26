package com.asuka.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.R

@Composable
fun ErrorOverlay(
    modifier: Modifier = Modifier,
    message: String?,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (message.isNullOrBlank()) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = Color.White)
            Text(text = stringResource(id = R.string.retry), color = Color.White, modifier = Modifier.testTag("err_retry").clickable { onRetry() })
            Text(text = stringResource(id = R.string.next), color = Color.White, modifier = Modifier.testTag("err_next").clickable { onNext() })
            Text(text = stringResource(id = R.string.dismiss), color = Color.White, modifier = Modifier.testTag("err_dismiss").clickable { onDismiss() })
        }
    }
}
