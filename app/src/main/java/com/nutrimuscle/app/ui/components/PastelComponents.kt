package com.nutrimuscle.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutrimuscle.app.ui.haptics.LocalHapticsPreferences
import com.nutrimuscle.app.ui.haptics.rememberHaptics
import com.nutrimuscle.app.ui.theme.CardCornerRadius
import com.nutrimuscle.app.ui.theme.LocalAppColors
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PastelCard(
    color: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .clip(RoundedCornerShape(CardCornerRadius))
        .background(color)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(16.dp)

    Column(
        modifier = baseModifier,
        content = content
    )
}

@Composable
fun CalorieRing(
    value: Double,
    target: Double,
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
    strokeWidth: Dp = 10.dp,
    dateKey: String = LocalDate.now().toString()
) {
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    val hapticsPrefs = LocalHapticsPreferences.current
    val ratio = if (target > 0.0) (value / target).toFloat().coerceIn(0f, 1f) else 0f
    val targetReached = target > 0.0 && value >= target

    LaunchedEffect(dateKey, targetReached) {
        if (targetReached && hapticsPrefs != null) {
            val lastCelebrated = hapticsPrefs.lastGoalCelebratedDateFlow.first()
            if (lastCelebrated != dateKey) {
                haptics.confirm()
                hapticsPrefs.markGoalCelebrated(dateKey)
            }
        }
    }

    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 480),
        label = "pastelCalorieRing"
    )
    val fmt = remember { NumberFormat.getIntegerInstance(Locale.US) }

    // Open gap at top (~44 degrees centered at -90°)
    val startAngle = -68f
    val totalSweep = 316f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = appColors.ringTrack,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (animatedRatio > 0f) {
                drawArc(
                    color = appColors.ring,
                    startAngle = startAngle,
                    sweepAngle = totalSweep * animatedRatio,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = fmt.format(value.roundToInt()),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Medium,
                color = appColors.onCard
            )
            Text(
                text = "kcal",
                style = MaterialTheme.typography.labelSmall,
                color = appColors.onCard.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PillGauge(
    label: String,
    value: String,
    progress: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = LocalAppColors.current.card1,
    pillWidth: Dp = 44.dp,
    pillHeight: Dp = 120.dp
) {
    val appColors = LocalAppColors.current
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 420),
        label = "pillGauge_$label"
    )
    // Ensure fill always fits the 32dp white circle cleanly
    val minFillFraction = 0.34f
    val displayFraction = minFillFraction + (1f - minFillFraction) * animatedProgress

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(pillHeight)
                .clip(CircleShape)
                .background(appColors.ringTrack),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(displayFraction)
                    .clip(CircleShape)
                    .background(fillColor)
                    .padding(top = 5.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = appColors.mutedText,
            maxLines = 1
        )
    }
}

@Composable
fun WeekStrip(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    val monday = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 7) {
            val day = monday.plusDays(i.toLong())
            val isSelected = day == selectedDate

            Column(
                modifier = Modifier
                    .width(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) appColors.card1 else Color.Transparent)
                    .clickable {
                        if (!isSelected) haptics.tick()
                        onSelectDate(day)
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dayLetters[i],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) appColors.onCard else appColors.mutedText
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) appColors.onCard else MaterialTheme.colorScheme.onBackground
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(appColors.onCard)
                    )
                } else {
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }
        }
    }
}

data class SegmentPart(
    val value: Float,
    val color: Color? = null
)

@Composable
fun SegmentedBar(
    parts: List<SegmentPart>,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    val appColors = LocalAppColors.current
    val defaultColors = listOf(appColors.card1, appColors.card3, appColors.card2)
    val total = parts.sumOf { it.value.coerceAtLeast(0f).toDouble() }.toFloat()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(appColors.ringTrack),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (total > 0f) {
            parts.forEachIndexed { idx, part ->
                val weight = (part.value / total).coerceAtLeast(0f)
                if (weight > 0.005f) {
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(part.color ?: defaultColors[idx % defaultColors.size])
                    )
                }
            }
        }
    }
}

data class FloatingNavItem(
    val key: String,
    val icon: ImageVector,
    val contentDescription: String
)

@Composable
fun FloatingNavBar(
    items: List<FloatingNavItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()

    Row(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = item.key == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) appColors.accent else Color.Transparent)
                    .clickable {
                        if (!isSelected) haptics.tick()
                        onSelect(item.key)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.contentDescription,
                    tint = if (isSelected) appColors.onCard else appColors.mutedText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
