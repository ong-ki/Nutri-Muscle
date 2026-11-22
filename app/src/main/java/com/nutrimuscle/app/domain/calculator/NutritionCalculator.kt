package com.nutrimuscle.app.domain.calculator

import com.nutrimuscle.app.domain.model.ActivityLevel
import com.nutrimuscle.app.domain.model.BmiCategory
import com.nutrimuscle.app.domain.model.FitnessGoal
import com.nutrimuscle.app.domain.model.Gender
import com.nutrimuscle.app.domain.model.NutritionTargets
import com.nutrimuscle.app.domain.model.ProfileValidationResult
import com.nutrimuscle.app.domain.model.UserProfile
import kotlin.math.max
import kotlin.math.pow

object NutritionCalculator {

    const val MIN_AGE = 10
    const val MAX_AGE = 100
    const val MIN_WEIGHT_KG = 20.0
    const val MAX_WEIGHT_KG = 300.0
    const val MIN_HEIGHT_CM = 100.0
    const val MAX_HEIGHT_CM = 250.0

    fun validateProfile(
        weightKg: Double?,
        heightCm: Double?,
        age: Int?
    ): ProfileValidationResult {
        val ageInvalid = age == null || age < MIN_AGE || age > MAX_AGE
        val weightInvalid = weightKg == null || weightKg < MIN_WEIGHT_KG || weightKg > MAX_WEIGHT_KG
        val heightInvalid = heightCm == null || heightCm < MIN_HEIGHT_CM || heightCm > MAX_HEIGHT_CM
        return ProfileValidationResult(
            isValid = !ageInvalid && !weightInvalid && !heightInvalid,
            ageError = ageInvalid,
            weightError = weightInvalid,
            heightError = heightInvalid
        )
    }

    /**
     * Mifflin-St Jeor BMR equation:
     * Male:   10 * weight(kg) + 6.25 * height(cm) - 5 * age + 5
     * Female: 10 * weight(kg) + 6.25 * height(cm) - 5 * age - 161
     */
    fun calculateBmr(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        gender: Gender
    ): Double {
        val genderOffset = if (gender == Gender.MALE) 5.0 else -161.0
        return (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + genderOffset
    }

    fun calculateTdee(bmr: Double, activityLevel: ActivityLevel): Double {
        return bmr * activityLevel.factor
    }

    fun calculateCalorieTarget(tdee: Double, goal: FitnessGoal): Double {
        return max(0.0, tdee + goal.calorieDelta)
    }

    fun calculateProteinTargetGrams(weightKg: Double, goal: FitnessGoal): Double {
        return weightKg * goal.proteinPerKg
    }

    /**
     * Fat = 25% of target calories (9 kcal per gram).
     * Carbs = remaining calories after protein (4 kcal/g) and fat (4 kcal/g for carbs).
     */
    fun calculateFatTargetGrams(calorieTarget: Double): Double {
        return (calorieTarget * 0.25) / 9.0
    }

    fun calculateCarbsTargetGrams(
        calorieTarget: Double,
        proteinGrams: Double,
        fatGrams: Double
    ): Double {
        val proteinCalories = proteinGrams * 4.0
        val fatCalories = fatGrams * 9.0
        val remainingCalories = max(0.0, calorieTarget - proteinCalories - fatCalories)
        return remainingCalories / 4.0
    }

    fun calculateBmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0.0) return 0.0
        val heightMeters = heightCm / 100.0
        return weightKg / heightMeters.pow(2)
    }

    fun getBmiCategory(bmi: Double): BmiCategory = when {
        bmi < 18.5 -> BmiCategory.UNDERWEIGHT
        bmi < 25.0 -> BmiCategory.NORMAL
        bmi < 30.0 -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }

    fun calculateTargets(profile: UserProfile): NutritionTargets {
        val bmr = calculateBmr(profile.weightKg, profile.heightCm, profile.age, profile.gender)
        val tdee = calculateTdee(bmr, profile.activityLevel)
        val calorieTarget = calculateCalorieTarget(tdee, profile.goal)
        val proteinGrams = calculateProteinTargetGrams(profile.weightKg, profile.goal)
        val fatGrams = calculateFatTargetGrams(calorieTarget)
        val carbsGrams = calculateCarbsTargetGrams(calorieTarget, proteinGrams, fatGrams)
        val bmi = calculateBmi(profile.weightKg, profile.heightCm)
        return NutritionTargets(
            bmr = bmr,
            tdee = tdee,
            calorieTarget = calorieTarget,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            bmi = bmi,
            bmiCategory = getBmiCategory(bmi)
        )
    }
}
