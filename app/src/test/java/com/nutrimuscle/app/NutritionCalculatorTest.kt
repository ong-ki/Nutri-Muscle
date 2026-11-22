package com.nutrimuscle.app

import com.nutrimuscle.app.domain.calculator.NutritionCalculator
import com.nutrimuscle.app.domain.model.ActivityLevel
import com.nutrimuscle.app.domain.model.BmiCategory
import com.nutrimuscle.app.domain.model.FitnessGoal
import com.nutrimuscle.app.domain.model.Gender
import com.nutrimuscle.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun `mifflin st jeor bmr calculates accurately for male and female`() {
        val maleBmr = NutritionCalculator.calculateBmr(
            weightKg = 70.0,
            heightCm = 175.0,
            age = 25,
            gender = Gender.MALE
        )
        // 10*70 + 6.25*175 - 5*25 + 5 = 700 + 1093.75 - 125 + 5 = 1673.75
        assertEquals(1673.75, maleBmr, 0.01)

        val femaleBmr = NutritionCalculator.calculateBmr(
            weightKg = 55.0,
            heightCm = 162.0,
            age = 28,
            gender = Gender.FEMALE
        )
        // 10*55 + 6.25*162 - 5*28 - 161 = 550 + 1012.5 - 140 - 161 = 1261.5
        assertEquals(1261.5, femaleBmr, 0.01)
    }

    @Test
    fun `tdee and targets apply exact activity and goal multipliers`() {
        val profile = UserProfile(
            weightKg = 70.0,
            heightCm = 175.0,
            age = 25,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATE,
            goal = FitnessGoal.MUSCLE_GAIN
        )
        val targets = NutritionCalculator.calculateTargets(profile)
        val expectedTdee = 1673.75 * 1.55 // 2594.3125
        val expectedCalTarget = expectedTdee + 300.0 // 2894.3125
        val expectedProtein = 70.0 * 2.0 // 140.0
        val expectedFat = (expectedCalTarget * 0.25) / 9.0
        val expectedCarbs = (expectedCalTarget - expectedProtein * 4.0 - expectedFat * 9.0) / 4.0

        assertEquals(expectedTdee, targets.tdee, 0.01)
        assertEquals(expectedCalTarget, targets.calorieTarget, 0.01)
        assertEquals(expectedProtein, targets.proteinGrams, 0.01)
        assertEquals(expectedFat, targets.fatGrams, 0.01)
        assertEquals(expectedCarbs, targets.carbsGrams, 0.01)
    }

    @Test
    fun `fat loss and maintain goals use correct deltas and protein per kg`() {
        val fatLoss = NutritionCalculator.calculateTargets(
            UserProfile(weightKg = 80.0, heightCm = 175.0, age = 30, goal = FitnessGoal.FAT_LOSS, activityLevel = ActivityLevel.SEDENTARY)
        )
        assertEquals(fatLoss.tdee - 400.0, fatLoss.calorieTarget, 0.01)
        assertEquals(80.0 * 1.6, fatLoss.proteinGrams, 0.01)

        val maintain = NutritionCalculator.calculateTargets(
            UserProfile(weightKg = 60.0, heightCm = 165.0, age = 22, goal = FitnessGoal.MAINTAIN, activityLevel = ActivityLevel.VERY_ACTIVE)
        )
        assertEquals(maintain.tdee, maintain.calorieTarget, 0.01)
        assertEquals(60.0 * 1.2, maintain.proteinGrams, 0.01)
    }

    @Test
    fun `bmi calculation and categories are accurate`() {
        assertEquals(BmiCategory.UNDERWEIGHT, NutritionCalculator.getBmiCategory(17.8))
        assertEquals(BmiCategory.NORMAL, NutritionCalculator.getBmiCategory(22.5))
        assertEquals(BmiCategory.OVERWEIGHT, NutritionCalculator.getBmiCategory(27.0))
        assertEquals(BmiCategory.OBESE, NutritionCalculator.getBmiCategory(31.2))
    }

    @Test
    fun `profile validation enforces age weight and height bounds`() {
        assertTrue(NutritionCalculator.validateProfile(65.0, 170.0, 25).isValid)
        assertTrue(NutritionCalculator.validateProfile(20.0, 100.0, 10).isValid)
        assertTrue(NutritionCalculator.validateProfile(300.0, 250.0, 100).isValid)

        val badAge = NutritionCalculator.validateProfile(65.0, 170.0, 9)
        assertFalse(badAge.isValid)
        assertTrue(badAge.ageError)

        val badWeight = NutritionCalculator.validateProfile(301.0, 170.0, 25)
        assertFalse(badWeight.isValid)
        assertTrue(badWeight.weightError)

        val badHeight = NutritionCalculator.validateProfile(65.0, 99.0, 25)
        assertFalse(badHeight.isValid)
        assertTrue(badHeight.heightError)
    }
}
