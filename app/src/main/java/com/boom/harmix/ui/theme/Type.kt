package com.boom.harmix.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.boom.harmix.R

val Exo2Family = FontFamily(
    Font(R.font.exo2_regular, FontWeight.Normal),
    Font(R.font.exo2_bold, FontWeight.Bold)
)

// We override the default typography styles to use Exo 2 globally across the app
val HarmixTypography = androidx.compose.material3.Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    displayMedium = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    displaySmall = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    headlineSmall = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family, fontWeight = FontWeight.Bold),
    titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family, fontWeight = FontWeight.Bold),
    titleMedium = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family, fontWeight = FontWeight.Medium),
    titleSmall = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    bodySmall = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    labelMedium = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family),
    labelSmall = androidx.compose.ui.text.TextStyle(fontFamily = Exo2Family)
)
