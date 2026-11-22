package com.nutrimuscle.app.ui.addfood

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.OutdoorGrill
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrimuscle.app.R
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.FoodItem
import com.nutrimuscle.app.domain.model.MealType
import com.nutrimuscle.app.domain.model.ParsedFoodCandidate
import com.nutrimuscle.app.ui.components.PastelCard
import com.nutrimuscle.app.ui.haptics.rememberHaptics
import com.nutrimuscle.app.ui.theme.CardCornerRadius
import com.nutrimuscle.app.ui.theme.LocalAppColors
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodBottomSheet(
    onDismiss: () -> Unit,
    viewModel: AddFoodViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isBurmese = state.language == AppLanguage.MM
    val fmt = remember { NumberFormat.getIntegerInstance(Locale.US) }

    var customFoodInitialQuery by remember { mutableStateOf<String?>(null) }
    var customFoodCandidateId by remember { mutableStateOf<String?>(null) }
    var editingFoodOverride by remember { mutableStateOf<FoodItem?>(null) }

    val selectedByFoodId = remember(state.draft.candidates) {
        state.draft.candidates
            .mapNotNull { c -> c.matchedFood?.id?.let { id -> id to c } }
            .toMap()
    }
    val isSearching = state.draft.rawInput.isNotBlank()

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
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pill meal type switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MealType.entries.forEach { type ->
                    val selected = state.draft.mealType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(if (selected) appColors.accent else Color.Transparent)
                            .clickable {
                                if (!selected) haptics.tick()
                                viewModel.selectMealType(type)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = mealTypeIcon(type),
                            contentDescription = type.name,
                            tint = if (selected) appColors.onCard else appColors.mutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Rounded pill search field with soft card1 fill
            OutlinedTextField(
                value = state.draft.rawInput,
                onValueChange = viewModel::updateInput,
                placeholder = {
                    Text(
                        stringResource(R.string.food_input_placeholder),
                        color = appColors.mutedText
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = appColors.mutedText,
                        modifier = Modifier.size(19.dp)
                    )
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = appColors.card1.copy(alpha = 0.32f),
                    unfocusedContainerColor = appColors.card1.copy(alpha = 0.28f),
                    focusedBorderColor = appColors.card1,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.commitInputToCandidates() }),
                trailingIcon = {
                    if (isSearching) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = viewModel::commitInputToCandidates,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardReturn,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.updateInput("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Selected multi-select chips strip
            AnimatedVisibility(
                visible = state.draft.candidates.isNotEmpty(),
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(140))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.draft.candidates.forEach { cand ->
                        val food = cand.matchedFood
                        val label = when {
                            food == null -> cand.queryName
                            isBurmese && food.nameMm.isNotBlank() -> food.nameMm
                            else -> food.nameEn
                        }
                        val qty = if (cand.quantity % 1.0 == 0.0) cand.quantity.toInt().toString() else cand.quantity.toString()
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(appColors.card2)
                                .clickable {
                                    if (food != null) {
                                        editingFoodOverride = food
                                    } else {
                                        customFoodCandidateId = cand.id
                                        customFoodInitialQuery = cand.queryName
                                    }
                                }
                                .padding(start = 11.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$label ×$qty",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = appColors.onCard,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                tint = appColors.onCard,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        haptics.click()
                                        viewModel.removeCandidate(cand.id)
                                    }
                            )
                        }
                    }
                }
            }

            // Category pill chips (selected = accent, others = surfaceVariant)
            if (!isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BrowseCategory.entries.forEach { category ->
                        val selected = state.activeCategory == category
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) appColors.accent
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    if (!selected) haptics.tick()
                                    viewModel.selectCategory(category)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBurmese) category.labelMm else category.labelEn,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) {
                                    appColors.onCard
                                } else {
                                    appColors.mutedText
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // No-divider 12dp-spaced browsable food list with smooth fade-in
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                if (state.browsableFoods.isEmpty()) {
                    item(key = "no_result") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .clickable {
                                    customFoodCandidateId = null
                                    customFoodInitialQuery = state.draft.rawInput.trim()
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isSearching) {
                                    if (isBurmese) "မတွေ့ပါ · အစားအစာအသစ်ထည့်" else "No result · Add custom food"
                                } else {
                                    if (isBurmese) "စာရင်းမရှိသေးပါ" else "No items yet"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = appColors.mutedText
                            )
                            if (isSearching) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(appColors.success),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "+",
                                        tint = appColors.onCard,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(state.browsableFoods, key = { it.id }) { food ->
                        val selectedCandidate = selectedByFoodId[food.id]
                        FadeInItem(key = food.id) {
                            BrowsableFoodRow(
                                food = food,
                                selectedCandidate = selectedCandidate,
                                isBurmese = isBurmese,
                                onEditServing = { editingFoodOverride = food },
                                onAdd = {
                                    haptics.click()
                                    viewModel.addSuggestion(food)
                                },
                                onStep = { delta ->
                                    haptics.tick()
                                    viewModel.stepFoodQuantity(food, delta)
                                }
                            )
                        }
                    }
                }
            }

            // Sticky bottom bar: one wide accent pill "Add · {total kcal}" + count badge
            val totalKcal = state.runningCalories.roundToInt()
            val selectedCount = state.draft.candidates.count { it.isMatched }.let { count ->
                if (count > 0) count else state.previewCandidates.count { it.isMatched }
            }
            val canSave = selectedCount > 0
            val buttonLabel = when {
                !canSave -> stringResource(R.string.add_button_empty)
                state.draft.editingMealId != null -> stringResource(R.string.update_button_total, fmt.format(totalKcal))
                else -> stringResource(R.string.add_button_total, fmt.format(totalKcal))
            }

            Button(
                onClick = {
                    haptics.confirm()
                    viewModel.saveMeal(onSaved = onDismiss)
                },
                enabled = canSave,
                shape = CircleShape,
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = appColors.accent,
                    contentColor = appColors.onCard,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = appColors.mutedText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (selectedCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 9.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    customFoodInitialQuery?.let { query ->
        CompactFoodEditorDialog(
            title = stringResource(R.string.custom_food_title),
            initialName = query,
            initialServingGrams = 100.0,
            initialCalories = 200.0,
            initialProtein = 15.0,
            initialCarbs = 20.0,
            initialFat = 6.0,
            initialSugar = 0.0,
            allowRename = true,
            onDismiss = {
                customFoodInitialQuery = null
                customFoodCandidateId = null
            },
            onSave = { name, grams, kcal, p, c, f, sugar ->
                viewModel.createAndAttachCustomFood(
                    customFoodCandidateId,
                    name,
                    name,
                    grams,
                    kcal,
                    p,
                    c,
                    f,
                    sugar
                )
                customFoodInitialQuery = null
                customFoodCandidateId = null
            }
        )
    }

    editingFoodOverride?.let { food ->
        val displayName = if (isBurmese && food.nameMm.isNotBlank()) food.nameMm else food.nameEn
        CompactFoodEditorDialog(
            title = stringResource(R.string.edit_food_title),
            initialName = displayName,
            initialServingGrams = food.defaultServingGrams,
            initialCalories = food.caloriesPerServing,
            initialProtein = food.proteinPerServing,
            initialCarbs = food.carbsPerServing,
            initialFat = food.fatPerServing,
            initialSugar = food.sugarPerServing,
            allowRename = false,
            onDismiss = { editingFoodOverride = null },
            onSave = { _, grams, kcal, p, c, f, sugar ->
                viewModel.overrideFoodValues(food, grams, kcal, p, c, f, sugar)
                editingFoodOverride = null
            }
        )
    }
}

@Composable
private fun FadeInItem(
    key: String,
    content: @Composable () -> Unit
) {
    var visible by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 200))
    ) {
        content()
    }
}

@Composable
private fun BrowsableFoodRow(
    food: FoodItem,
    selectedCandidate: ParsedFoodCandidate?,
    isBurmese: Boolean,
    onEditServing: () -> Unit,
    onAdd: () -> Unit,
    onStep: (Double) -> Unit
) {
    val appColors = LocalAppColors.current
    val primaryName = if (food.nameMm.isNotBlank()) food.nameMm else food.nameEn
    val secondaryName = if (food.nameMm.isNotBlank() && !food.nameEn.equals(food.nameMm, ignoreCase = true)) {
        food.nameEn
    } else null
    val servingStr = if (isBurmese && food.servingLabelMm.isNotBlank()) food.servingLabelMm else food.servingLabelEn
    val kcal = if (selectedCandidate != null) {
        selectedCandidate.nutrition.calories.roundToInt()
    } else if (food.caloriesPerServing > 0.0) {
        food.caloriesPerServing.roundToInt()
    } else {
        (food.effectiveCaloriesPer100 * food.defaultServingGrams / 100.0).roundToInt()
    }
    val estPrefix = if (food.isEstimate) "~" else ""
    val isSelected = selectedCandidate != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isSelected) appColors.card1.copy(alpha = 0.24f)
                else MaterialTheme.colorScheme.background
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEditServing),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = primaryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (secondaryName != null) {
                Text(
                    text = secondaryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = servingStr,
                style = MaterialTheme.typography.labelSmall,
                color = appColors.mutedText,
                maxLines = 1
            )
        }

        // Small pastel kcal chip on the right
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(appColors.card1.copy(alpha = 0.3f))
                .padding(horizontal = 9.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$estPrefix$kcal kcal",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        if (selectedCandidate != null) {
            val qtyText = if (selectedCandidate.quantity % 1.0 == 0.0) {
                selectedCandidate.quantity.toInt().toString()
            } else {
                selectedCandidate.quantity.toString()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onStep(-1.0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = "-",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = qtyText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(appColors.success)
                        .clickable { onStep(1.0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "+",
                        tint = appColors.onCard,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        } else {
            // Round success "+" button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(appColors.success)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "+",
                    tint = appColors.onCard,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactFoodEditorDialog(
    title: String,
    initialName: String,
    initialServingGrams: Double,
    initialCalories: Double,
    initialProtein: Double,
    initialCarbs: Double,
    initialFat: Double,
    initialSugar: Double,
    allowRename: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Double, Double, Double) -> Unit
) {
    val appColors = LocalAppColors.current
    val haptics = rememberHaptics()
    var name by remember { mutableStateOf(initialName) }
    var servingGrams by remember { mutableStateOf(initialServingGrams.roundToInt().toString()) }
    var calories by remember { mutableStateOf(initialCalories.roundToInt().toString()) }
    var protein by remember { mutableStateOf(initialProtein.toString()) }
    var carbs by remember { mutableStateOf(initialCarbs.toString()) }
    var fat by remember { mutableStateOf(initialFat.toString()) }
    var sugar by remember { mutableStateOf(initialSugar.toString()) }

    val pillColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.background,
        unfocusedContainerColor = MaterialTheme.colorScheme.background,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedBorderColor = appColors.card1,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PastelCard(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (allowRename) name = it },
                    enabled = allowRename,
                    label = { Text(stringResource(R.string.custom_name_en)) },
                    singleLine = true,
                    shape = CircleShape,
                    colors = pillColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = servingGrams,
                        onValueChange = { servingGrams = it },
                        label = { Text(stringResource(R.string.custom_serving_g)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = CircleShape,
                        colors = pillColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text(stringResource(R.string.custom_calories)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = CircleShape,
                        colors = pillColors,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text(stringResource(R.string.custom_protein)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = CircleShape,
                        colors = pillColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text(stringResource(R.string.custom_carbs)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = CircleShape,
                        colors = pillColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text(stringResource(R.string.custom_fat)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = CircleShape,
                        colors = pillColors,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = sugar,
                    onValueChange = { sugar = it },
                    label = { Text(stringResource(R.string.custom_sugar)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = CircleShape,
                    colors = pillColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two buttons only: secondary text button + primary yellow pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.cancel_action),
                        color = appColors.mutedText
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    shape = CircleShape,
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors.accent,
                        contentColor = appColors.onCard
                    ),
                    onClick = {
                        val parsedGrams = servingGrams.toDoubleOrNull()
                        val parsedKcal = calories.toDoubleOrNull()
                        if (parsedGrams == null || parsedGrams <= 0.0 || parsedKcal == null || parsedKcal < 0.0) {
                            haptics.reject()
                            return@Button
                        }
                        haptics.confirm()
                        onSave(
                            name.ifBlank { "Custom" },
                            parsedGrams,
                            parsedKcal,
                            protein.toDoubleOrNull() ?: 0.0,
                            carbs.toDoubleOrNull() ?: 0.0,
                            fat.toDoubleOrNull() ?: 0.0,
                            sugar.toDoubleOrNull() ?: 0.0
                        )
                    }
                ) {
                    Text(
                        text = stringResource(R.string.save_action),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "myanmar_meals" -> Icons.Outlined.Restaurant
    "myanmar_snacks" -> Icons.Outlined.BakeryDining
    "street_food" -> Icons.Outlined.OutdoorGrill
    "food_court" -> Icons.Outlined.Fastfood
    "packaged_99" -> Icons.Outlined.Inventory2
    "drinks" -> Icons.Outlined.LocalCafe
    "fruits" -> Icons.Outlined.Eco
    "global_foods" -> Icons.Outlined.FitnessCenter
    else -> Icons.Outlined.Restaurant
}

fun mealTypeIcon(type: MealType): ImageVector = when (type) {
    MealType.BREAKFAST -> Icons.Outlined.FreeBreakfast
    MealType.LUNCH -> Icons.Outlined.LightMode
    MealType.DINNER -> Icons.Outlined.DarkMode
    MealType.SNACK -> Icons.Outlined.Cookie
}

fun localizedUnit(unit: String, isBurmese: Boolean): String {
    if (!isBurmese) return unit
    return when (unit.lowercase()) {
        "bowl" -> "ပန်းကန်"
        "cup" -> "ခွက်"
        "piece" -> "လုံး"
        "slice" -> "ချပ်"
        "pack" -> "ထုပ်"
        "can" -> "ဘူး"
        "bottle" -> "ပုလင်း"
        "skewer" -> "ချောင်း"
        "shot" -> "ပက်"
        "bar" -> "ခု"
        "cone" -> "ခု"
        "scoop" -> "ဇွန်း"
        "spoon" -> "ဇွန်း"
        "serving" -> "ပွဲ"
        "g" -> "ဂရမ်"
        "kg" -> "ကီလို"
        "ml" -> "မီလီ"
        else -> unit
    }
}
