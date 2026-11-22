package com.nutrimuscle.app.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemePreset(
    val storageKey: String,
    val labelEn: String,
    val labelMm: String,
    val forcesDark: Boolean = false
) {
    PASTEL("pastel", "Pastel", "ပက်စတယ်"),
    SAGE("sage", "Sage", "စိမ်းနု"),
    OCEAN("ocean", "Ocean", "သမုဒ္ဒရာ"),
    SUNSET("sunset", "Sunset", "နေဝင်ချိန်"),
    LAVENDER("lavender", "Lavender", "ခရမ်းနု"),
    MONO("mono", "Mono", "အဖြူအမည်း"),
    AMOLED("amoled", "AMOLED", "AMOLED အနက်", forcesDark = true),
    DYNAMIC("dynamic", "Dynamic", "စနစ်အရောင်");

    companion object {
        fun fromKey(key: String?): ThemePreset {
            val found = entries.firstOrNull { it.storageKey == key } ?: PASTEL
            return if (found == DYNAMIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) PASTEL else found
        }

        fun availablePresets(): List<ThemePreset> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                entries
            } else {
                entries.filterNot { it == DYNAMIC }
            }
    }
}

// Default Pastel Light neutrals & accents
val LightBackground = Color(0xFFF3F4F2)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEAECE8)
val LightText = Color(0xFF1F2A2E)
val LightMutedText = Color(0xFF8A9498)
val LightOutline = Color(0xFFE3E6E2)

val PastelBlueTeal = Color(0xFFA8CED6)
val PastelSageGreen = Color(0xFFC6DBA8)
val PastelSoftYellow = Color(0xFFE9E48C)
val PastelPeach = Color(0xFFEBA77C)

// Default Pastel Dark neutrals & accents
val DarkBackground = Color(0xFF14181A)
val DarkSurface = Color(0xFF1F2528)
val DarkSurfaceVariant = Color(0xFF283034)
val DarkText = Color(0xFFEFF3F4)
val DarkMutedText = Color(0xFF8A9498)
val DarkOutline = Color(0xFF2E373C)

val PastelBlueTealDark = Color(0xFF6B9BA6)
val PastelSageGreenDark = Color(0xFF88A566)
val PastelSoftYellowDark = Color(0xFFB5AE55)
val PastelPeachDark = Color(0xFFC27B52)

val SoftErrorLight = Color(0xFFD66A5E)
val SoftErrorDark = Color(0xFFE28479)

@Immutable
data class AppColors(
    val card1: Color,
    val card2: Color,
    val card3: Color,
    val card4: Color,
    val accent: Color,
    val ring: Color,
    val ringTrack: Color,
    val success: Color,
    val warning: Color,
    val onCard: Color,
    val mutedText: Color
)

@Immutable
data class PastelPalette(
    val blueTeal: Color,
    val sageGreen: Color,
    val softYellow: Color,
    val peach: Color,
    val mutedText: Color,
    val ringTrack: Color
)

internal fun AppColors.toPastelPalette(): PastelPalette = PastelPalette(
    blueTeal = card1,
    sageGreen = card2,
    softYellow = accent,
    peach = ring,
    mutedText = mutedText,
    ringTrack = ringTrack
)

@Immutable
internal data class ThemePaletteBundle(
    val colorScheme: ColorScheme,
    val appColors: AppColors
)

// 1. Pastel (default reference)
val LightPastelAppColors = AppColors(
    card1 = PastelBlueTeal,
    card2 = PastelSageGreen,
    card3 = PastelSoftYellow,
    card4 = PastelPeach,
    accent = PastelSoftYellow,
    ring = PastelPeach,
    ringTrack = LightSurfaceVariant,
    success = PastelSageGreen,
    warning = PastelPeach,
    onCard = LightText,
    mutedText = LightMutedText
)

val DarkPastelAppColors = AppColors(
    card1 = PastelBlueTealDark,
    card2 = PastelSageGreenDark,
    card3 = PastelSoftYellowDark,
    card4 = PastelPeachDark,
    accent = PastelSoftYellowDark,
    ring = PastelPeachDark,
    ringTrack = DarkSurfaceVariant,
    success = PastelSageGreenDark,
    warning = PastelPeachDark,
    onCard = DarkText,
    mutedText = DarkMutedText
)

val LightPastelPalette = LightPastelAppColors.toPastelPalette()
val DarkPastelPalette = DarkPastelAppColors.toPastelPalette()

val LocalAppColors = compositionLocalOf { LightPastelAppColors }
val LocalPastelPalette = compositionLocalOf { LightPastelPalette }

private fun buildBundle(
    dark: Boolean,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    onSurface: Color,
    outline: Color,
    error: Color,
    appColors: AppColors
): ThemePaletteBundle {
    val scheme = if (dark) {
        darkColorScheme(
            primary = appColors.card1,
            onPrimary = appColors.onCard,
            primaryContainer = appColors.card1,
            onPrimaryContainer = appColors.onCard,
            secondary = appColors.card2,
            onSecondary = appColors.onCard,
            secondaryContainer = surfaceVariant,
            onSecondaryContainer = onSurface,
            tertiary = appColors.ring,
            onTertiary = appColors.onCard,
            tertiaryContainer = appColors.accent,
            onTertiaryContainer = appColors.onCard,
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = appColors.mutedText,
            outline = outline,
            error = error
        )
    } else {
        lightColorScheme(
            primary = appColors.card1,
            onPrimary = appColors.onCard,
            primaryContainer = appColors.card1,
            onPrimaryContainer = appColors.onCard,
            secondary = appColors.card2,
            onSecondary = appColors.onCard,
            secondaryContainer = surfaceVariant,
            onSecondaryContainer = onSurface,
            tertiary = appColors.ring,
            onTertiary = appColors.onCard,
            tertiaryContainer = appColors.accent,
            onTertiaryContainer = appColors.onCard,
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = appColors.mutedText,
            outline = outline,
            error = error
        )
    }
    return ThemePaletteBundle(colorScheme = scheme, appColors = appColors)
}

internal fun resolveStaticThemeBundle(preset: ThemePreset, dark: Boolean): ThemePaletteBundle =
    when (preset) {
        ThemePreset.PASTEL, ThemePreset.DYNAMIC -> if (!dark) {
            buildBundle(
                dark = false,
                background = LightBackground,
                surface = LightSurface,
                surfaceVariant = LightSurfaceVariant,
                onSurface = LightText,
                outline = LightOutline,
                error = SoftErrorLight,
                appColors = LightPastelAppColors
            )
        } else {
            buildBundle(
                dark = true,
                background = DarkBackground,
                surface = DarkSurface,
                surfaceVariant = DarkSurfaceVariant,
                onSurface = DarkText,
                outline = DarkOutline,
                error = SoftErrorDark,
                appColors = DarkPastelAppColors
            )
        }

        ThemePreset.SAGE -> if (!dark) {
            buildBundle(
                dark = false,
                background = Color(0xFFF2F5F0),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE6ECE2),
                onSurface = Color(0xFF1D2920),
                outline = Color(0xFFDCE4D8),
                error = SoftErrorLight,
                appColors = AppColors(
                    card1 = Color(0xFFB3CFA8),
                    card2 = Color(0xFFCDE0BE),
                    card3 = Color(0xFFDCE6A8),
                    card4 = Color(0xFFA7C7B7),
                    accent = Color(0xFFD2E2A0),
                    ring = Color(0xFF7FA876),
                    ringTrack = Color(0xFFE4ECE0),
                    success = Color(0xFFA5CA92),
                    warning = Color(0xFFDFB07C),
                    onCard = Color(0xFF1D2920),
                    mutedText = Color(0xFF7F8E81)
                )
            )
        } else {
            buildBundle(
                dark = true,
                background = Color(0xFF121814),
                surface = Color(0xFF1C241E),
                surfaceVariant = Color(0xFF263129),
                onSurface = Color(0xFFEBF2EC),
                outline = Color(0xFF2F3C32),
                error = SoftErrorDark,
                appColors = AppColors(
                    card1 = Color(0xFF5D7F57),
                    card2 = Color(0xFF6F8F64),
                    card3 = Color(0xFF8C9658),
                    card4 = Color(0xFF527968),
                    accent = Color(0xFF839656),
                    ring = Color(0xFF93BA87),
                    ringTrack = Color(0xFF263129),
                    success = Color(0xFF779C69),
                    warning = Color(0xFFC08A58),
                    onCard = Color(0xFFEBF2EC),
                    mutedText = Color(0xFF8A9A8C)
                )
            )
        }

        ThemePreset.OCEAN -> if (!dark) {
            buildBundle(
                dark = false,
                background = Color(0xFFF1F5F8),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE3ECF2),
                onSurface = Color(0xFF192732),
                outline = Color(0xFFD8E4EC),
                error = SoftErrorLight,
                appColors = AppColors(
                    card1 = Color(0xFF9EC9E2),
                    card2 = Color(0xFFB4DBE8),
                    card3 = Color(0xFFC4DFF0),
                    card4 = Color(0xFF9EBBE0),
                    accent = Color(0xFF8EC5E6),
                    ring = Color(0xFF5E9DC6),
                    ringTrack = Color(0xFFE1ECF3),
                    success = Color(0xFF9FD2C6),
                    warning = Color(0xFFE8B488),
                    onCard = Color(0xFF192732),
                    mutedText = Color(0xFF7C8F9D)
                )
            )
        } else {
            buildBundle(
                dark = true,
                background = Color(0xFF10171D),
                surface = Color(0xFF19232C),
                surfaceVariant = Color(0xFF22303C),
                onSurface = Color(0xFFEAF2F7),
                outline = Color(0xFF2A3B49),
                error = SoftErrorDark,
                appColors = AppColors(
                    card1 = Color(0xFF4F7FA0),
                    card2 = Color(0xFF4C8697),
                    card3 = Color(0xFF587C9D),
                    card4 = Color(0xFF5A6F9E),
                    accent = Color(0xFF5E93B8),
                    ring = Color(0xFF7BB3D9),
                    ringTrack = Color(0xFF22303C),
                    success = Color(0xFF5B9588),
                    warning = Color(0xFFC58D61),
                    onCard = Color(0xFFEAF2F7),
                    mutedText = Color(0xFF8699A8)
                )
            )
        }

        ThemePreset.SUNSET -> if (!dark) {
            buildBundle(
                dark = false,
                background = Color(0xFFFBF4F1),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFF2E6E1),
                onSurface = Color(0xFF2E201E),
                outline = Color(0xFFEBDDD7),
                error = SoftErrorLight,
                appColors = AppColors(
                    card1 = Color(0xFFF3B597),
                    card2 = Color(0xFFF2B5C6),
                    card3 = Color(0xFFF5D39B),
                    card4 = Color(0xFFECA19B),
                    accent = Color(0xFFF5CE8F),
                    ring = Color(0xFFE3856B),
                    ringTrack = Color(0xFFF1E5DF),
                    success = Color(0xFFC5D8A8),
                    warning = Color(0xFFE89578),
                    onCard = Color(0xFF2E201E),
                    mutedText = Color(0xFF998480)
                )
            )
        } else {
            buildBundle(
                dark = true,
                background = Color(0xFF1B1413),
                surface = Color(0xFF271E1D),
                surfaceVariant = Color(0xFF332826),
                onSurface = Color(0xFFF6ECEA),
                outline = Color(0xFF3F312F),
                error = SoftErrorDark,
                appColors = AppColors(
                    card1 = Color(0xFFB36D52),
                    card2 = Color(0xFFA8667A),
                    card3 = Color(0xFFB58E52),
                    card4 = Color(0xFFA85D5A),
                    accent = Color(0xFFC29555),
                    ring = Color(0xFFE08B70),
                    ringTrack = Color(0xFF332826),
                    success = Color(0xFF83A168),
                    warning = Color(0xFFD67E66),
                    onCard = Color(0xFFF6ECEA),
                    mutedText = Color(0xFFA38D89)
                )
            )
        }

        ThemePreset.LAVENDER -> if (!dark) {
            buildBundle(
                dark = false,
                background = Color(0xFFF5F3FA),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFEAE5F4),
                onSurface = Color(0xFF242030),
                outline = Color(0xFFE1DAF0),
                error = SoftErrorLight,
                appColors = AppColors(
                    card1 = Color(0xFFC8B8EB),
                    card2 = Color(0xFFDEC5EE),
                    card3 = Color(0xFFB9C4EE),
                    card4 = Color(0xFFE5C1DA),
                    accent = Color(0xFFD5BEF0),
                    ring = Color(0xFF9C84D6),
                    ringTrack = Color(0xFFE9E4F3),
                    success = Color(0xFFB8D4B8),
                    warning = Color(0xFFE5A98C),
                    onCard = Color(0xFF242030),
                    mutedText = Color(0xFF8C869E)
                )
            )
        } else {
            buildBundle(
                dark = true,
                background = Color(0xFF16131E),
                surface = Color(0xFF211D2C),
                surfaceVariant = Color(0xFF2C273A),
                onSurface = Color(0xFFF1EEF8),
                outline = Color(0xFF373148),
                error = SoftErrorDark,
                appColors = AppColors(
                    card1 = Color(0xFF7B69A8),
                    card2 = Color(0xFF9172A8),
                    card3 = Color(0xFF6B77A8),
                    card4 = Color(0xFF996C8C),
                    accent = Color(0xFF8972B8),
                    ring = Color(0xFFA992E0),
                    ringTrack = Color(0xFF2C273A),
                    success = Color(0xFF789B7B),
                    warning = Color(0xFFC48668),
                    onCard = Color(0xFFF1EEF8),
                    mutedText = Color(0xFF9690A8)
                )
            )
        }

        ThemePreset.MONO -> if (!dark) {
            buildBundle(
                dark = false,
                background = Color(0xFFF4F4F4),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE8E8E8),
                onSurface = Color(0xFF161616),
                outline = Color(0xFFDDDDDD),
                error = Color(0xFF4A4A4A),
                appColors = AppColors(
                    card1 = Color(0xFFE2E2E2),
                    card2 = Color(0xFFD8D8D8),
                    card3 = Color(0xFFEAEAEA),
                    card4 = Color(0xFFD0D0D0),
                    accent = Color(0xFFD8D8D8),
                    ring = Color(0xFF5E5E5E),
                    ringTrack = Color(0xFFE6E6E6),
                    success = Color(0xFF7A7A7A),
                    warning = Color(0xFF666666),
                    onCard = Color(0xFF161616),
                    mutedText = Color(0xFF787878)
                )
            )
        } else {
            buildBundle(
                dark = true,
                background = Color(0xFF111111),
                surface = Color(0xFF1C1C1C),
                surfaceVariant = Color(0xFF282828),
                onSurface = Color(0xFFF2F2F2),
                outline = Color(0xFF333333),
                error = Color(0xFFB5B5B5),
                appColors = AppColors(
                    card1 = Color(0xFF2C2C2C),
                    card2 = Color(0xFF343434),
                    card3 = Color(0xFF3E3E3E),
                    card4 = Color(0xFF262626),
                    accent = Color(0xFF424242),
                    ring = Color(0xFFA8A8A8),
                    ringTrack = Color(0xFF282828),
                    success = Color(0xFF8C8C8C),
                    warning = Color(0xFF9E9E9E),
                    onCard = Color(0xFFF2F2F2),
                    mutedText = Color(0xFF8E8E8E)
                )
            )
        }

        ThemePreset.AMOLED -> buildBundle(
            dark = true,
            background = Color(0xFF000000),
            surface = Color(0xFF0F0F0F),
            surfaceVariant = Color(0xFF1A1A1A),
            onSurface = Color(0xFFF3F5F6),
            outline = Color(0xFF242424),
            error = SoftErrorDark,
            appColors = AppColors(
                card1 = Color(0xFF19282E),
                card2 = Color(0xFF1D291D),
                card3 = Color(0xFF2B2816),
                card4 = Color(0xFF2E1E17),
                accent = Color(0xFF3A361A),
                ring = PastelPeachDark,
                ringTrack = Color(0xFF1E1E1E),
                success = PastelSageGreenDark,
                warning = PastelPeachDark,
                onCard = Color(0xFFF3F5F6),
                mutedText = Color(0xFF8A9498)
            )
        )
    }

/** Preview swatch colors for theme selector cards in Profile. */
fun ThemePreset.previewSwatches(dark: Boolean): List<Color> {
    val bundle = resolveStaticThemeBundle(this, dark = forcesDark || dark)
    return listOf(
        bundle.colorScheme.background,
        bundle.appColors.card1,
        bundle.appColors.card2,
        bundle.appColors.accent
    )
}
