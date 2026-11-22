package com.nutrimuscle.app.domain.insights

import com.nutrimuscle.app.domain.model.DailyInsights
import com.nutrimuscle.app.domain.model.FoodItem
import com.nutrimuscle.app.domain.model.InsightTip
import com.nutrimuscle.app.domain.model.MealEntry
import com.nutrimuscle.app.domain.model.MealType
import com.nutrimuscle.app.domain.model.MuscleScoreBreakdown
import com.nutrimuscle.app.domain.model.NutritionTargets
import com.nutrimuscle.app.domain.model.NutritionTotals
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object MuscleInsightsEngine {

    /**
     * Computes a deterministic 0-100 Muscle Score from:
     * 1. Protein progress (0..50 pts)
     * 2. Calorie balance vs target (0..30 pts)
     * 3. Protein spread across distinct meals (0..20 pts)
     */
    fun calculateMuscleScore(
        totals: NutritionTotals,
        targets: NutritionTargets,
        meals: List<MealEntry>
    ): MuscleScoreBreakdown {
        if (totals.calories <= 0.0 && totals.protein <= 0.0) {
            return MuscleScoreBreakdown(
                totalScore = 0,
                proteinPoints = 0,
                calorieBalancePoints = 0,
                proteinSpreadPoints = 0,
                mealsWithAdequateProtein = 0
            )
        }

        // 1. Protein progress (up to 50 pts)
        val proteinRatio = if (targets.proteinGrams > 0.0) {
            min(1.0, totals.protein / targets.proteinGrams)
        } else 0.0
        val proteinPoints = (proteinRatio * 50.0).roundToInt()

        // 2. Calorie balance vs target (up to 30 pts)
        // Within ±10% of target = full 30 pts; tapers linearly to 0 at ±50% deviation.
        val calPoints = if (targets.calorieTarget > 0.0) {
            val deviationRatio = abs(totals.calories - targets.calorieTarget) / targets.calorieTarget
            when {
                deviationRatio <= 0.10 -> 30
                deviationRatio >= 0.50 -> 0
                else -> ((1.0 - (deviationRatio - 0.10) / 0.40) * 30.0).roundToInt()
            }
        } else 0

        // 3. Protein spread across meals (up to 20 pts)
        val proteinByMeal = meals.groupBy { it.mealType }
            .mapValues { (_, list) -> list.sumOf { it.totals.protein } }
        val minPerMealThreshold = max(15.0, targets.proteinGrams * 0.15)
        val adequateMeals = MealType.entries.count { type ->
            (proteinByMeal[type] ?: 0.0) >= minPerMealThreshold
        }
        val spreadPoints = when {
            adequateMeals >= 3 -> 20
            adequateMeals == 2 -> 14
            adequateMeals == 1 -> 7
            else -> 0
        }

        val total = (proteinPoints + calPoints + spreadPoints).coerceIn(0, 100)
        return MuscleScoreBreakdown(
            totalScore = total,
            proteinPoints = proteinPoints,
            calorieBalancePoints = calPoints,
            proteinSpreadPoints = spreadPoints,
            mealsWithAdequateProtein = adequateMeals
        )
    }

    fun generateDailyInsights(
        totals: NutritionTotals,
        targets: NutritionTargets,
        meals: List<MealEntry>,
        allFoods: List<FoodItem>
    ): DailyInsights {
        val score = calculateMuscleScore(totals, targets, meals)
        val tips = mutableListOf<InsightTip>()

        val proteinDiff = (targets.proteinGrams - totals.protein).roundToInt()
        if (proteinDiff > 5) {
            tips += InsightTip.ProteinNeeded(proteinDiff)
        } else {
            tips += InsightTip.ProteinGoalReached
        }

        val calDiff = (totals.calories - targets.calorieTarget).roundToInt()
        when {
            calDiff > 150 -> tips += InsightTip.CaloriesOver(calDiff)
            calDiff < -150 -> tips += InsightTip.CaloriesUnder(-calDiff)
            else -> tips += InsightTip.CaloriesOnTrack
        }

        if (score.mealsWithAdequateProtein >= 3) {
            tips += InsightTip.GoodProteinSpread(score.mealsWithAdequateProtein)
        } else {
            tips += InsightTip.ImproveProteinSpread
        }

        if (totals.calories > 200.0 && totals.fiber < 15.0) {
            tips += InsightTip.FiberLow(totals.fiber.roundToInt())
        }

        val suggestedFoods = suggestFoodsForRemainingMacros(
            remainingProtein = max(0.0, targets.proteinGrams - totals.protein),
            remainingCalories = max(0.0, targets.calorieTarget - totals.calories),
            allFoods = allFoods
        )

        return DailyInsights(
            score = score,
            tips = tips,
            suggestedFoods = suggestedFoods
        )
    }

    fun suggestFoodsForRemainingMacros(
        remainingProtein: Double,
        remainingCalories: Double,
        allFoods: List<FoodItem>,
        limit: Int = 4
    ): List<FoodItem> {
        if (allFoods.isEmpty()) return emptyList()

        val scored = allFoods.mapNotNull { food ->
            val servingFactor = food.defaultServingGrams / 100.0
            val servingCals = if (food.caloriesPerServing > 0.0) food.caloriesPerServing else food.effectiveCaloriesPer100 * servingFactor
            val servingProt = if (food.caloriesPerServing > 0.0) food.proteinPerServing else food.effectiveProteinPer100 * servingFactor
            val servingCarbs = if (food.caloriesPerServing > 0.0) food.carbsPerServing else food.effectiveCarbsPer100 * servingFactor

            if (servingCals <= 0.0) return@mapNotNull null

            val fitsCalorieBudget = remainingCalories <= 50.0 || servingCals <= remainingCalories + 80.0
            if (!fitsCalorieBudget && !food.isMuscleFriendly) return@mapNotNull null

            val relevance = if (remainingProtein > 10.0) {
                val proteinDensity = (food.effectiveProteinPer100 / max(1.0, food.effectiveCaloriesPer100)) * 100.0
                val friendlyBoost = if (food.isMuscleFriendly) 15.0 else 0.0
                val budgetBoost = if (servingCals <= remainingCalories + 50.0) 10.0 else -8.0
                proteinDensity + friendlyBoost + budgetBoost + min(servingProt, remainingProtein) * 0.5
            } else {
                val balancedBoost = if (servingCals <= remainingCalories + 50.0) 15.0 else 0.0
                balancedBoost + (servingProt * 0.4) + (servingCarbs * 0.1)
            }
            food to relevance
        }

        return scored
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}
