package com.flowreader.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.core.R

/**
 * The single loading / empty / error / content switch.
 *
 * Four screens each hand-rolled their own version of this with different wording, layout and
 * button sets before v52. Screens now pass state in and get one consistent treatment out.
 *
 * v53 added [loadingContent] so a screen whose shape is known ahead of time (the shelf) can render
 * a skeleton instead of a spinner without forking the rest of the switch.
 */
@Composable
fun FlowStateHost(
    isLoading: Boolean,
    isEmpty: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    emptyTitle: String = stringResource(R.string.flow_state_empty),
    emptyMessage: String? = null,
    emptyIcon: ImageVector? = null,
    emptyAction: (@Composable () -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onDismissError: (() -> Unit)? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    loadingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    when {
        error != null -> FlowErrorState(
            message = error,
            modifier = modifier,
            onRetry = onRetry,
            onDismiss = onDismissError,
            contentColor = contentColor
        )

        isLoading -> if (loadingContent != null) {
            Box(modifier = modifier.fillMaxSize()) { loadingContent() }
        } else {
            FlowLoadingState(modifier = modifier)
        }

        isEmpty -> FlowEmptyState(
            title = emptyTitle,
            modifier = modifier,
            message = emptyMessage,
            icon = emptyIcon,
            action = emptyAction
        )

        else -> content()
    }
}

@Composable
fun FlowLoadingState(modifier: Modifier = Modifier, label: String = stringResource(R.string.flow_state_loading)) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun FlowEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FlowSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.xl))
            action()
        }
    }
}

@Composable
fun FlowErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FlowSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = stringResource(R.string.flow_state_error),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(FlowSpacing.lg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            textAlign = TextAlign.Center
        )
        if (onRetry != null || onDismiss != null) {
            Spacer(modifier = Modifier.height(FlowSpacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(FlowSpacing.md)) {
                if (onRetry != null) {
                    Button(onClick = onRetry) { Text(stringResource(R.string.flow_action_retry)) }
                }
                if (onDismiss != null) {
                    OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.flow_action_back)) }
                }
            }
        }
    }
}
