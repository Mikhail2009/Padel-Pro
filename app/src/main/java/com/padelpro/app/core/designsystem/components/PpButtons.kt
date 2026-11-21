package com.padelpro.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.padelpro.app.core.designsystem.theme.LocalPpMotion
import com.padelpro.app.core.designsystem.theme.PpCourtGlass
import com.padelpro.app.core.designsystem.theme.PpDanger
import com.padelpro.app.core.designsystem.theme.PpGlassCyan
import com.padelpro.app.core.designsystem.theme.PpLime
import com.padelpro.app.core.designsystem.theme.PpLimeDeep
import com.padelpro.app.core.designsystem.theme.PpLimeGlow
import com.padelpro.app.core.designsystem.theme.PpOnAccent
import com.padelpro.app.core.designsystem.theme.PpRadius
import com.padelpro.app.core.designsystem.theme.PpTextHigh
import com.padelpro.app.core.designsystem.theme.PpTextLow
import com.padelpro.app.core.designsystem.theme.PpTextStyles
import com.padelpro.app.core.designsystem.theme.PpTouch

fun Modifier.ppClickable(
    interaction: MutableInteractionSource?,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this.clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = null, indication = null, onClick = onClick)

@Composable
private fun rememberPressScale(interaction: MutableInteractionSource): Float {
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalPpMotion.current
    return animateFloatAsState(
        targetValue = if (pressed && motion.enabled) 0.965f else 1f,
        animationSpec = motion.pop(), label = "pp-press"
    ).value
}

@Composable
fun PpPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .defaultMinSize(minHeight = PpTouch.min)
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(PpRadius.pill)
            .drawBehind {
                val cr = CornerRadius(size.height / 2)
                if (enabled) {
                    drawRoundRect(Brush.horizontalGradient(listOf(PpLimeGlow, PpLime, PpLimeDeep)), cornerRadius = cr)
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color.Transparent)),
                        size = Size(size.width, size.height * 0.5f), cornerRadius = cr
                    )
                } else {
                    drawRoundRect(Color(0x1F9DB2BC), cornerRadius = cr)
                }
            }
            .ppClickable(interaction, enabled) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick()
            }
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = if (enabled) PpOnAccent else PpTextLow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text.uppercase(), style = PpTextStyles.Label,
                color = if (enabled) PpOnAccent else PpTextLow,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
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
    leadingIcon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Box(
        modifier
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .glassPanel(radius = 28.dp)
            .ppClickable(interaction, enabled, onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = if (enabled) PpTextHigh else PpTextLow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text.uppercase(), style = PpTextStyles.Label,
                color = if (enabled) PpTextHigh else PpTextLow,
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PpDangerOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Box(
        modifier
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(PpRadius.pill)
            .drawBehind {
                val cr = CornerRadius(size.height / 2)
                drawRoundRect(Color(0x1AFF4D5E), cornerRadius = cr)
                drawRoundRect(
                    PpDanger.copy(alpha = if (enabled) 0.75f else 0.25f),
                    cornerRadius = cr, style = Stroke(1.4f)
                )
            }
            .ppClickable(interaction, enabled, onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.uppercase(), style = PpTextStyles.Label,
            color = if (enabled) PpDanger else PpTextLow, maxLines = 2, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PpIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = PpTextHigh
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Box(
        modifier
            .size(PpTouch.min)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                val cr = CornerRadius(16.dp.toPx())
                drawRoundRect(PpCourtGlass.copy(alpha = 0.55f), cornerRadius = cr)
                drawRoundRect(PpGlassCyan.copy(alpha = 0.18f), cornerRadius = cr, style = Stroke(1f))
            }
            .ppClickable(interaction, enabled, onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = if (enabled) tint else PpTextLow, modifier = Modifier.size(22.dp))
    }
}