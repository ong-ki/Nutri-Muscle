package com.nutrimuscle.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimuscle.app.data.repository.NutriRepository
import com.nutrimuscle.app.domain.calculator.NutritionCalculator
import com.nutrimuscle.app.domain.insights.MuscleInsightsEngine
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.DailyInsights
import com.nutrimuscle.app.domain.model.MealEntry
import com.nutrimuscle.app.domain.model.NutritionTargets
import com.nutrimuscle.app.domain.model.NutritionTotals
import com.nutrimuscle.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayUiState(
    val dateIso: String = LocalDate.now().toString(),
    val profile: UserProfile = UserProfile(),
    val targets: NutritionTargets = NutritionCalculator.calculateTargets(UserProfile()),
    val totals: NutritionTotals = NutritionTotals(),
    val meals: List<MealEntry> = emptyList(),
    val last7DaysCalories: List<Double> = List(7) { 0.0 },
    val insights: DailyInsights? = null
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: NutriRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val todayIso = today.toString()
    private val start7Iso = today.minusDays(6).toString()

    val uiState: StateFlow<TodayUiState> = combine(
        repository.profileFlow,
        repository.observeMealsInRange(start7Iso, todayIso),
        repository.foodsFlow
    ) { profile, rangeMeals, foods ->
        val meals = rangeMeals.filter { it.dateIso == todayIso }
        val byDate = rangeMeals.groupBy { it.dateIso }
            .mapValues { (_, list) -> list.sumOf { it.totals.calories } }
        val sparkline = (6 downTo 0).map { offset ->
            byDate[today.minusDays(offset.toLong()).toString()] ?: 0.0
        }
        val targets = NutritionCalculator.calculateTargets(profile)
        val totals = meals.fold(NutritionTotals()) { acc, meal -> acc + meal.totals }
        val insights = MuscleInsightsEngine.generateDailyInsights(totals, targets, meals, foods)
        TodayUiState(
            dateIso = todayIso,
            profile = profile,
            targets = targets,
            totals = totals,
            meals = meals,
            last7DaysCalories = sparkline,
            insights = insights
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(dateIso = todayIso)
    )

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMeal(mealId)
        }
    }

    fun deleteFoodItem(mealId: Long, itemId: Long) {
        viewModelScope.launch {
            val meal = repository.getMealById(mealId) ?: return@launch
            val remaining = meal.items.filterNot { it.id == itemId }
            if (remaining.isEmpty()) {
                repository.deleteMeal(mealId)
            } else {
                repository.saveMeal(meal.copy(items = remaining))
            }
        }
    }

    fun toggleLanguage() {
        val current = uiState.value.profile.language
        val next = if (current == AppLanguage.EN) AppLanguage.MM else AppLanguage.EN
        viewModelScope.launch {
            repository.setLanguage(next)
        }
    }
}
