package com.worktime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Visual bottom-sheet handle without the default Material tooltip semantics.
 * The containing sheet retains its normal drag behavior.
 */
@Composable
fun PlainDragHandle(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(72.dp)
            .height(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
                ),
        )
    }
}
