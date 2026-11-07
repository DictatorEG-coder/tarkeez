package app.tarkeez

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection

// Palette taken from the "الديكتاتور" cover: near-black, crimson, aged gold.
val Ink = Color(0xFF0A0405)
val Graphite = Color(0xFF14080A)
val GraphiteHigh = Color(0xFF230C10)
val Crimson = Color(0xFFD62F3B)
val Gold = Color(0xFFD4AF6A)

private val Scheme = darkColorScheme(
    primary = Crimson,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3A1519),
    onPrimaryContainer = Color(0xFFFFD9DB),
    secondary = Gold,
    onSecondary = Color(0xFF1A1205),
    secondaryContainer = Color(0xFF3A1519),
    onSecondaryContainer = Color(0xFFF2E9E4),
    background = Ink,
    onBackground = Color(0xFFF2E9E4),
    surface = Ink,
    onSurface = Color(0xFFF2E9E4),
    surfaceVariant = Graphite,
    onSurfaceVariant = Color(0xFFA08A86),
    surfaceContainer = Graphite,
    surfaceContainerHigh = GraphiteHigh,
    error = Color(0xFFFF7A59),
    errorContainer = Color(0xFF2A140E),
    onErrorContainer = Color(0xFFFFB59E),
    outline = Color(0xFF3A1519),
    outlineVariant = Color(0xFF2A1013),
)

private val PlexArabic = FontFamily(
    Font(R.font.plex_arabic_regular, FontWeight.Normal),
    Font(R.font.plex_arabic_medium, FontWeight.Medium),
    Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.plex_arabic_semibold, FontWeight.Bold),
)

private val Base = Typography()
private val AppTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = PlexArabic),
    displayMedium = Base.displayMedium.copy(fontFamily = PlexArabic),
    displaySmall = Base.displaySmall.copy(fontFamily = PlexArabic),
    headlineLarge = Base.headlineLarge.copy(fontFamily = PlexArabic),
    headlineMedium = Base.headlineMedium.copy(fontFamily = PlexArabic),
    headlineSmall = Base.headlineSmall.copy(fontFamily = PlexArabic),
    titleLarge = Base.titleLarge.copy(fontFamily = PlexArabic),
    titleMedium = Base.titleMedium.copy(fontFamily = PlexArabic),
    titleSmall = Base.titleSmall.copy(fontFamily = PlexArabic),
    bodyLarge = Base.bodyLarge.copy(fontFamily = PlexArabic),
    bodyMedium = Base.bodyMedium.copy(fontFamily = PlexArabic),
    bodySmall = Base.bodySmall.copy(fontFamily = PlexArabic),
    labelLarge = Base.labelLarge.copy(fontFamily = PlexArabic),
    labelMedium = Base.labelMedium.copy(fontFamily = PlexArabic),
    labelSmall = Base.labelSmall.copy(fontFamily = PlexArabic),
)

@Composable
fun TarkeezTheme(content: @Composable () -> Unit) {
    // The whole UI is Arabic, so force right-to-left whatever the phone language is.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
    }
}
