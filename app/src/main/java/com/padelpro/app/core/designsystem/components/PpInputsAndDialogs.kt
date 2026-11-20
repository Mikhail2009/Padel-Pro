package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun PpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    maxLength: Int? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            if (maxLength == null || next.length <= maxLength) {
                onValueChange(next)
            }
        },
        placeholder = {
            Text(
                text = placeholder,
                style = PpTextStyles.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else null,
        singleLine = singleLine,
        textStyle = PpTextStyles.Body.copy(color = MaterialTheme.colorScheme.onBackground),
        shape = RoundedCornerShape(PpRadius.input),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun PpEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    ctaEnabled: Boolean = true
) {
    PpCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PpSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(PpSpacing.md))
            Text(
                text = title,
                style = PpTextStyles.Title,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(PpSpacing.xs))
            Text(
                text = subtitle,
                style = PpTextStyles.Label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (ctaText != null && onCtaClick != null) {
                Spacer(Modifier.height(PpSpacing.lg))
                PpPrimaryButton(
                    text = ctaText,
                    onClick = onCtaClick,
                    enabled = ctaEnabled,
                    fullWidth = false
                )
            }
        }
    }
}

@Composable
fun PpConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(PpRadius.card),
        title = {
            Text(
                text = title,
                style = PpTextStyles.H2,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Text(
                text = body,
                style = PpTextStyles.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText.uppercase(),
                    style = PpTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = PpTextStyles.Label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PpBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onBackground,
        shape = RoundedCornerShape(topStart = PpRadius.sheetTop, topEnd = PpRadius.sheetTop)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PpSpacing.lg)
                .padding(bottom = PpSpacing.xxxl)
        ) {
            Text(
                text = title,
                style = PpTextStyles.H2,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(PpSpacing.lg))
            content()
        }
    }
}
