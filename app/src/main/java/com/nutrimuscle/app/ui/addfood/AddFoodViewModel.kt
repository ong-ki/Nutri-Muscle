package com.nutrimuscle.app.ui.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimuscle.app.data.repository.NutriRepository
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.FoodItem
import com.nutrimuscle.app.domain.model.LoggedFoodItem
import com.nutrimuscle.app.domain.model.MealEntry
import com.nutrimuscle.app.domain.model.MealType
import com.nutrimuscle.app.domain.model.ParsedFoodCandidate
import com.nutrimuscle.app.domain.parser.FoodParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

enum class BrowseCategory(
    val key: String,
    val labelEn: String,
    val labelMm: String
) {
    RECENT("recent", "Recent", "မကြာသေးမီ"),
    FAVORITES("favorites", "Favorites", "အကြိုက်ဆုံး"),
    MYANMAR_MEALS("myanmar_meals", "Myanmar meals", "မြန်မာထမင်းဟင်း"),
    SNACKS("myanmar_snacks", "Snacks", "မုန့်"),
    STREET_FOOD("street_food", "Street food", "လမ်းဘေးစာ"),
    FOOD_COURT("food_court", "Food court", "Food court"),
    DRINKS("drinks", "Drinks", "အအေး/ကော်ဖီ"),
    SHOP_99("packaged_99", "99 shop", "99 ဆိုင်"),
    FRUITS("fruits", "Fruits", "သစ်သီး"),
    GYM("global_foods", "Gym", "Gym/Global")
}

data class AddFoodDraftState(
    val editingMealId: Long? = null,
    val dateIso: String = LocalDate.now().toString(),
    val mealType: MealType = MealType.defaultForHour(LocalTime.now().hour),
    val rawInput: String = "",
    val selectedCategory: BrowseCategory? = null,
    val candidates: List<ParsedFoodCandidate> = emptyList()
)

data class AddFoodUiState(
    val draft: AddFoodDraftState = AddFoodDraftState(),
    val activeCategory: BrowseCategory = BrowseCategory.MYANMAR_MEALS,
    val foods: List<FoodItem> = emptyList(),
    val recentFoods: List<FoodItem> = emptyList(),
    val favoriteFoods: List<FoodItem> = emptyList(),
    val browsableFoods: List<FoodItem> = emptyList(),
    val liveSuggestions: List<FoodItem> = emptyList(),
    val previewCandidates: List<ParsedFoodCandidate> = emptyList(),
    val runningCalories: Double = 0.0,
    val language: AppLanguage = AppLanguage.EN
)

@HiltViewModel
class AddFoodViewModel @Inject constructor(
    private val repository: NutriRepository
) : ViewModel() {

    private val draftFlow = MutableStateFlow(AddFoodDraftState())

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
        }
    }

    val uiState: StateFlow<AddFoodUiState> = combine(
        draftFlow,
        repository.foodsFlow,
        repository.profileFlow
    ) { draft, foods, profile ->
        val foodById = foods.associateBy { it.id }
        val syncedCandidates = draft.candidates.map { cand ->
            val fresh = cand.matchedFood?.id?.let { foodById[it] }
            if (fresh != null) cand.copy(matchedFood = fresh) else cand
        }

        val recent = foods
            .filter { it.lastUsedAtMillis > 0L }
            .sortedByDescending { it.lastUsedAtMillis }
            .take(25)
        val favorites = foods
            .filter { it.isFavorite }
            .sortedByDescending { it.usageCount }

        val resolvedCategory = draft.selectedCategory
            ?: if (recent.isNotEmpty()) BrowseCategory.RECENT else BrowseCategory.MYANMAR_MEALS

        val isSearching = draft.rawInput.isNotBlank()
        val suggestions = if (isSearching) {
            FoodParser.suggestFoods(draft.rawInput, foods, limit = 30)
        } else {
            emptyList()
        }

        val browsable = if (isSearching) {
            suggestions
        } else {
            when (resolvedCategory) {
                BrowseCategory.RECENT -> recent
                BrowseCategory.FAVORITES -> favorites
                else -> foods.filter { it.category.equals(resolvedCategory.key, ignoreCase = true) }
            }
        }

        val preview = if (isSearching && syncedCandidates.isEmpty()) {
            FoodParser.parse(draft.rawInput, foods)
        } else {
            emptyList()
        }

        val combinedCalories = (syncedCandidates + preview)
            .filter { it.isMatched }
            .sumOf { it.nutrition.calories }

        AddFoodUiState(
            draft = draft.copy(candidates = syncedCandidates),
            activeCategory = resolvedCategory,
            foods = foods,
            recentFoods = recent,
            favoriteFoods = favorites,
            browsableFoods = browsable,
            liveSuggestions = suggestions,
            previewCandidates = preview,
            runningCalories = combinedCalories,
            language = profile.language
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddFoodUiState()
    )

    fun startNewMeal(initialMealType: MealType? = null, dateIso: String = LocalDate.now().toString()) {
        viewModelScope.launch {
            repository.initializeDatabase()
        }
        val defaultMeal = initialMealType ?: MealType.defaultForHour(LocalTime.now().hour)
        draftFlow.value = AddFoodDraftState(
            editingMealId = null,
            dateIso = dateIso,
            mealType = defaultMeal,
            rawInput = "",
            selectedCategory = null,
            candidates = emptyList()
        )
    }

    fun loadMealForEditing(mealId: Long) {
        viewModelScope.launch {
            repository.initializeDatabase()
            val meal = repository.getMealById(mealId) ?: return@launch
            val foodMap = uiState.value.foods.associateBy { it.id }
            val candidates = meal.items.map { item ->
                ParsedFoodCandidate(
                    id = UUID.randomUUID().toString(),
                    rawSegment = item.foodNameEn,
                    queryName = item.foodNameEn,
                    quantity = item.quantity,
                    unit = item.unit,
                    matchedFood = foodMap[item.foodId],
                    matchScore = 1.0,
                    isMatched = foodMap[item.foodId] != null
                )
            }
            draftFlow.value = AddFoodDraftState(
                editingMealId = meal.id,
                dateIso = meal.dateIso,
                mealType = meal.mealType,
                rawInput = "",
                selectedCategory = null,
                candidates = candidates
            )
        }
    }

    fun updateInput(text: String) {
        draftFlow.update { it.copy(rawInput = text) }
    }

    fun selectCategory(category: BrowseCategory) {
        draftFlow.update { it.copy(selectedCategory = category) }
    }

    fun selectMealType(type: MealType) {
        draftFlow.update { it.copy(mealType = type) }
    }

    fun commitInputToCandidates() {
        val raw = draftFlow.value.rawInput
        if (raw.isBlank()) return
        val parsed = FoodParser.parse(raw, uiState.value.foods)
        draftFlow.update { it.copy(candidates = it.candidates + parsed, rawInput = "") }
    }

    fun addSuggestion(food: FoodItem) {
        draftFlow.update { state ->
            val existing = state.candidates.find { it.matchedFood?.id == food.id }
            if (existing != null) {
                val step = if (existing.unit == "g" || existing.unit == "ml") 25.0 else 0.5
                state.copy(
                    candidates = state.candidates.map { c ->
                        if (c.id == existing.id) c.copy(quantity = c.quantity + step) else c
                    }
                )
            } else {
                val qty = food.lastUsedQuantity ?: 1.0
                val unit = food.lastUsedUnit ?: food.defaultUnit
                val candidate = ParsedFoodCandidate(
                    id = UUID.randomUUID().toString(),
                    rawSegment = food.nameEn,
                    queryName = food.nameEn,
                    quantity = qty,
                    unit = unit,
                    matchedFood = food,
                    matchScore = 1.0,
                    isMatched = true
                )
                state.copy(candidates = state.candidates + candidate)
            }
        }
    }

    fun stepFoodQuantity(food: FoodItem, delta: Double) {
        draftFlow.update { state ->
            val existing = state.candidates.find { it.matchedFood?.id == food.id }
            if (existing == null) {
                if (delta <= 0.0) return@update state
                val qty = food.lastUsedQuantity ?: 1.0
                val unit = food.lastUsedUnit ?: food.defaultUnit
                val candidate = ParsedFoodCandidate(
                    id = UUID.randomUUID().toString(),
                    rawSegment = food.nameEn,
                    queryName = food.nameEn,
                    quantity = qty,
                    unit = unit,
                    matchedFood = food,
                    matchScore = 1.0,
                    isMatched = true
                )
                state.copy(candidates = state.candidates + candidate)
            } else {
                val stepSize = if (existing.unit == "g" || existing.unit == "ml") delta * 25.0 else delta * 0.5
                val next = existing.quantity + stepSize
                if (next <= 0.0) {
                    state.copy(candidates = state.candidates.filterNot { it.id == existing.id })
                } else {
                    state.copy(
                        candidates = state.candidates.map { c ->
                            if (c.id == existing.id) c.copy(quantity = next) else c
                        }
                    )
                }
            }
        }
    }

    fun toggleFavorite(foodId: String) {
        val food = uiState.value.foods.find { it.id == foodId } ?: return
        viewModelScope.launch {
            repository.toggleFavoriteFood(foodId, !food.isFavorite)
        }
    }

    fun stepQuantity(candidateId: String, delta: Double) {
        draftFlow.update { state ->
            val target = state.candidates.find { it.id == candidateId } ?: return@update state
            val stepSize = if (target.unit == "g" || target.unit == "ml") delta * 25.0 else delta * 0.5
            val next = target.quantity + stepSize
            if (next <= 0.0) {
                state.copy(candidates = state.candidates.filterNot { it.id == candidateId })
            } else {
                state.copy(
                    candidates = state.candidates.map { c ->
                        if (c.id == candidateId) c.copy(quantity = next) else c
                    }
                )
            }
        }
    }

    fun replaceCandidateFood(candidateId: String, replacement: FoodItem) {
        draftFlow.update { state ->
            state.copy(
                candidates = state.candidates.map { c ->
                    if (c.id == candidateId) {
                        c.copy(
                            matchedFood = replacement,
                            matchScore = 1.0,
                            isMatched = true,
                            unit = if (replacement.unitGrams.containsKey(c.unit)) c.unit else replacement.defaultUnit
                        )
                    } else c
                }
            )
        }
    }

    fun removeCandidate(candidateId: String) {
        draftFlow.update { state ->
            state.copy(candidates = state.candidates.filterNot { it.id == candidateId })
        }
    }

    fun overrideFoodValues(
        food: FoodItem,
        servingGrams: Double,
        caloriesPerServing: Double,
        proteinPerServing: Double,
        carbsPerServing: Double,
        fatPerServing: Double,
        sugarPerServing: Double
    ) {
        val safeGrams = servingGrams.coerceAtLeast(1.0)
        val scale = 100.0 / safeGrams
        val cal100 = caloriesPerServing * scale
        val prot100 = proteinPerServing * scale
        val carbs100 = carbsPerServing * scale
        val fat100 = fatPerServing * scale
        val sugar100 = sugarPerServing * scale
        val updatedUnits = food.unitGrams.toMutableMap().apply {
            put(food.defaultUnit, safeGrams)
            put("serving", safeGrams)
        }

        val updated = food.copy(
            defaultServingGrams = safeGrams,
            caloriesPerServing = caloriesPerServing,
            proteinPerServing = proteinPerServing,
            carbsPerServing = carbsPerServing,
            fatPerServing = fatPerServing,
            sugarPerServing = sugarPerServing,
            caloriesPer100g = cal100,
            proteinPer100g = prot100,
            carbsPer100g = carbs100,
            fatPer100g = fat100,
            sugarPer100g = sugar100,
            unitGrams = updatedUnits,
            isEstimate = false,
            isOverride = true
        )

        viewModelScope.launch {
            repository.saveFoodOverride(updated)
        }
    }

    fun createAndAttachCustomFood(
        candidateId: String?,
        nameEn: String,
        nameMm: String,
        servingGrams: Double,
        caloriesPerServing: Double,
        proteinPerServing: Double,
        carbsPerServing: Double,
        fatPerServing: Double,
        sugarPerServing: Double = 0.0
    ) {
        val safeGrams = servingGrams.coerceAtLeast(1.0)
        val scale = 100.0 / safeGrams
        val cal100 = caloriesPerServing * scale
        val prot100 = proteinPerServing * scale
        val carbs100 = carbsPerServing * scale
        val fat100 = fatPerServing * scale
        val sugar100 = sugarPerServing * scale
        val protPer100Kcal = if (cal100 > 0) (prot100 / cal100) * 100.0 else 0.0

        val customFood = FoodItem(
            id = "custom_${UUID.randomUUID()}",
            nameEn = nameEn.trim().ifEmpty { "Custom" },
            nameMm = nameMm.trim().ifEmpty { nameEn.trim().ifEmpty { "Custom" } },
            aliases = listOf(nameEn.trim(), nameMm.trim()).filter { it.isNotEmpty() },
            category = "custom",
            servingLabelMm = "၁ ပွဲ",
            servingLabelEn = "1 serving",
            defaultServingGrams = safeGrams,
            caloriesPerServing = caloriesPerServing,
            proteinPerServing = proteinPerServing,
            carbsPerServing = carbsPerServing,
            fatPerServing = fatPerServing,
            fiberPerServing = 0.0,
            sugarPerServing = sugarPerServing,
            caloriesPer100g = cal100,
            proteinPer100g = prot100,
            carbsPer100g = carbs100,
            fatPer100g = fat100,
            fiberPer100g = 0.0,
            sugarPer100g = sugar100,
            defaultUnit = "serving",
            unitGrams = mapOf("g" to 1.0, "serving" to safeGrams),
            isMuscleFriendly = protPer100Kcal >= 7.5 && prot100 >= 6.0,
            isEstimate = false,
            isCustom = true
        )

        viewModelScope.launch {
            repository.saveCustomFood(customFood)
            if (candidateId != null) {
                replaceCandidateFood(candidateId, customFood)
            } else {
                addSuggestion(customFood)
                draftFlow.update { it.copy(rawInput = "") }
            }
        }
    }

    fun saveMeal(onSaved: () -> Unit) {
        val current = uiState.value
        val allCandidates = if (current.draft.candidates.isNotEmpty()) {
            current.draft.candidates
        } else {
            current.previewCandidates
        }
        val validItems = allCandidates.mapNotNull { cand ->
            val food = cand.matchedFood ?: return@mapNotNull null
            val nut = cand.nutrition
            LoggedFoodItem(
                foodId = food.id,
                foodNameEn = food.nameEn,
                foodNameMm = food.nameMm,
                quantity = cand.quantity,
                unit = cand.unit,
                grams = cand.computedGrams,
                calories = nut.calories,
                protein = nut.protein,
                carbs = nut.carbs,
                fat = nut.fat,
                fiber = nut.fiber,
                sugar = nut.sugar,
                isEstimate = food.isEstimate
            )
        }
        if (validItems.isEmpty()) return

        viewModelScope.launch {
            repository.saveMeal(
                MealEntry(
                    id = current.draft.editingMealId ?: 0L,
                    dateIso = current.draft.dateIso,
                    mealType = current.draft.mealType,
                    createdAtMillis = System.currentTimeMillis(),
                    items = validItems
                )
            )
            startNewMeal()
            onSaved()
        }
    }
}
