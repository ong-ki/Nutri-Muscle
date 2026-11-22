package com.nutrimuscle.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutrimuscle.app.data.repository.NutriRepository
import com.nutrimuscle.app.domain.calculator.NutritionCalculator
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.MealEntry
import com.nutrimuscle.app.domain.model.NutritionTargets
import com.nutrimuscle.app.domain.model.NutritionTotals
import com.nutrimuscle.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class HistoryRange(val days: Int) {
    DAYS_7(7),
    DAYS_30(30)
}

enum class HistoryMetric {
    CALORIES,
    PROTEIN
}

data class DailyChartPoint(
    val dateIso: String,
    val shortLabel: String,
    val totals: NutritionTotals,
    val meals: List<MealEntry>
)

data class HistoryUiState(
    val range: HistoryRange = HistoryRange.DAYS_7,
    val metric: HistoryMetric = HistoryMetric.CALORIES,
    val selectedDateIso: String = LocalDate.now().toString(),
    val selectedDayMeals: List<MealEntry> = emptyList(),
    val selectedDayTotals: NutritionTotals = NutritionTotals(),
    val points: List<DailyChartPoint> = emptyList(),
    val targets: NutritionTargets = NutritionCalculator.calculateTargets(UserProfile()),
    val language: AppLanguage = AppLanguage.EN,
    val statusEvent: Pair<Int, Boolean>? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: NutriRepository
) : ViewModel() {

    private val rangeFlow = MutableStateFlow(HistoryRange.DAYS_7)
    private val metricFlow = MutableStateFlow(HistoryMetric.CALORIES)
    private val selectedDateFlow = MutableStateFlow(LocalDate.now().toString())
    private val statusEventFlow = MutableStateFlow<Pair<Int, Boolean>?>(null)

    private val selectedDayMealsFlow = selectedDateFlow.flatMapLatest { dateIso ->
        repository.observeMealsForDate(dateIso)
    }

    private val rangeMealsFlow = rangeFlow.flatMapLatest { range ->
        val today = LocalDate.now()
        val start = today.minusDays((range.days - 1).toLong())
        repository.observeMealsInRange(start.toString(), today.toString())
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        rangeFlow,
        metricFlow,
        selectedDateFlow,
        selectedDayMealsFlow,
        rangeMealsFlow,
        repository.profileFlow,
        statusEventFlow
    ) { values ->
        val range = values[0] as HistoryRange
        val metric = values[1] as HistoryMetric
        val selectedDate = values[2] as String
        @Suppress("UNCHECKED_CAST")
        val dayMeals = values[3] as List<MealEntry>
        @Suppress("UNCHECKED_CAST")
        val rangeMeals = values[4] as List<MealEntry>
        val profile = values[5] as UserProfile
        @Suppress("UNCHECKED_CAST")
        val status = values[6] as Pair<Int, Boolean>?

        val targets = NutritionCalculator.calculateTargets(profile)
        val dayTotals = dayMeals.fold(NutritionTotals()) { acc, m -> acc + m.totals }

        val grouped = rangeMeals.groupBy { it.dateIso }
        val today = LocalDate.now()
        val points = (range.days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val iso = date.toString()
            val list = grouped[iso].orEmpty()
            DailyChartPoint(
                dateIso = iso,
                shortLabel = "${date.monthValue}/${date.dayOfMonth}",
                totals = list.fold(NutritionTotals()) { acc, m -> acc + m.totals },
                meals = list
            )
        }

        HistoryUiState(
            range = range,
            metric = metric,
            selectedDateIso = selectedDate,
            selectedDayMeals = dayMeals,
            selectedDayTotals = dayTotals,
            points = points,
            targets = targets,
            language = profile.language,
            statusEvent = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun setRange(range: HistoryRange) {
        rangeFlow.value = range
    }

    fun setMetric(metric: HistoryMetric) {
        metricFlow.value = metric
    }

    fun selectDate(dateIso: String) {
        selectedDateFlow.value = dateIso
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMeal(mealId)
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            runCatching { repository.importBackupJson(rawJson) }
                .onSuccess {
                    statusEventFlow.value = (statusEventFlow.value?.first ?: 0) + 1 to true
                }
                .onFailure {
                    statusEventFlow.value = (statusEventFlow.value?.first ?: 0) + 1 to false
                }
        }
    }
}
