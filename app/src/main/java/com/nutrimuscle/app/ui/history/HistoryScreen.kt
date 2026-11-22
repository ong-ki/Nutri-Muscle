package com.nutrimuscle.app.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrimuscle.app.R
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.LoggedFoodItem
import com.nutrimuscle.app.domain.model.NutritionTargets
import com.nutrimuscle.app.domain.model.NutritionTotals
import com.nutrimuscle.app.ui.components.PastelCard
import com.nutrimuscle.app.ui.components.PillGauge
import com.nutrimuscle.app.ui.components.SegmentPart
import com.nutrimuscle.app.ui.components.SegmentedBar
import com.nutrimuscle.app.ui.components.WeekStrip
import com.nutrimuscle.app.ui.haptics.rememberHaptics
import com.nutrimuscle.app.ui.theme.CardCornerRadius
import com.nutrimuscle.app.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private enum class HistoryTab {
    LOG,
    HISTORY
}

private data class DailyFoodCardEntry(
    val key: String,
    val mealId: Long,
    val item: LoggedFoodItem
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onEditMeal: (Long) -> Unit,
    onOpenAddFood: (String) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    val isBurmese = state.language == AppLanguage.MM
    val fmt = remember { NumberFormat.getIntegerInstance(Locale.US) }

    var activeTab by remember { mutableStateOf(HistoryTab.LOG) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingDeleteMealId by remember { mutableStateOf<Long?>(null) }

    val selectedDate = remember(state.selectedDateIso) {
        runCatching { LocalDate.parse(state.selectedDateIso) }.getOrDefault(LocalDate.now())
    }
    val dayTotals = state.selectedDayTotals
    val caloriesLeft = max(0, (state.targets.calorieTarget - dayTotals.calories).roundToInt())

    val foodCards = remember(state.selectedDayMeals) {
        state.selectedDayMeals.flatMap { meal ->
            meal.items.mapIndexed { idx, item ->
                DailyFoodCardEntry(
                    key = "${meal.id}_${item.id}_$idx",
                    mealId = meal.id,
                    item = item
                )
            }
        }
    }

    val segments = remember(dayTotals) {
        listOf(
            SegmentPart((dayTotals.protein * 4.0).toFloat()),
            SegmentPart((dayTotals.carbs * 4.0).toFloat()),
            SegmentPart((dayTotals.fat * 9.0).toFloat())
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title + Log/History pill switch + calendar icon button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (activeTab == HistoryTab.LOG) {
                        if (isBurmese) "ကယ်လိုရီ" else "Calories"
                    } else {
                        stringResource(R.string.nav_history)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        HistoryTabPill(
                            label = if (isBurmese) "မှတ်တမ်း" else "Log",
                            selected = activeTab == HistoryTab.LOG,
                            onClick = { activeTab = HistoryTab.LOG }
                        )
                        HistoryTabPill(
                            label = if (isBurmese) "စာရင်း" else "Stats",
                            selected = activeTab == HistoryTab.HISTORY,
                            onClick = { activeTab = HistoryTab.HISTORY }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { showDatePicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.nav_history),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (activeTab == HistoryTab.LOG) {
                // WeekStrip
                WeekStrip(
                    selectedDate = selectedDate,
                    onSelectDate = { date -> viewModel.selectDate(date.toString()) }
                )

                // "Calorie left" + big number + "kcal" + SegmentedBar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBurmese) "ကျန်ကယ်လိုရီ" else "Calorie left",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.mutedText
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = fmt.format(caloriesLeft),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = " kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = appColors.mutedText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    SegmentedBar(parts = segments, height = 10.dp)
                }

                // 2-column grid of alternating card1 and surface food cards
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 152.dp)
                ) {
                    itemsIndexed(foodCards, key = { _, entry -> entry.key }) { idx, entry ->
                        val isColored = ((idx / 2) + (idx % 2)) % 2 == 0
                        val cardColor = if (isColored) appColors.card1 else MaterialTheme.colorScheme.surface
                        val textColor = if (isColored) appColors.onCard else MaterialTheme.colorScheme.onSurface
                        val subColor = if (isColored) textColor.copy(alpha = 0.72f) else appColors.mutedText
                        var menuOpen by remember { mutableStateOf(false) }

                        val itemName = if (isBurmese && entry.item.foodNameMm.isNotBlank()) {
                            entry.item.foodNameMm
                        } else {
                            entry.item.foodNameEn
                        }
                        val est = if (entry.item.isEstimate) "~" else ""

                        PastelCard(
                            color = cardColor,
                            onClick = { onEditMeal(entry.mealId) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = itemName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = null,
                                        tint = subColor,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { menuOpen = true }
                                    )
                                    DropdownMenu(
                                        expanded = menuOpen,
                                        onDismissRequest = { menuOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.edit_action)) },
                                            onClick = {
                                                menuOpen = false
                                                onEditMeal(entry.mealId)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete_action)) },
                                            onClick = {
                                                menuOpen = false
                                                pendingDeleteMealId = entry.mealId
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "$est${entry.item.calories.roundToInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                    Text(
                                        text = "kcal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = subColor
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${entry.item.carbs.roundToInt()}g",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                    Text(
                                        text = if (isBurmese) "ကာဗို" else "carbs",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = subColor
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // History & Nutrient Overview tab
                val reversedPoints = remember(state.points) { state.points.reversed() }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 104.dp)
                ) {
                    // 1. Nutrient overview white card with 5 PillGauges
                    item(key = "overview") {
                        NutrientOverviewCard(
                            totals = dayTotals,
                            targets = state.targets
                        )
                    }

                    // 2. 7/30-day pastel bar chart with dashed target line
                    item(key = "chart") {
                        PastelHistoryChartCard(
                            points = state.points,
                            range = state.range,
                            targetCalories = state.targets.calorieTarget,
                            onSelectRange = viewModel::setRange
                        )
                    }

                    // 3. Compact day rows on white cards
                    items(reversedPoints, key = { it.dateIso }) { point ->
                        val kcal = point.totals.calories.roundToInt()
                        val prot = point.totals.protein.roundToInt()
                        val isSelected = point.dateIso == state.selectedDateIso

                        PastelCard(
                            color = if (isSelected) appColors.card1 else MaterialTheme.colorScheme.surface,
                            onClick = {
                                viewModel.selectDate(point.dateIso)
                                activeTab = HistoryTab.LOG
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = point.shortLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) {
                                        appColors.onCard
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "${prot}g P",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) {
                                            appColors.onCard.copy(alpha = 0.78f)
                                        } else {
                                            appColors.mutedText
                                        }
                                    )
                                    Text(
                                        text = "${fmt.format(kcal)} kcal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) {
                                            appColors.onCard
                                        } else if (kcal > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            appColors.mutedText
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Round 56dp accent Add Food FAB above FloatingNavBar on Daily Log tab only
        if (activeTab == HistoryTab.LOG) {
            FloatingActionButton(
                onClick = {
                    haptics.click()
                    onOpenAddFood(state.selectedDateIso)
                },
                shape = CircleShape,
                containerColor = appColors.accent,
                contentColor = appColors.onCard,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 1.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 74.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_button_empty),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(selectedDate) {
            selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(CardCornerRadius),
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            confirmButton = {
                Button(
                    shape = CircleShape,
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors.accent,
                        contentColor = appColors.onCard
                    ),
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            viewModel.selectDate(picked.toString())
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.save_action), fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel_action), color = appColors.mutedText)
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedDayContainerColor = appColors.card1,
                    selectedDayContentColor = appColors.onCard,
                    todayDateBorderColor = appColors.ring,
                    todayContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }

    pendingDeleteMealId?.let { mealId ->
        Dialog(
            onDismissRequest = { pendingDeleteMealId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PastelCard(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                Text(
                    text = stringResource(R.string.delete_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isBurmese) "ဒီမှတ်တမ်းကို ဖျက်မှာသေချာပါသလား?" else "Remove this entry from your log?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.mutedText
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { pendingDeleteMealId = null }) {
                        Text(stringResource(R.string.cancel_action), color = appColors.mutedText)
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        shape = CircleShape,
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appColors.accent,
                            contentColor = appColors.onCard
                        ),
                        onClick = {
                            haptics.click()
                            viewModel.deleteMeal(mealId)
                            pendingDeleteMealId = null
                        }
                    ) {
                        Text(stringResource(R.string.delete_action), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) appColors.accent else Color.Transparent)
            .clickable {
                if (!selected) haptics.tick()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (selected) appColors.onCard else appColors.mutedText
        )
    }
}

@Composable
private fun NutrientOverviewCard(
    totals: NutritionTotals,
    targets: NutritionTargets
) {
    val appColors = LocalAppColors.current
    val calVal = totals.calories.roundToInt()
    val calLabel = if (calVal >= 1000) String.format(Locale.US, "%.1fk", calVal / 1000.0) else "$calVal"
    val calProg = if (targets.calorieTarget > 0) (totals.calories / targets.calorieTarget).toFloat() else 0f
    val protProg = if (targets.proteinGrams > 0) (totals.protein / targets.proteinGrams).toFloat() else 0f
    val carbProg = if (targets.carbsGrams > 0) (totals.carbs / targets.carbsGrams).toFloat() else 0f
    val fatProg = if (targets.fatGrams > 0) (totals.fat / targets.fatGrams).toFloat() else 0f
    val rdcPct = (calProg * 100f).roundToInt().coerceAtLeast(0)

    PastelCard(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            PillGauge(
                label = "Cal",
                value = calLabel,
                progress = calProg,
                fillColor = appColors.ring
            )
            PillGauge(
                label = "Prot",
                value = "${totals.protein.roundToInt()}g",
                progress = protProg,
                fillColor = appColors.card1
            )
            PillGauge(
                label = "Carb",
                value = "${totals.carbs.roundToInt()}g",
                progress = carbProg,
                fillColor = appColors.card3
            )
            PillGauge(
                label = "Fats",
                value = "${totals.fat.roundToInt()}g",
                progress = fatProg,
                fillColor = appColors.card2
            )
            PillGauge(
                label = "RDC",
                value = "$rdcPct%",
                progress = calProg,
                fillColor = appColors.card1
            )
        }
    }
}

@Composable
private fun PastelHistoryChartCard(
    points: List<DailyChartPoint>,
    range: HistoryRange,
    targetCalories: Double,
    onSelectRange: (HistoryRange) -> Unit
) {
    val appColors = LocalAppColors.current

    PastelCard(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "kcal",
                style = MaterialTheme.typography.labelMedium,
                color = appColors.mutedText
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                HistoryTabPill(
                    label = stringResource(R.string.range_7_days),
                    selected = range == HistoryRange.DAYS_7,
                    onClick = { onSelectRange(HistoryRange.DAYS_7) }
                )
                HistoryTabPill(
                    label = stringResource(R.string.range_30_days),
                    selected = range == HistoryRange.DAYS_30,
                    onClick = { onSelectRange(HistoryRange.DAYS_30) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val barPrimary = appColors.card1
        val barSuccess = appColors.success
        val emptyColor = appColors.ringTrack
        val targetColor = appColors.ring

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val maxData = max(
                targetCalories * 1.2,
                max(points.maxOf { it.totals.calories } * 1.15, 100.0)
            )
            val topPad = 8.dp.toPx()
            val bottomPad = 4.dp.toPx()
            val chartH = size.height - topPad - bottomPad
            val slotW = size.width / points.size
            val barW = (slotW * 0.56f).coerceIn(3.dp.toPx(), 24.dp.toPx())
            val radius = (barW / 2f).coerceAtMost(10.dp.toPx())

            points.forEachIndexed { idx, p ->
                val value = p.totals.calories
                val ratio = (value / maxData).toFloat().coerceIn(0f, 1f)
                val barH = max(if (value > 0) 6.dp.toPx() else 3.dp.toPx(), chartH * ratio)
                val x = idx * slotW + (slotW - barW) / 2f
                val y = topPad + chartH - barH
                val fill = when {
                    value <= 0.0 -> emptyColor
                    value >= targetCalories * 0.9 -> barSuccess
                    else -> barPrimary
                }
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }

            val targetRatio = (targetCalories / maxData).toFloat().coerceIn(0f, 1f)
            val targetY = topPad + chartH - chartH * targetRatio
            drawLine(
                color = targetColor,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )
        }

        if (range == HistoryRange.DAYS_7 && points.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEach { p ->
                    Text(
                        text = p.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.mutedText,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
