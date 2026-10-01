package de.afd.parteiapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.afd.parteiapp.R
import de.afd.parteiapp.ui.glass.LocalGlassEnabled
import de.afd.parteiapp.ui.glass.glassSurface

@Composable
fun BrandTopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val iconRowHeight = with(density) { 30.dp.roundToPx() }
    val topPadding = with(density) { minOf(statusBarTop, iconRowHeight).toDp() + 8.dp }
    val glass = LocalGlassEnabled.current
    val row: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .height(46.dp)
                .padding(start = 22.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(
                    if (isSystemInDarkTheme()) R.drawable.afd_logo else R.drawable.afd_logo_light,
                ),
                contentDescription = null,
                modifier = Modifier.height(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
    }
    if (glass) {
        val surfaceColor = MaterialTheme.colorScheme.surface
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .glassSurface(
                        shape = RoundedCornerShape(0.dp),
                        tint = surfaceColor.copy(alpha = 0.96f),
                        shadowAlpha = 0f,
                        shadowRadius = 0.dp,
                        showHighlight = false,
                        showInnerShadow = false,
                    ),
            ) {
                row()
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to surfaceColor.copy(alpha = 0.96f),
                                0.45f to surfaceColor.copy(alpha = 0.35f),
                                1f to Color.Transparent,
                            ),
                        )
                    },
            )
        }
    } else {
        Surface(color = MaterialTheme.colorScheme.surface) { row() }
    }
}
