package de.afd.parteiapp.ui.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import de.afd.parteiapp.R

private val AfDBlue = Color(0xFF009EE0)
private val CapsuleShape = RoundedCornerShape(percent = 50)

val LocalGlassNavPadding = compositionLocalOf { 0.dp }

val LocalGlassEnabled = compositionLocalOf { true }

class GlassScrollTopState {
    var visible by mutableStateOf(false)
    var action: (() -> Unit)? = null
}

val LocalGlassScrollTop = compositionLocalOf<GlassScrollTopState?> { null }

@Composable
fun GlassNavBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
) {
    val titles = listOf(
        stringResource(R.string.nav_news),
        stringResource(R.string.nav_wahlkampf),
        stringResource(R.string.nav_donate),
        stringResource(R.string.nav_contacts),
        stringResource(R.string.nav_more),
    )
    val icons = listOf<ImageVector>(
        Icons.Outlined.Newspaper,
        Icons.Outlined.HowToVote,
        Icons.Outlined.VolunteerActivism,
        Icons.Outlined.Contacts,
        Icons.Outlined.MoreHoriz,
    )
    val isLight = !isSystemInDarkTheme()
    val containerTint = if (isLight) Color(0x66FAFAFA) else Color(0x66121212)
    val unselectedTint = if (isLight) Color(0xB3000000) else Color(0xB8FFFFFF)

    BoxWithConstraints(modifier) {
        val pad = 4.dp
        val inset = 2.dp
        val tabWidth = (maxWidth - pad * 2) / titles.size
        val dropletX by animateDpAsState(
            targetValue = pad + inset + tabWidth * selected,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "dropletX",
        )
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CapsuleShape },
                    effects = {
                        vibrancy()
                        blur(12.dp.toPx())
                        lens(20.dp.toPx(), 20.dp.toPx())
                    },
                    shadow = {
                        Shadow(
                            radius = 30.dp,
                            offset = DpOffset(0.dp, 12.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                        )
                    },
                    onDrawSurface = { drawRect(containerTint) },
                ),
        ) {
            Box(
                Modifier
                    .offset(x = dropletX)
                    .width(tabWidth - inset * 2)
                    .fillMaxHeight()
                    .padding(vertical = pad + inset)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CapsuleShape },
                        effects = {
                            blur(10.dp.toPx())
                            lens(6.dp.toPx(), 10.dp.toPx(), chromaticAberration = true)
                        },
                        shadow = {
                            Shadow(
                                radius = 12.dp,
                                offset = DpOffset(0.dp, 5.dp),
                                color = Color.Black.copy(alpha = 0.30f),
                            )
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 8.dp,
                                offset = DpOffset(0.dp, 3.dp),
                                color = Color.Black.copy(alpha = 0.20f),
                            )
                        },
                        onDrawSurface = { drawRect(AfDBlue.copy(alpha = 0.92f)) },
                    ),
            )
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = pad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                titles.forEachIndexed { index, title ->
                    val isSelected = index == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            icons[index],
                            contentDescription = null,
                            tint = if (isSelected) Color.White else unselectedTint,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            title,
                            color = if (isSelected) Color.White else unselectedTint,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassFab(
    backdrop: LayerBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = !isSystemInDarkTheme()
    val tint = if (isLight) Color(0x59FFFFFF) else Color(0x4D16283A)
    val iconTint = if (isLight) Color(0xFF10141A) else Color(0xFFE9E9E9)

    Box(
        modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(5.dp.toPx(), 9.dp.toPx())
                },
                shadow = {
                    Shadow(
                        radius = 20.dp,
                        offset = DpOffset(0.dp, 9.dp),
                        color = Color.Black.copy(alpha = 0.38f),
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 7.dp,
                        offset = DpOffset(0.dp, 3.dp),
                        color = Color.Black.copy(alpha = 0.22f),
                    )
                },
                onDrawSurface = { drawRect(tint) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.KeyboardArrowUp,
            contentDescription = stringResource(R.string.news_to_top),
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
    }
}
