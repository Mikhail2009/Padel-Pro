package com.padelpro.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpSpacing
import com.padelpro.app.core.designsystem.theme.PpTextStyles

@Composable
fun PpPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fullWidth: Boolean = true
) {
    val baseModifier = if (fullWidth) modifier.fillMaxWidth() else modifier
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PpRadius.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = PpSpacing.xl, vertical = 14.dp),
        modifier = baseModifier.heightIn(min = 52.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(PpSpacing.sm))
            }
            Text(
                text = text.uppercase(),
                style = PpTextStyles.Label.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PpSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fullWidth: Boolean = true
) {
    val baseModifier = if (fullWidth) modifier.fillMaxWidth() else modifier
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PpRadius.pill,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = PpSpacing.xl, vertical = 14.dp),
        modifier = baseModifier.heightIn(min = 50.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(PpSpacing.sm))
            }
            Text(
                text = text.uppercase(),
                style = PpTextStyles.Label.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PpDangerOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PpRadius.pill,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = PpSpacing.xl, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(PpSpacing.sm))
            }
            Text(
                text = text.uppercase(),
                style = PpTextStyles.Label.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PpIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp)
        )
    }
}
