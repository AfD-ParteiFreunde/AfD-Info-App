package de.afd.parteiapp.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.afd.parteiapp.R
import de.afd.parteiapp.ui.theme.AfDDisplayFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val dropY = remember { Animatable(-980f) }
    val scale = remember { Animatable(1f) }
    val glow = remember { Animatable(0f) }
    val flagScale = remember { Animatable(0.82f) }
    val reveal = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val dissolve = remember { Animatable(0f) }
    var skip by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        launch { glow.animateTo(1f, tween(520)) }
        launch { reveal.animateTo(1f, tween(520)) }
        launch { flagScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
        dropY.animateTo(
            0f,
            spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
        )
        scale.animateTo(1.08f, tween(140, easing = FastOutSlowInEasing))
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        val elapsed = System.currentTimeMillis() - startTime
        val end = System.currentTimeMillis() + (2150L - elapsed).coerceAtLeast(150L)
        while (!skip && System.currentTimeMillis() < end) delay(40)
        exit.animateTo(1f, tween(if (skip) 560 else 780, easing = FastOutSlowInEasing))
        dissolve.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        onFinished()
    }

    val density = LocalDensity.current
    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val targetTopPx = minOf(statusBarTopPx, with(density) { 30.dp.roundToPx() }) +
        with(density) { 19.dp.roundToPx() }
    val targetLeftPx = with(density) { 22.dp.roundToPx() }
    val targetHeightPx = with(density) { 24.dp.roundToPx() }

    val interaction = remember { MutableInteractionSource() }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = interaction, indication = null) { skip = true },
        contentAlignment = Alignment.Center,
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val bgAlpha = (1f - dissolve.value).coerceIn(0f, 1f)
        val bottomAlpha = (1f - exit.value / 0.4f).coerceIn(0f, 1f)

        Box(
            Modifier
                .fillMaxSize()
                .alpha(bgAlpha)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF04121F), Color(0xFF0A2A43), Color(0xFF0B4C74)),
                        )
                    )
            )
            Canvas(Modifier.fillMaxSize()) {
                drawGlow(
                    Offset(size.width * 0.5f, size.height * 0.42f),
                    size.minDimension * 0.62f,
                    Color(0xFF009EE0),
                    0.08f + 0.14f * glow.value,
                )
                drawGlow(
                    Offset(size.width * 0.5f, size.height * 0.40f),
                    size.minDimension * 0.34f,
                    Color.White,
                    0.04f + 0.10f * glow.value,
                )
                drawGlow(
                    Offset(size.width * 0.16f, size.height * 0.86f),
                    size.minDimension * 0.55f,
                    Color(0xFFE2001A),
                    0.05f + 0.08f * glow.value,
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.afd_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .width(262.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    val t = exit.value
                    val centeredLeft = (containerWidth - size.width) / 2f
                    val centeredTop = (containerHeight - size.height) / 2f
                    translationX = t * (targetLeftPx - centeredLeft)
                    translationY = t * (targetTopPx - centeredTop)
                    val endScale = targetHeightPx / size.height
                    val s = 1f + (endScale - 1f) * t
                    scaleX = s
                    scaleY = s
                    alpha = (1f - dissolve.value).coerceIn(0f, 1f)
                }
                .graphicsLayer {
                    translationY = dropY.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .alpha(bottomAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_german_flag),
                contentDescription = null,
                modifier = Modifier
                    .width(74.dp)
                    .graphicsLayer {
                        alpha = reveal.value
                        scaleX = flagScale.value
                        scaleY = flagScale.value
                    },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.splash_slogan),
                color = Color.White,
                style = TextStyle(
                    fontFamily = AfDDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 27.sp,
                    letterSpacing = 0.5.sp,
                ),
                modifier = Modifier.alpha(reveal.value),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.splash_skip),
                color = Color(0xFF6E8CA6),
                fontSize = 12.sp,
                modifier = Modifier.alpha(reveal.value * 0.9f),
            )
        }
    }
}

private fun DrawScope.drawGlow(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
