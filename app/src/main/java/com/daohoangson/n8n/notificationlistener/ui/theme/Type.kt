package com.daohoangson.n8n.notificationlistener.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.daohoangson.n8n.notificationlistener.R

// Listener Design System — tokens/typography.css. Geist for everything, Geist
// Mono for times, counts and package names. Both ship as variable fonts in
// res/font (OFL, see assets/licenses/geist-OFL.txt).

@OptIn(ExperimentalTextApi::class)
private fun geist(res: Int, weight: FontWeight) = Font(
    resId = res,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Geist = FontFamily(
    geist(R.font.geist, FontWeight.Normal),
    geist(R.font.geist, FontWeight.Medium),
    geist(R.font.geist, FontWeight.SemiBold),
)

val GeistMono = FontFamily(
    geist(R.font.geist_mono, FontWeight.Normal),
    geist(R.font.geist_mono, FontWeight.Medium),
)

@Immutable
data class ListenerTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyS: TextStyle,
    val label: TextStyle,
    val mono: TextStyle,
    /** AppRow name and Button label: medium 15/20. */
    val rowTitle: TextStyle,
    /** SectionHeader action, small Button and Chip: medium 13/18. */
    val action: TextStyle,
)

val ListenerType = ListenerTypography(
    display = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.03).em),
    headline = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.02).em),
    title = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em),
    body = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyS = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    label = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.06.em),
    mono = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    rowTitle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.005).em),
    action = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
)
