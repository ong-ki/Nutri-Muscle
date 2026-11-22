package com.nutrimuscle.app

import com.nutrimuscle.app.domain.calculator.NutritionCalculator
import com.nutrimuscle.app.domain.insights.MuscleInsightsEngine
import com.nutrimuscle.app.domain.model.FoodItem
import com.nutrimuscle.app.domain.model.InsightTip
import com.nutrimuscle.app.domain.model.LoggedFoodItem
import com.nutrimuscle.app.domain.model.MealEntry
import com.nutrimuscle.app.domain.model.MealType
import com.nutrimuscle.app.domain.model.NutritionTotals
import com.nutrimuscle.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleInsightsEngineTest {

    @Test
    fun `muscle score reaches high score when protein calories and meal spread align`() {
        val targets = NutritionCalculator.calculateTargets(UserProfile(weightKg = 70.0))
        val meals = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER).map { type ->
            MealEntry(
                dateIso = "2026-11-21",
                mealType = type,
                createdAtMillis = 1L,
                items = listOf(
                    LoggedFoodItem(
                        foodId = "gl_chicken_breast",
                        foodNameEn = "Chicken Breast",
                        foodNameMm = "ကြက်ရင်ပုံသား",
                        quantity = 1.0,
                        unit = "serving",
                        grams = 200.0,
                        calories = targets.calorieTarget / 3.0,
                        protein = targets.proteinGrams / 3.0,
                        carbs = 40.0,
                        fat = 10.0,
                        fiber = 6.0
                    )
                )
            )
        }
        val totals = meals.fold(NutritionTotals()) { acc, m -> acc + m.totals }
        val breakdown = MuscleInsightsEngine.calculateMuscleScore(totals, targets, meals)

        assertEquals(50, breakdown.proteinPoints)
        assertEquals(30, breakdown.calorieBalancePoints)
        assertEquals(20, breakdown.proteinSpreadPoints)
        assertEquals(100, breakdown.totalScore)
    }

    @Test
    fun `insights flag missing protein and calorie excess with food suggestions`() {
        val targets = NutritionCalculator.calculateTargets(UserProfile(weightKg = 70.0))
        val totals = NutritionTotals(
            calories = targets.calorieTarget + 400.0,
            protein = targets.proteinGrams - 35.0,
            carbs = 300.0,
            fat = 90.0,
            fiber = 8.0
        )
        val foods = listOf(
            FoodItem(
                id = "gl_whey",
                nameEn = "Whey Protein",
                nameMm = "ဝေပရိုတင်း",
                caloriesPer100g = 375.0,
                proteinPer100g = 83.0,
                carbsPer100g = 5.0,
                fatPer100g = 1.5,
                defaultServingGrams = 30.0,
                isMuscleFriendly = true
            )
        )
        val insights = MuscleInsightsEngine.generateDailyInsights(totals, targets, emptyList(), foods)
        assertTrue(insights.tips.any { it is InsightTip.ProteinNeeded && it.gramsRemaining == 35 })
        assertTrue(insights.tips.any { it is InsightTip.CaloriesOver && it.kcalOver == 400 })
        assertEquals("gl_whey", insights.suggestedFoods.first().id)
    }
}
