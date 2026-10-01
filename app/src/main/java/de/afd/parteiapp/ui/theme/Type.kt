package de.afd.parteiapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val default = Typography()

val AfDTypography = default.copy(
    displayLarge = default.displayLarge.copy(fontFamily = AfDDisplayFont),
    displayMedium = default.displayMedium.copy(fontFamily = AfDDisplayFont),
    displaySmall = default.displaySmall.copy(fontFamily = AfDDisplayFont),
    headlineLarge = default.headlineLarge.copy(fontFamily = AfDDisplayFont),
    headlineMedium = default.headlineMedium.copy(fontFamily = AfDDisplayFont),
    headlineSmall = default.headlineSmall.copy(
        fontFamily = AfDDisplayFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = default.titleLarge.copy(
        fontFamily = AfDDisplayFont,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.25).sp,
    ),
    titleMedium = default.titleMedium.copy(
        fontFamily = AfDFont,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = default.titleSmall.copy(
        fontFamily = AfDFont,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = default.bodyLarge.copy(fontFamily = AfDFont),
    bodyMedium = default.bodyMedium.copy(fontFamily = AfDFont),
    bodySmall = default.bodySmall.copy(fontFamily = AfDFont),
    labelLarge = default.labelLarge.copy(
        fontFamily = AfDFont,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = default.labelMedium.copy(fontFamily = AfDFont),
    labelSmall = default.labelSmall.copy(fontFamily = AfDFont),
)
