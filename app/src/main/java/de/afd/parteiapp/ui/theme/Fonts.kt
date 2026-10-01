package de.afd.parteiapp.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import de.afd.parteiapp.R

val AfDFont = FontFamily(
    Font(R.font.afd_barlow_regular, FontWeight.Normal),
    Font(R.font.afd_barlow_medium, FontWeight.Medium),
    Font(R.font.afd_barlow_semibold, FontWeight.SemiBold),
    Font(R.font.afd_barlow_bold, FontWeight.Bold),
)

val AfDDisplayFont = FontFamily(
    Font(R.font.afd_barlowcond_regular, FontWeight.Normal),
    Font(R.font.afd_barlowcond_medium, FontWeight.Medium),
    Font(R.font.afd_barlowcond_semibold, FontWeight.SemiBold),
    Font(R.font.afd_barlowcond_bold, FontWeight.Bold),
    Font(R.font.afd_barlowcond_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.afd_barlowcond_bolditalic, FontWeight.Bold, FontStyle.Italic),
)

private val OpenSansFont = FontFamily(
    Font(R.font.opensans_regular, FontWeight.Normal),
    Font(R.font.opensans_semibold, FontWeight.SemiBold),
    Font(R.font.opensans_bold, FontWeight.Bold),
)

private val InterFont = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val LibreFranklinFont = FontFamily(
    Font(R.font.librefranklin_regular, FontWeight.Normal),
    Font(R.font.librefranklin_semibold, FontWeight.SemiBold),
    Font(R.font.librefranklin_bold, FontWeight.Bold),
)

private val EbGaramondFont = FontFamily(
    Font(R.font.ebgaramond_regular, FontWeight.Normal),
    Font(R.font.ebgaramond_semibold, FontWeight.SemiBold),
    Font(R.font.ebgaramond_bold, FontWeight.Bold),
)

private val AntonFont = FontFamily(
    Font(R.font.anton_regular, FontWeight.Normal),
    Font(R.font.anton_regular, FontWeight.Bold),
)

private val MulishFont = FontFamily(
    Font(R.font.mulish_regular, FontWeight.Normal),
    Font(R.font.mulish_semibold, FontWeight.SemiBold),
    Font(R.font.mulish_bold, FontWeight.Bold),
)

private val GelasioFont = FontFamily(
    Font(R.font.gelasio_regular, FontWeight.Normal),
    Font(R.font.gelasio_semibold, FontWeight.SemiBold),
    Font(R.font.gelasio_bold, FontWeight.Bold),
)

private val MontserratFont = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold),
)

private val SourceSerifFont = FontFamily(
    Font(R.font.sourceserif_regular, FontWeight.Normal),
    Font(R.font.sourceserif_semibold, FontWeight.SemiBold),
    Font(R.font.sourceserif_bold, FontWeight.Bold),
)

private val LatoFont = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_bold, FontWeight.Bold),
)

data class SourceFonts(val title: FontFamily, val body: FontFamily)

fun sourceFonts(sourceId: String): SourceFonts = when (sourceId) {
    "afd" -> SourceFonts(AfDDisplayFont, AfDFont)
    "afd-fraktion" -> SourceFonts(AfDDisplayFont, OpenSansFont)
    "nius" -> SourceFonts(InterFont, LibreFranklinFont)
    "junge-freiheit" -> SourceFonts(EbGaramondFont, OpenSansFont)
    "apollo-news" -> SourceFonts(AntonFont, MulishFont)
    "tichys-einblick" -> SourceFonts(GelasioFont, GelasioFont)
    "compact" -> SourceFonts(MontserratFont, InterFont)
    "deutschlandkurier" -> SourceFonts(SourceSerifFont, LatoFont)
    else -> SourceFonts(AfDFont, AfDFont)
}
