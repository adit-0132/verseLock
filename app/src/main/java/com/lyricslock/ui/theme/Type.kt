package com.lyricslock.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.lyricslock.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val SpaceGroteskFamily = FontFamily(
    Font(GoogleFont("Space Grotesk"), provider, FontWeight.Light),
    Font(GoogleFont("Space Grotesk"), provider, FontWeight.Normal),
    Font(GoogleFont("Space Grotesk"), provider, FontWeight.Medium),
    Font(GoogleFont("Space Grotesk"), provider, FontWeight.SemiBold),
    Font(GoogleFont("Space Grotesk"), provider, FontWeight.Bold),
)

val DmSansFamily = FontFamily(
    Font(GoogleFont("DM Sans"), provider, FontWeight.Light),
    Font(GoogleFont("DM Sans"), provider, FontWeight.Normal),
    Font(GoogleFont("DM Sans"), provider, FontWeight.Medium),
    Font(GoogleFont("DM Sans"), provider, FontWeight.SemiBold),
    Font(GoogleFont("DM Sans"), provider, FontWeight.Bold),
)
