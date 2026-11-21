package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.padelpro.app.R
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpCourtLineStrong
import com.padelpro.app.core.designsystem.theme.PpCourtNight
import com.padelpro.app.core.designsystem.theme.PpCourtPanel
import com.padelpro.app.core.designsystem.theme.PpDanger
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpScrimColor
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextMid
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
                color = PpTextLow
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = PpGlassCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else null,
        singleLine = singleLine,
        textStyle = PpTextStyles.Body.copy(color = PpTextHigh),
        shape = PpRadius.inputShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PpCourtPanel,
            unfocusedContainerColor = PpCourtNight,
            focusedBorderColor = PpGlassCyan,
            unfocusedBorderColor = PpCourtLineStrong,
            cursorColor = PpLime,
            focusedTextColor = PpTextHigh,
            unfocusedTextColor = PpTextHigh
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth()
    )
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
        containerColor = PpCourtGlass,
        shape = PpRadius.cardShape,
        modifier = Modifier.border(1.dp, PpCourtLineStrong, PpRadius.cardShape),
        title = {
            Text(
                text = title,
                style = PpTextStyles.H2,
                color = PpTextHigh
            )
        },
        text = {
            Text(
                text = body,
                style = PpTextStyles.Body,
                color = PpTextMid
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText.uppercase(),
                    style = PpTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                    color = if (isDestructive) PpDanger else PpLime
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = PpTextStyles.Label,
                    color = PpTextMid
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
        containerColor = PpCourtGlass,
        contentColor = PpTextHigh,
        scrimColor = PpScrimColor,
        shape = RoundedCornerShape(topStart = PpRadius.sheet, topEnd = PpRadius.sheet),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = PpCourtLineStrong)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PpSpacing.lg)
                .padding(bottom = PpSpacing.xxl)
        ) {
            Text(
                text = title,
                style = PpTextStyles.H2,
                color = PpTextHigh
            )
            Spacer(Modifier.height(PpSpacing.lg))
            content()
        }
    }
}
