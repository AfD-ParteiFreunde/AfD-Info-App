package de.afd.parteiapp.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

internal fun Modifier.glassSurface(
    shape: Shape,
    tint: Color,
    shadowAlpha: Float = 0.30f,
    shadowRadius: Dp = 14.dp,
    innerShadowAlpha: Float = 0.18f,
    showHighlight: Boolean = true,
    showInnerShadow: Boolean = true,
): Modifier = this
    .clip(shape)
    .drawBackdrop(
        backdrop = emptyBackdrop(),
        shape = { shape },
        effects = {},
        highlight = { if (showHighlight) Highlight.Default else Highlight.Default.copy(alpha = 0f) },
        shadow = {
            Shadow(
                radius = shadowRadius,
                offset = DpOffset(0.dp, 6.dp),
                color = Color.Black.copy(alpha = shadowAlpha),
            )
        },
        innerShadow = {
            if (showInnerShadow) {
                InnerShadow(
                    radius = 6.dp,
                    offset = DpOffset(0.dp, 2.dp),
                    color = Color.Black.copy(alpha = innerShadowAlpha),
                )
            } else {
                InnerShadow.Default.copy(alpha = 0f)
            }
        },
        onDrawSurface = { drawRect(tint) },
    )

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(percent = 50),
    tint: Color = Color(0x40FFFFFF),
    contentColor: Color = Color.White,
    content: @Composable () -> Unit,
) {
    if (!LocalGlassEnabled.current) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = tint,
                contentColor = contentColor,
            ),
            content = {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    content()
                }
            },
        )
        return
    }
    Box(
        modifier
            .glassSurface(shape = shape, tint = tint)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    if (!LocalGlassEnabled.current) {
        FilterChip(selected = selected, onClick = onClick, label = label, modifier = modifier)
        return
    }
    val tint = if (selected) Color(0x59FFFFFF) else Color(0x1FFFFFFF)
    Box(
        modifier
            .glassSurface(
                shape = RoundedCornerShape(percent = 50),
                tint = tint,
                shadowAlpha = if (selected) 0.30f else 0.18f,
                shadowRadius = if (selected) 14.dp else 8.dp,
                innerShadowAlpha = if (selected) 0.18f else 0.10f,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (selected) Color.White else Color(0xFFB9D4E8),
            LocalTextStyle provides MaterialTheme.typography.labelMedium,
        ) {
            label()
        }
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    tint: Color = Color(0x1FFFFFFF),
    content: @Composable () -> Unit,
) {
    if (!LocalGlassEnabled.current) {
        IconButton(onClick = onClick, modifier = modifier) { content() }
        return
    }
    Box(
        modifier
            .size(size)
            .glassSurface(
                shape = CircleShape,
                tint = tint,
                shadowAlpha = 0.20f,
                shadowRadius = 8.dp,
                innerShadowAlpha = 0.12f,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
