package com.worktime.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.ui.components.AppModalBottomSheet
import com.worktime.app.ui.components.AppDimens

@Composable
internal fun PrivacyDataSheet(onDismiss: () -> Unit) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.privacy_and_data),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
        ) {
            PrivacySection(
                title = stringResource(R.string.privacy_stored_data_title),
                body = stringResource(R.string.privacy_stored_data_text),
            )
            PrivacySection(
                title = stringResource(R.string.privacy_network_title),
                body = stringResource(R.string.privacy_network_text),
            )
            PrivacySection(
                title = stringResource(R.string.privacy_backup_title),
                body = stringResource(R.string.privacy_backup_text),
            )
            PrivacySection(
                title = stringResource(R.string.privacy_deletion_title),
                body = stringResource(R.string.privacy_deletion_text),
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
