package com.nutrimuscle.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nutrimuscle.app.domain.model.ThemeMode

private const val THEME_CROSSFADE_MS = 300

@Composable
fun NutriMuscleTheme(
    preset: ThemePreset = ThemePreset.PASTEL,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when {
        preset.forcesDark -> true
        darkTheme != null -> darkTheme
        else -> when (themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }

    val context = LocalContext.current
    val bundle = if (preset == ThemePreset.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dyn = if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        val dynAppColors = AppColors(
            card1 = dyn.primaryContainer,
            card2 = dyn.secondaryContainer,
            card3 = dyn.tertiaryContainer,
            card4 = dyn.surfaceVariant,
            accent = dyn.tertiaryContainer,
            ring = dyn.primary,
            ringTrack = dyn.surfaceVariant,
            success = dyn.secondary,
            warning = dyn.tertiary,
            onCard = dyn.onPrimaryContainer,
            mutedText = dyn.onSurfaceVariant
        )
        ThemePaletteBundle(colorScheme = dyn, appColors = dynAppColors)
    } else {
        resolveStaticThemeBundle(preset = preset, dark = effectiveDark)
    }

    val animatedScheme = animateColorScheme(bundle.colorScheme)
    val animatedAppColors = animateAppColors(bundle.appColors)
    val animatedPastels = animatedAppColors.toPastelPalette()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !effectiveDark
            controller.isAppearanceLightNavigationBars = !effectiveDark
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides animatedAppColors,
        LocalPastelPalette provides animatedPastels,
        LocalTonalElevationEnabled provides false
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = NutriTypography,
            shapes = NutriShapes,
            content = content
        )
    }
}

@Composable
private fun animColor(target: Color, label: String): Color {
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = THEME_CROSSFADE_MS),
        label = label
    )
    return animated
}

@Composable
private fun animateAppColors(target: AppColors): AppColors = AppColors(
    card1 = animColor(target.card1, "app_card1"),
    card2 = animColor(target.card2, "app_card2"),
    card3 = animColor(target.card3, "app_card3"),
    card4 = animColor(target.card4, "app_card4"),
    accent = animColor(target.accent, "app_accent"),
    ring = animColor(target.ring, "app_ring"),
    ringTrack = animColor(target.ringTrack, "app_ringTrack"),
    success = animColor(target.success, "app_success"),
    warning = animColor(target.warning, "app_warning"),
    onCard = animColor(target.onCard, "app_onCard"),
    mutedText = animColor(target.mutedText, "app_mutedText")
)

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme = target.copy(
    primary = animColor(target.primary, "cs_primary"),
    onPrimary = animColor(target.onPrimary, "cs_onPrimary"),
    primaryContainer = animColor(target.primaryContainer, "cs_primaryContainer"),
    onPrimaryContainer = animColor(target.onPrimaryContainer, "cs_onPrimaryContainer"),
    secondary = animColor(target.secondary, "cs_secondary"),
    onSecondary = animColor(target.onSecondary, "cs_onSecondary"),
    secondaryContainer = animColor(target.secondaryContainer, "cs_secondaryContainer"),
    onSecondaryContainer = animColor(target.onSecondaryContainer, "cs_onSecondaryContainer"),
    tertiary = animColor(target.tertiary, "cs_tertiary"),
    onTertiary = animColor(target.onTertiary, "cs_onTertiary"),
    tertiaryContainer = animColor(target.tertiaryContainer, "cs_tertiaryContainer"),
    onTertiaryContainer = animColor(target.onTertiaryContainer, "cs_onTertiaryContainer"),
    background = animColor(target.background, "cs_background"),
    onBackground = animColor(target.onBackground, "cs_onBackground"),
    surface = animColor(target.surface, "cs_surface"),
    onSurface = animColor(target.onSurface, "cs_onSurface"),
    surfaceVariant = animColor(target.surfaceVariant, "cs_surfaceVariant"),
    onSurfaceVariant = animColor(target.onSurfaceVariant, "cs_onSurfaceVariant"),
    outline = animColor(target.outline, "cs_outline"),
    error = animColor(target.error, "cs_error")
)
