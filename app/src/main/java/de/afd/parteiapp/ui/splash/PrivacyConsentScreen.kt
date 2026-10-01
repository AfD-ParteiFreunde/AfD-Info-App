package de.afd.parteiapp.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.afd.parteiapp.R
import de.afd.parteiapp.ui.glass.GlassButton
import de.afd.parteiapp.ui.theme.AfDBlueBright

enum class PrivacyExit { CLOSE, REVIEW_CLOSE }

@Composable
fun PrivacyConsentScreen(
    onAccept: () -> Unit,
    onExit: (PrivacyExit) -> Unit,
    reviewMode: Boolean = false,
) {
    var showDecline by remember { mutableStateOf(false) }
    val statusBars = WindowInsets.statusBars.asPaddingValues()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF04121F), Color(0xFF0A2A43), Color(0xFF0B4C74)),
                )
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = statusBars.calculateTopPadding() + 18.dp,
                ),
        ) {
            Text(
                text = stringResource(R.string.privacy_title),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    if (reviewMode) R.string.privacy_intro_review else R.string.privacy_intro
                ),
                color = Color(0xFFB8CEE0),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0x14FFFFFF), RoundedCornerShape(18.dp)),
            ) {
                Text(
                    text = stringResource(R.string.privacy_body),
                    color = Color(0xFFDCE8F2),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.privacy_footer),
                color = Color(0xFF7B96AC),
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(14.dp))
            if (reviewMode) {
                GlassButton(
                    onClick = { onExit(PrivacyExit.REVIEW_CLOSE) },
                    modifier = Modifier.fillMaxWidth(),
                    tint = AfDBlueBright.copy(alpha = 0.85f),
                    contentColor = Color(0xFF04121F),
                ) {
                    Text(
                        stringResource(R.string.privacy_close),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlassButton(
                        onClick = { showDecline = true },
                        modifier = Modifier.weight(1f),
                        tint = Color(0x1FFFFFFF),
                        contentColor = Color.White,
                    ) {
                        Text(
                            stringResource(R.string.privacy_decline),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    GlassButton(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        tint = AfDBlueBright.copy(alpha = 0.85f),
                        contentColor = Color(0xFF04121F),
                    ) {
                        Text(
                            stringResource(R.string.privacy_accept),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showDecline) {
        AlertDialog(
            onDismissRequest = { showDecline = false },
            title = { Text(stringResource(R.string.privacy_decline_title)) },
            text = { Text(stringResource(R.string.privacy_decline_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDecline = false
                    onExit(PrivacyExit.CLOSE)
                }) { Text(stringResource(R.string.privacy_decline_close)) }
            },
            dismissButton = {
                TextButton(onClick = { showDecline = false }) {
                    Text(stringResource(R.string.privacy_decline_back))
                }
            },
        )
    }
}
