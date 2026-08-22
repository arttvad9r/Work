package com.worktime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.dp

/**
 * Visual bottom-sheet handle without Material's long-press tooltip.
 *
 * When [onClick] is supplied, the handle remains tappable without ripple/tooltip feedback.
 * [accessibilityLabel] restores a single TalkBack action without using Material's tooltip slot.
 * Dragging is still owned by the containing sheet.
 */
@Composable
fun PlainDragHandle(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accessibilityLabel: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(clickModifier)
            .clearAndSetSemantics {
                if (onClick != null && accessibilityLabel != null) {
                    contentDescription = accessibilityLabel
                    onClick {
                        onClick()
                        true
                    }
                }
            }
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
