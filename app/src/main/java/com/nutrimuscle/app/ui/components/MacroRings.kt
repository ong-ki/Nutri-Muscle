package com.nutrimuscle.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CalorieHeroRing(
    consumed: Double,
    target: Double,
    modifier: Modifier = Modifier,
    ringSize: Dp = 188.dp
) {
    val ratio = if (target > 0.0) (consumed / target).toFloat().coerceIn(0f, 1.25f) else 0f
    val animatedSweep by animateFloatAsState(
        targetValue = ratio.coerceAtMost(1f) * 360f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "calorie_ring_sweep"
    )
    val animatedConsumed by animateIntAsState(
        targetValue = consumed.roundToInt(),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "calorie_count"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (ratio > 1.1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val fmt = NumberFormat.getIntegerInstance(Locale.US)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(ringSize)
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = 12.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = fmt.format(animatedConsumed),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "/ ${fmt.format(target.roundToInt())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SlimMacroBarRow(
    protein: Double,
    proteinTarget: Double,
    carbs: Double,
    carbsTarget: Double,
    fat: Double,
    fatTarget: Double,
    proteinLabel: String,
    carbsLabel: String,
    fatLabel: String,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SlimMacroBarItem(
            label = proteinLabel,
            consumed = protein,
            target = proteinTarget,
            fillAlpha = 1.0f,
            accent = accent,
            modifier = Modifier.weight(1f)
        )
        SlimMacroBarItem(
            label = carbsLabel,
            consumed = carbs,
            target = carbsTarget,
            fillAlpha = 0.68f,
            accent = accent,
            modifier = Modifier.weight(1f)
        )
        SlimMacroBarItem(
            label = fatLabel,
            consumed = fat,
            target = fatTarget,
            fillAlpha = 0.42f,
            accent = accent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SlimMacroBarItem(
    label: String,
    consumed: Double,
    target: Double,
    fillAlpha: Float,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0.0) (consumed / target).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "macro_bar_$label"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val barColor = accent.copy(alpha = fillAlpha)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
        ) {
            val radius = CornerRadius(size.height / 2f, size.height / 2f)
            drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
            if (animatedProgress > 0f) {
                drawRoundRect(
                    color = barColor,
                    size = Size(size.width * animatedProgress, size.height),
                    cornerRadius = radius
                )
            }
        }
        Text(
            text = "${consumed.roundToInt()} / ${target.roundToInt()} g",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
    }
}
