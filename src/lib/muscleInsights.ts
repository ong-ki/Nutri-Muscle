import type {
  DailyInsights,
  FoodItem,
  InsightTip,
  MealEntry,
  MealType,
  MuscleScoreBreakdown,
  NutritionTargets,
  NutritionTotals,
} from './types';

export function sumMealTotals(meals: MealEntry[]): NutritionTotals {
  return meals.reduce<NutritionTotals>(
    (acc, meal) => {
      for (const item of meal.items) {
        acc.calories += item.calories;
        acc.protein += item.protein;
        acc.carbs += item.carbs;
        acc.fat += item.fat;
        acc.fiber += item.fiber;
        acc.sugar = (acc.sugar ?? 0) + (item.sugar ?? 0);
      }
      return acc;
    },
    { calories: 0, protein: 0, carbs: 0, fat: 0, fiber: 0, sugar: 0 }
  );
}

export function sumSingleMeal(meal: MealEntry): NutritionTotals {
  return sumMealTotals([meal]);
}

export function calculateMuscleScore(
  totals: NutritionTotals,
  targets: NutritionTargets,
  meals: MealEntry[]
): MuscleScoreBreakdown {
  if (totals.calories <= 0 && totals.protein <= 0) {
    return {
      totalScore: 0,
      proteinPoints: 0,
      calorieBalancePoints: 0,
      proteinSpreadPoints: 0,
      mealsWithAdequateProtein: 0,
    };
  }

  const proteinRatio =
    targets.proteinGrams > 0
      ? Math.min(1, totals.protein / targets.proteinGrams)
      : 0;
  const proteinPoints = Math.round(proteinRatio * 50);

  let calorieBalancePoints = 0;
  if (targets.calorieTarget > 0) {
    const deviationRatio =
      Math.abs(totals.calories - targets.calorieTarget) / targets.calorieTarget;
    if (deviationRatio <= 0.1) {
      calorieBalancePoints = 30;
    } else if (deviationRatio < 0.5) {
      calorieBalancePoints = Math.round(
        (1 - (deviationRatio - 0.1) / 0.4) * 30
      );
    }
  }

  const proteinByMeal: Record<MealType, number> = {
    BREAKFAST: 0,
    LUNCH: 0,
    DINNER: 0,
    SNACK: 0,
  };
  for (const m of meals) {
    proteinByMeal[m.mealType] += sumSingleMeal(m).protein;
  }

  const minPerMeal = Math.max(15, targets.proteinGrams * 0.15);
  const adequateMeals = (
    Object.values(proteinByMeal) as number[]
  ).filter((p) => p >= minPerMeal).length;

  let proteinSpreadPoints = 0;
  if (adequateMeals >= 3) proteinSpreadPoints = 20;
  else if (adequateMeals === 2) proteinSpreadPoints = 14;
  else if (adequateMeals === 1) proteinSpreadPoints = 7;

  const totalScore = Math.max(
    0,
    Math.min(100, proteinPoints + calorieBalancePoints + proteinSpreadPoints)
  );

  return {
    totalScore,
    proteinPoints,
    calorieBalancePoints,
    proteinSpreadPoints,
    mealsWithAdequateProtein: adequateMeals,
  };
}

export function suggestFoodsForRemainingMacros(
  remainingProtein: number,
  remainingCalories: number,
  allFoods: FoodItem[],
  limit = 4
): FoodItem[] {
  if (allFoods.length === 0) return [];

  const scored: Array<{ food: FoodItem; relevance: number }> = [];

  for (const food of allFoods) {
    const factor = food.defaultServingGrams / 100;
    const servingCals =
      food.caloriesPerServing > 0
        ? food.caloriesPerServing
        : food.caloriesPer100g * factor;
    const servingProt =
      food.caloriesPerServing > 0
        ? food.proteinPerServing
        : food.proteinPer100g * factor;
    const servingCarbs =
      food.caloriesPerServing > 0
        ? food.carbsPerServing
        : food.carbsPer100g * factor;

    if (servingCals <= 0) continue;
    const fitsBudget =
      remainingCalories <= 50 || servingCals <= remainingCalories + 80;
    if (!fitsBudget && !food.isMuscleFriendly) continue;

    let relevance = 0;
    if (remainingProtein > 10) {
      const proteinDensity =
        (food.proteinPer100g / Math.max(1, food.caloriesPer100g)) * 100;
      const friendlyBoost = food.isMuscleFriendly ? 15 : 0;
      const budgetBoost = servingCals <= remainingCalories + 50 ? 10 : -8;
      relevance =
        proteinDensity +
        friendlyBoost +
        budgetBoost +
        Math.min(servingProt, remainingProtein) * 0.5;
    } else {
      const budgetBoost = servingCals <= remainingCalories + 50 ? 15 : 0;
      relevance = budgetBoost + servingProt * 0.4 + servingCarbs * 0.1;
    }
    scored.push({ food, relevance });
  }

  return scored
    .sort((a, b) => b.relevance - a.relevance)
    .slice(0, limit)
    .map((x) => x.food);
}

export function generateDailyInsights(
  totals: NutritionTotals,
  targets: NutritionTargets,
  meals: MealEntry[],
  allFoods: FoodItem[]
): DailyInsights {
  const score = calculateMuscleScore(totals, targets, meals);
  const tips: InsightTip[] = [];

  const proteinDiff = Math.round(targets.proteinGrams - totals.protein);
  if (proteinDiff > 5) {
    tips.push({ type: 'protein_needed', gramsRemaining: proteinDiff });
  } else {
    tips.push({ type: 'protein_hit' });
  }

  const calDiff = Math.round(totals.calories - targets.calorieTarget);
  if (calDiff > 150) {
    tips.push({ type: 'calories_over', kcalOver: calDiff });
  } else if (calDiff < -150) {
    tips.push({ type: 'calories_under', kcalUnder: -calDiff });
  } else {
    tips.push({ type: 'calories_on_track' });
  }

  if (score.mealsWithAdequateProtein >= 3) {
    tips.push({ type: 'spread_good', mealCount: score.mealsWithAdequateProtein });
  } else {
    tips.push({ type: 'spread_improve' });
  }

  if (totals.calories > 200 && totals.fiber < 15) {
    tips.push({ type: 'fiber_low', fiberGrams: Math.round(totals.fiber) });
  }

  const suggestedFoods = suggestFoodsForRemainingMacros(
    Math.max(0, targets.proteinGrams - totals.protein),
    Math.max(0, targets.calorieTarget - totals.calories),
    allFoods
  );

  return { score, tips, suggestedFoods };
}
