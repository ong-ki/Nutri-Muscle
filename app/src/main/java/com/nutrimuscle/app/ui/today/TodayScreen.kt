package com.nutrimuscle.app.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrimuscle.app.R
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.DailyInsights
import com.nutrimuscle.app.domain.model.InsightTip
import com.nutrimuscle.app.domain.model.LoggedFoodItem
import com.nutrimuscle.app.domain.model.MealType
import com.nutrimuscle.app.domain.model.NutritionTotals
import com.nutrimuscle.app.ui.addfood.mealTypeIcon
import com.nutrimuscle.app.ui.components.CalorieRing
import com.nutrimuscle.app.ui.components.PastelCard
import com.nutrimuscle.app.ui.haptics.rememberHaptics
import com.nutrimuscle.app.ui.theme.CardCornerRadius
import com.nutrimuscle.app.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private data class TodayFoodRow(
    val mealId: Long,
    val item: LoggedFoodItem
)

private fun currentMealTypeForTime(now: LocalTime): MealType {
    val minutes = now.hour * 60 + now.minute
    return when {
        minutes < 10 * 60 + 30 -> MealType.BREAKFAST
        minutes < 15 * 60 -> MealType.LUNCH
        minutes < 21 * 60 -> MealType.DINNER
        else -> MealType.SNACK
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodayScreen(
    onOpenAddFood: (MealType?) -> Unit,
    onEditMeal: (Long) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    val isBurmese = state.profile.language == AppLanguage.MM
    val fmt = remember { NumberFormat.getIntegerInstance(Locale.US) }

    var showMuscleSheet by remember { mutableStateOf(false) }
    var expandedMeals by remember { mutableStateOf(setOf<MealType>()) }
    var pendingDeleteFood by remember { mutableStateOf<Triple<Long, Long, String>?>(null) }

    val currentMealType = remember { currentMealTypeForTime(LocalTime.now()) }
    val mealsByType = remember(state.meals) {
        state.meals.groupBy { it.mealType }
    }

    val pct = if (state.targets.calorieTarget > 0.0) {
        ((state.totals.calories / state.targets.calorieTarget) * 100.0).roundToInt().coerceIn(0, 999)
    } else 0

    val dateLabel = remember(state.dateIso) {
        runCatching {
            LocalDate.parse(state.dateIso).format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
        }.getOrDefault(state.dateIso)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 144.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // 1. Header: avatar + tiny greeting, search & bell round buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(appColors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = appColors.onCard,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isBurmese) "မင်္ဂလာပါ!" else "Hello, Athlete!",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            haptics.click()
                            onOpenAddFood(null)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.search_hint),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showMuscleSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.muscle_score_title),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Card1 "Progress": big % + date on left, CalorieRing on right
        PastelCard(
            color = appColors.card1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isBurmese) "တိုးတက်မှု" else "Progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = appColors.onCard.copy(alpha = 0.78f)
                    )
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Medium,
                        color = appColors.onCard
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.onCard.copy(alpha = 0.72f)
                    )
                }

                CalorieRing(
                    value = state.totals.calories,
                    target = state.targets.calorieTarget,
                    size = 134.dp,
                    strokeWidth = 9.dp,
                    dateKey = state.dateIso
                )
            }
        }

        // 3. Two small white cards side by side: Current Weight & Today's Calories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PastelCard(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isBurmese) "အလေးချိန်" else "Current Weight",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.mutedText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (state.profile.weightKg % 1.0 == 0.0) {
                            state.profile.weightKg.toInt().toString()
                        } else {
                            String.format(Locale.US, "%.1f", state.profile.weightKg)
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " kg",
                        style = MaterialTheme.typography.labelMedium,
                        color = appColors.mutedText,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "0.0 kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.mutedText
                )
            }

            PastelCard(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isBurmese) "ယနေ့ ကယ်လိုရီ" else "Today's Calories",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.mutedText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = fmt.format(state.totals.calories.roundToInt()),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Sparkline7Days(
                    values = state.last7DaysCalories,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                )
            }
        }

        // 4. All 4 meal time cards in order: Breakfast, Lunch, Dinner, Snack
        val orderedMealTypes = listOf(
            MealType.BREAKFAST,
            MealType.LUNCH,
            MealType.DINNER,
            MealType.SNACK
        )

        orderedMealTypes.forEach { mealType ->
            val entriesForType = mealsByType[mealType].orEmpty()
            val foods = entriesForType.flatMap { meal ->
                meal.items.map { item -> TodayFoodRow(mealId = meal.id, item = item) }
            }
            val typeTotals = entriesForType.fold(NutritionTotals()) { acc, entry -> acc + entry.totals }
            val isEmpty = foods.isEmpty()
            val isExpanded = mealType in expandedMeals
            val isCurrent = mealType == currentMealType

            val cardColor = when (mealType) {
                MealType.BREAKFAST -> appColors.card2
                MealType.LUNCH -> appColors.card1
                MealType.DINNER -> appColors.card3
                MealType.SNACK -> appColors.card4
            }
            val textColor = appColors.onCard

            PastelCard(
                color = if (isCurrent) cardColor else cardColor.copy(alpha = 0.86f),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCurrent) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = textColor.copy(alpha = 0.36f),
                                shape = RoundedCornerShape(CardCornerRadius)
                            )
                        } else {
                            Modifier
                        }
                    ),
                onClick = {
                    if (isEmpty) {
                        haptics.click()
                        onOpenAddFood(mealType)
                    } else {
                        haptics.tick()
                        expandedMeals = if (isExpanded) expandedMeals - mealType else expandedMeals + mealType
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = mealTypeIcon(mealType),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = mealTypeLabel(mealType),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Text(
                                text = "${fmt.format(typeTotals.calories.roundToInt())} kcal",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = textColor.copy(alpha = if (isEmpty) 0.58f else 0.85f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!isEmpty) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(
                                        alpha = if (isEmpty) 0.45f else 0.72f
                                    )
                                )
                                .clickable {
                                    haptics.click()
                                    onOpenAddFood(mealType)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.add_button_empty),
                                tint = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (isEmpty) 0.6f else 0.9f
                                ),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MealMacroCell(
                        label = if (isBurmese) "ပရိုတင်း" else "Protein",
                        value = "${typeTotals.protein.roundToInt()}g",
                        textColor = textColor
                    )
                    MealMacroCell(
                        label = if (isBurmese) "ကာဗို" else "Carbs",
                        value = "${typeTotals.carbs.roundToInt()}g",
                        textColor = textColor
                    )
                    MealMacroCell(
                        label = if (isBurmese) "အဆီ" else "Fat",
                        value = "${typeTotals.fat.roundToInt()}g",
                        textColor = textColor
                    )
                }

                AnimatedVisibility(visible = isExpanded && !isEmpty) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        foods.forEach { row ->
                            val itemName = if (isBurmese && row.item.foodNameMm.isNotBlank()) {
                                row.item.foodNameMm
                            } else {
                                row.item.foodNameEn
                            }
                            val est = if (row.item.isEstimate) "~" else ""
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                                    .combinedClickable(
                                        onClick = { onEditMeal(row.mealId) },
                                        onLongClick = {
                                            haptics.click()
                                            pendingDeleteFood = Triple(row.mealId, row.item.id, itemName)
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = itemName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$est${row.item.calories.roundToInt()} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.mutedText
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        // Round 56dp accent Add Food FAB sitting cleanly above FloatingNavBar
        FloatingActionButton(
            onClick = {
                haptics.click()
                onOpenAddFood(null)
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

    pendingDeleteFood?.let { (mealId, itemId, foodName) ->
        Dialog(
            onDismissRequest = { pendingDeleteFood = null },
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
                    text = if (isBurmese) "$foodName ကို ဖျက်မှာသေချာပါသလား?" else "Remove $foodName from this meal?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.mutedText
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { pendingDeleteFood = null }) {
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
                            viewModel.deleteFoodItem(mealId, itemId)
                            pendingDeleteFood = null
                        }
                    ) {
                        Text(stringResource(R.string.delete_action), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    if (showMuscleSheet && state.insights != null) {
        MuscleTipsBottomSheet(
            insights = state.insights!!,
            isBurmese = isBurmese,
            onDismiss = { showMuscleSheet = false }
        )
    }
}

@Composable
private fun MealMacroCell(label: String, value: String, textColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun Sparkline7Days(
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    val lineColor = LocalAppColors.current.ring
    val trackColor = LocalAppColors.current.ringTrack

    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val maxVal = max(values.maxOrNull() ?: 1.0, 1.0)
        val stepX = size.width / (values.size - 1)
        val path = Path()

        values.forEachIndexed { idx, v ->
            val x = idx * stepX
            val y = size.height - ((v / maxVal).toFloat() * (size.height - 4.dp.toPx())) - 2.dp.toPx()
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawLine(
            color = trackColor,
            start = Offset(0f, size.height - 1.dp.toPx()),
            end = Offset(size.width, size.height - 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleTipsBottomSheet(
    insights: DailyInsights,
    isBurmese: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val appColors = LocalAppColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = CardCornerRadius, topEnd = CardCornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(R.string.muscle_score_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(appColors.accent)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${insights.score.totalScore} / 100",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = appColors.onCard
                    )
                }
            }

            PastelCard(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ScorePartStat(
                        label = stringResource(R.string.muscle_score_protein_part),
                        value = "${insights.score.proteinPoints}/50"
                    )
                    ScorePartStat(
                        label = stringResource(R.string.muscle_score_calorie_part),
                        value = "${insights.score.calorieBalancePoints}/30"
                    )
                    ScorePartStat(
                        label = stringResource(R.string.muscle_score_spread_part),
                        value = "${insights.score.proteinSpreadPoints}/20"
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                insights.tips.forEach { tip ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = formatTip(tip),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (insights.suggestedFoods.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.suggestions_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.mutedText
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    insights.suggestedFoods.take(4).forEach { food ->
                        val name = if (isBurmese && food.nameMm.isNotBlank()) food.nameMm else food.nameEn
                        val factor = food.defaultServingGrams / 100.0
                        val prot = if (food.caloriesPerServing > 0.0) food.proteinPerServing else food.effectiveProteinPer100 * factor
                        val kcal = if (food.caloriesPerServing > 0.0) food.caloriesPerServing else food.effectiveCaloriesPer100 * factor
                        val estPrefix = if (food.isEstimate) "~" else ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(appColors.success.copy(alpha = 0.55f))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${prot.roundToInt()}g P · $estPrefix${kcal.roundToInt()} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScorePartStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun mealTypeLabel(type: MealType): String = when (type) {
    MealType.BREAKFAST -> stringResource(R.string.meal_breakfast)
    MealType.LUNCH -> stringResource(R.string.meal_lunch)
    MealType.DINNER -> stringResource(R.string.meal_dinner)
    MealType.SNACK -> stringResource(R.string.meal_snack)
}

@Composable
private fun formatTip(tip: InsightTip): String = when (tip) {
    is InsightTip.ProteinNeeded -> stringResource(R.string.tip_protein_needed, tip.gramsRemaining)
    InsightTip.ProteinGoalReached -> stringResource(R.string.tip_protein_hit)
    is InsightTip.CaloriesOver -> stringResource(R.string.tip_calories_over, tip.kcalOver)
    is InsightTip.CaloriesUnder -> stringResource(R.string.tip_calories_under, tip.kcalUnder)
    InsightTip.CaloriesOnTrack -> stringResource(R.string.tip_calories_on_track)
    is InsightTip.GoodProteinSpread -> stringResource(R.string.tip_spread_good, tip.mealCount)
    InsightTip.ImproveProteinSpread -> stringResource(R.string.tip_spread_improve)
    is InsightTip.FiberLow -> stringResource(R.string.tip_fiber_low, tip.fiberGrams)
}
