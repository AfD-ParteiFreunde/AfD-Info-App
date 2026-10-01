package de.afd.parteiapp.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.afd.parteiapp.R

fun sourceColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF009EE0))

fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

val PREFERRED_BROWSERS = listOf(
    "com.brave.browser",
    "com.android.chrome",
    "org.cromite.cromite",
    "org.mozilla.firefox",
)

fun openUrlInPreferredBrowser(context: Context, url: String) {
    val uri = Uri.parse(url)
    for (pkg in PREFERRED_BROWSERS) {
        val installed = runCatching {
            context.packageManager.getPackageInfo(pkg, 0)
        }.isSuccess
        if (installed) {
            val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(pkg)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }
    openUrl(context, url)
}

fun sendEmail(context: Context, address: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")))
    }
}

fun dialNumber(context: Context, number: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.replace(" ", "")}")))
    }
}

fun shareText(context: Context, title: String, link: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$title\n$link")
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}

@Composable
fun relativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val minutes = (System.currentTimeMillis() - timestamp) / 60_000L
    return when {
        minutes < 1L -> stringResource(R.string.news_now)
        minutes < 60L -> stringResource(R.string.news_minutes, minutes.toInt())
        minutes < 60L * 24L -> stringResource(R.string.news_hours, (minutes / 60L).toInt())
        else -> stringResource(R.string.news_days, (minutes / (60L * 24L)).toInt())
    }
}

@Composable
fun SourceBadge(sourceId: String, name: String, color: String, modifier: Modifier = Modifier) {
    val logoRes = sourceLogoRes(sourceId)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        when {
            logoRes != null -> {
                Surface(color = Color.White, shape = RoundedCornerShape(5.dp)) {
                    Image(
                        painterResource(logoRes),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .height(13.dp),
                    )
                }
            }
            isVideoSource(sourceId) -> {
                Surface(color = Color.White, shape = RoundedCornerShape(5.dp)) {
                    Image(
                        painterResource(R.drawable.ic_social_youtube),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0xFFFF0000)),
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .height(13.dp),
                    )
                }
            }
            else -> {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(sourceColor(color)),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun BrandLinkCard(
    iconRes: Int,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(tint),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.Outlined.OpenInNew,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun LinkCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.Outlined.OpenInNew,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CopyRow(label: String, value: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        IconButton(onClick = {
            clipboard.setText(AnnotatedString(value))
            Toast.makeText(context, context.getString(R.string.contacts_copied), Toast.LENGTH_SHORT).show()
        }) {
            Icon(Icons.Outlined.ContentCopy, stringResource(R.string.contacts_copy))
        }
    }
}

@Composable
fun EmptyState(title: String, hint: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun formatDateLong(iso: String): String {
    val context = LocalContext.current
    val locales = context.resources.configuration.locales
    val locale = if (locales.isEmpty) java.util.Locale.GERMAN else locales[0]
    return remember(iso, locale) {
        runCatching {
            java.time.LocalDate.parse(iso)
                .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", locale))
        }.getOrDefault(iso)
    }
}

@Composable
fun countdownText(iso: String): String {
    val days = remember(iso) {
        val target = runCatching { java.time.LocalDate.parse(iso) }
            .getOrDefault(java.time.LocalDate.now())
        java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), target)
    }
    return when {
        days <= 0L -> stringResource(R.string.wahlkampf_today)
        days == 1L -> stringResource(R.string.wahlkampf_tomorrow)
        else -> stringResource(R.string.wahlkampf_in_days, days.toInt())
    }
}

@Composable
fun rememberBottomShadow(
    height: Dp = 52.dp,
    maxAlpha: Float = 0.34f,
): Modifier {
    val glass = de.afd.parteiapp.ui.glass.LocalGlassEnabled.current
    return remember(glass, height, maxAlpha) {
        if (glass) Modifier else Modifier.bottomShadow(height, maxAlpha)
    }
}

fun Modifier.bottomShadow(
    height: Dp = 52.dp,
    maxAlpha: Float = 0.34f,
): Modifier = drawWithContent {
    drawContent()
    val shadowHeight = height.toPx()
    val top = size.height - shadowHeight
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.45f to Color.Black.copy(alpha = maxAlpha * 0.3f),
            1f to Color.Black.copy(alpha = maxAlpha),
            startY = top,
            endY = size.height,
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, shadowHeight),
    )
}

fun Modifier.topShadow(
    height: Dp = 28.dp,
    alpha: Float = 0f,
): Modifier = drawWithContent {
    drawContent()
    if (alpha <= 0f) return@drawWithContent
    val shadowHeight = height.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Black.copy(alpha = alpha),
            0.5f to Color.Black.copy(alpha = alpha * 0.4f),
            1f to Color.Transparent,
            startY = 0f,
            endY = shadowHeight,
        ),
        topLeft = Offset(0f, 0f),
        size = Size(size.width, shadowHeight),
    )
}
