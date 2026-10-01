package de.afd.parteiapp.ui.components

import androidx.compose.runtime.Composable
import de.afd.parteiapp.R

@Composable
fun sourceLogoRes(sourceId: String): Int? = when (sourceId) {
    "afd" -> R.drawable.afd_logo_light
    "afd-fraktion" -> R.drawable.logo_fraktion
    "nius" -> R.drawable.logo_nius
    "junge-freiheit" -> R.drawable.logo_junge_freiheit
    "apollo-news" -> R.drawable.logo_apollo
    "tichys-einblick" -> R.drawable.logo_tichy
    "compact" -> R.drawable.logo_compact
    "deutschlandkurier" -> R.drawable.logo_deutschlandkurier
    else -> null
}

fun isVideoSource(sourceId: String): Boolean = sourceId.startsWith("youtube")
