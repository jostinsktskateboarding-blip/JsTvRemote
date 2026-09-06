package com.js.tvremote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta de la app: fondo negro puro, acentos teal en el D-pad, rojo para power.
val TvRemoteBlack = Color(0xFF000000)
val TvRemoteSurface = Color(0xFF1C1C1E)
val TvRemotePill = Color(0xFF232326)
val TvRemotePurpleLight = Color(0xFF2DD4BF)
val TvRemotePurpleDark = Color(0xFF0F766E)
val TvRemoteOkBackground = Color(0xFF0B2E2A)
val TvRemoteOkBorder = Color(0xFF2DD4BF)
val TvRemotePower = Color(0xFFE2523F)
val TvRemoteTextPrimary = Color(0xFFF2F2F2)
val TvRemoteTextSecondary = Color(0xFF9A9A9E)

private val TvRemoteDarkColorScheme = darkColorScheme(
    primary = TvRemotePurpleLight,
    secondary = TvRemotePower,
    background = TvRemoteBlack,
    surface = TvRemoteBlack,
    onBackground = TvRemoteTextPrimary,
    onSurface = TvRemoteTextPrimary
)

@Composable
fun JsTvRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvRemoteDarkColorScheme,
        content = content
    )
}
