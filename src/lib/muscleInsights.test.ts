import test from 'node:test';
import assert from 'node:assert/strict';
import { calculateTargets } from './nutritionCalculator';
import { calculateMuscleScore, generateDailyInsights } from './muscleInsights';
import type { FoodItem, MealEntry } from './types';

test('Muscle score hits 100 when protein, calories, and 3-meal spread align', () => {
  const targets = calculateTargets({
    weightKg: 70,
    heightCm: 175,
    age: 25,
    gender: 'MALE',
    activityLevel: 'MODERATE',
    goal: 'MUSCLE_GAIN',
    language: 'EN',
  });

  const meals: MealEntry[] = (['BREAKFAST', 'LUNCH', 'DINNER'] as const).map(
    (mealType, i) => ({
      id: `m_${i}`,
      dateIso: '2026-11-21',
      mealType,
      createdAtMillis: 1000 + i,
      items: [
        {
          foodId: 'gl_chicken_breast',
          foodNameEn: 'Chicken Breast',
          foodNameMm: 'ကြက်ရင်ပုံသား',
          quantity: 1,
          unit: 'serving',
          grams: 200,
          calories: targets.calorieTarget / 3,
          protein: targets.proteinGrams / 3,
          carbs: 40,
          fat: 10,
          fiber: 7,
        },
      ],
    })
  );

  const totals = {
    calories: targets.calorieTarget,
    protein: targets.proteinGrams,
    carbs: 120,
    fat: 30,
    fiber: 21,
  };

  const score = calculateMuscleScore(totals, targets, meals);
  assert.equal(score.proteinPoints, 50);
  assert.equal(score.calorieBalancePoints, 30);
  assert.equal(score.proteinSpreadPoints, 20);
  assert.equal(score.totalScore, 100);
});

test('Insights identify remaining protein, excess calories, and suggest muscle-friendly foods', () => {
  const targets = calculateTargets({
    weightKg: 70,
    heightCm: 175,
    age: 25,
    gender: 'MALE',
    activityLevel: 'MODERATE',
    goal: 'MUSCLE_GAIN',
    language: 'EN',
  });
  const sampleFoods: FoodItem[] = [
    {
      id: 'gl_whey_protein',
      nameEn: 'Whey Protein Isolate (1 Scoop)',
      nameMm: 'ဝေပရိုတင်းမှုန့်',
      aliases: ['whey'],
      category: 'global_foods',
      servingLabelMm: '၁ ဇွန်း (30g)',
      servingLabelEn: '1 scoop (30g)',
      defaultServingGrams: 30,
      caloriesPerServing: 112,
      proteinPerServing: 25,
      carbsPerServing: 1.5,
      fatPerServing: 0.5,
      fiberPerServing: 0.2,
      sugarPerServing: 0.5,
      caloriesPer100g: 375,
      proteinPer100g: 83,
      carbsPer100g: 5,
      fatPer100g: 1.5,
      fiberPer100g: 0.5,
      sugarPer100g: 1.7,
      defaultUnit: 'scoop',
      unitGrams: { g: 1, scoop: 30 },
      isMuscleFriendly: true,
      isEstimate: false,
    },
  ];
  const insights = generateDailyInsights(
    {
      calories: targets.calorieTarget + 400,
      protein: targets.proteinGrams - 35,
      carbs: 300,
      fat: 90,
      fiber: 8,
    },
    targets,
    [],
    sampleFoods
  );

  assert.ok(
    insights.tips.some((t) => t.type === 'protein_needed' && t.gramsRemaining === 35)
  );
  assert.ok(
    insights.tips.some((t) => t.type === 'calories_over' && t.kcalOver === 400)
  );
  assert.equal(insights.suggestedFoods[0]?.id, 'gl_whey_protein');
});
