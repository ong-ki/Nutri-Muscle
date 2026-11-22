package com.nutrimuscle.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Gender {
    MALE,
    FEMALE
}

enum class ActivityLevel(val factor: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9)
}

enum class FitnessGoal(val calorieDelta: Double, val proteinPerKg: Double) {
    MUSCLE_GAIN(300.0, 2.0),
    FAT_LOSS(-400.0, 1.6),
    MAINTAIN(0.0, 1.2)
}

enum class AppLanguage(val tag: String) {
    EN("en"),
    MM("my")
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class BmiCategory {
    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
    OBESE
}

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK;

    companion object {
        fun defaultForHour(hour24: Int): MealType = when (hour24) {
            in 5..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 16..21 -> DINNER
            else -> SNACK
        }
    }
}

@Serializable
data class UserProfile(
    val weightKg: Double = 65.0,
    val heightCm: Double = 170.0,
    val age: Int = 25,
    val gender: Gender = Gender.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goal: FitnessGoal = FitnessGoal.MUSCLE_GAIN,
    val language: AppLanguage = AppLanguage.EN,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

data class ProfileValidationResult(
    val isValid: Boolean,
    val ageError: Boolean = false,
    val weightError: Boolean = false,
    val heightError: Boolean = false
)

data class NutritionTargets(
    val bmr: Double,
    val tdee: Double,
    val calorieTarget: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val bmi: Double,
    val bmiCategory: BmiCategory
)

@Serializable
data class FoodItem(
    val id: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("name_my") val nameMm: String,
    val aliases: List<String> = emptyList(),
    val category: String = "custom",
    @SerialName("serving_label_my") val servingLabelMm: String = "၁ ပွဲ",
    @SerialName("serving_label_en") val servingLabelEn: String = "1 serving",
    @SerialName("serving_grams_or_ml") val defaultServingGrams: Double = 100.0,
    @SerialName("calories") val caloriesPerServing: Double = 0.0,
    @SerialName("protein_g") val proteinPerServing: Double = 0.0,
    @SerialName("carbs_g") val carbsPerServing: Double = 0.0,
    @SerialName("fat_g") val fatPerServing: Double = 0.0,
    @SerialName("fiber_g") val fiberPerServing: Double = 0.0,
    @SerialName("sugar_g") val sugarPerServing: Double = 0.0,
    @SerialName("calories_per_100") val caloriesPer100g: Double = 0.0,
    @SerialName("protein_per_100") val proteinPer100g: Double = 0.0,
    @SerialName("carbs_per_100") val carbsPer100g: Double = 0.0,
    @SerialName("fat_per_100") val fatPer100g: Double = 0.0,
    @SerialName("fiber_per_100") val fiberPer100g: Double = 0.0,
    @SerialName("sugar_per_100") val sugarPer100g: Double = 0.0,
    @SerialName("default_unit") val defaultUnit: String = "serving",
    @SerialName("unit_grams") val unitGrams: Map<String, Double> = mapOf("g" to 1.0, "serving" to 100.0),
    @SerialName("muscle_friendly") val isMuscleFriendly: Boolean = false,
    @SerialName("is_estimate") val isEstimate: Boolean = true,
    val isCustom: Boolean = false,
    val isOverride: Boolean = false,
    val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAtMillis: Long = 0L,
    val lastUsedQuantity: Double? = null,
    val lastUsedUnit: String? = null
) {
    val effectiveCaloriesPer100: Double
        get() = if (caloriesPer100g > 0.0) caloriesPer100g else (caloriesPerServing * 100.0 / defaultServingGrams.coerceAtLeast(1.0))

    val effectiveProteinPer100: Double
        get() = if (proteinPer100g > 0.0) proteinPer100g else (proteinPerServing * 100.0 / defaultServingGrams.coerceAtLeast(1.0))

    val effectiveCarbsPer100: Double
        get() = if (carbsPer100g > 0.0) carbsPer100g else (carbsPerServing * 100.0 / defaultServingGrams.coerceAtLeast(1.0))

    val effectiveFatPer100: Double
        get() = if (fatPer100g > 0.0) fatPer100g else (fatPerServing * 100.0 / defaultServingGrams.coerceAtLeast(1.0))

    val effectiveFiberPer100: Double
        get() = if (fiberPer100g > 0.0) fiberPer100g else (fiberPerServing * 100.0 / defaultServingGrams.coerceAtLeast(1.0))

    val effectiveSugarPer100: Double
        get() = if (sugarPer100g > 0.0) sugarPer100g else (sugarPerServing * 100.0 / defaultServingGrams.coerceAtLeast(1.0))
}

data class NutritionTotals(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0
) {
    operator fun plus(other: NutritionTotals): NutritionTotals = NutritionTotals(
        calories = calories + other.calories,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat,
        fiber = fiber + other.fiber,
        sugar = sugar + other.sugar
    )
}

data class ParsedFoodCandidate(
    val id: String,
    val rawSegment: String,
    val queryName: String,
    val quantity: Double,
    val unit: String,
    val matchedFood: FoodItem?,
    val matchScore: Double,
    val isMatched: Boolean
) {
    val computedGrams: Double
        get() {
            val food = matchedFood ?: return 0.0
            return when (unit.lowercase()) {
                "kg" -> quantity * 1000.0
                "g" -> quantity
                "ml" -> quantity * (food.unitGrams["ml"] ?: 1.0)
                else -> {
                    val perUnit = food.unitGrams[unit.lowercase()] ?: food.defaultServingGrams
                    quantity * perUnit
                }
            }
        }

    val nutrition: NutritionTotals
        get() {
            val food = matchedFood ?: return NutritionTotals()
            val u = unit.lowercase()
            if ((u == food.defaultUnit.lowercase() || u == "serving") && food.caloriesPerServing > 0.0) {
                return NutritionTotals(
                    calories = food.caloriesPerServing * quantity,
                    protein = food.proteinPerServing * quantity,
                    carbs = food.carbsPerServing * quantity,
                    fat = food.fatPerServing * quantity,
                    fiber = food.fiberPerServing * quantity,
                    sugar = food.sugarPerServing * quantity
                )
            }
            val factor = computedGrams / 100.0
            return NutritionTotals(
                calories = food.effectiveCaloriesPer100 * factor,
                protein = food.effectiveProteinPer100 * factor,
                carbs = food.effectiveCarbsPer100 * factor,
                fat = food.effectiveFatPer100 * factor,
                fiber = food.effectiveFiberPer100 * factor,
                sugar = food.effectiveSugarPer100 * factor
            )
        }
}

@Serializable
data class LoggedFoodItem(
    val id: Long = 0,
    val foodId: String,
    val foodNameEn: String,
    val foodNameMm: String,
    val quantity: Double,
    val unit: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double = 0.0,
    val isEstimate: Boolean = true
)

@Serializable
data class MealEntry(
    val id: Long = 0,
    val dateIso: String,
    val mealType: MealType,
    val createdAtMillis: Long,
    val items: List<LoggedFoodItem>
) {
    val totals: NutritionTotals
        get() = items.fold(NutritionTotals()) { acc, item ->
            acc + NutritionTotals(
                calories = item.calories,
                protein = item.protein,
                carbs = item.carbs,
                fat = item.fat,
                fiber = item.fiber,
                sugar = item.sugar
            )
        }
}

data class MuscleScoreBreakdown(
    val totalScore: Int,
    val proteinPoints: Int,
    val calorieBalancePoints: Int,
    val proteinSpreadPoints: Int,
    val mealsWithAdequateProtein: Int
)

sealed interface InsightTip {
    data class ProteinNeeded(val gramsRemaining: Int) : InsightTip
    data object ProteinGoalReached : InsightTip
    data class CaloriesOver(val kcalOver: Int) : InsightTip
    data class CaloriesUnder(val kcalUnder: Int) : InsightTip
    data object CaloriesOnTrack : InsightTip
    data class GoodProteinSpread(val mealCount: Int) : InsightTip
    data object ImproveProteinSpread : InsightTip
    data class FiberLow(val fiberGrams: Int) : InsightTip
}

data class DailyInsights(
    val score: MuscleScoreBreakdown,
    val tips: List<InsightTip>,
    val suggestedFoods: List<FoodItem>
)
