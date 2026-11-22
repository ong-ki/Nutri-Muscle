export type Gender = 'MALE' | 'FEMALE';

export type ActivityLevel =
  | 'SEDENTARY'
  | 'LIGHT'
  | 'MODERATE'
  | 'ACTIVE'
  | 'VERY_ACTIVE';

export type FitnessGoal = 'MUSCLE_GAIN' | 'FAT_LOSS' | 'MAINTAIN';

export type AppLanguage = 'EN' | 'MM';

export type ThemeMode = 'SYSTEM' | 'LIGHT' | 'DARK';

export type BmiCategory = 'UNDERWEIGHT' | 'NORMAL' | 'OVERWEIGHT' | 'OBESE';

export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK';

export interface UserProfile {
  weightKg: number;
  heightCm: number;
  age: number;
  gender: Gender;
  activityLevel: ActivityLevel;
  goal: FitnessGoal;
  language: AppLanguage;
  themeMode?: ThemeMode;
}

export interface ProfileValidationResult {
  isValid: boolean;
  ageError: boolean;
  weightError: boolean;
  heightError: boolean;
}

export interface NutritionTargets {
  bmr: number;
  tdee: number;
  calorieTarget: number;
  proteinGrams: number;
  carbsGrams: number;
  fatGrams: number;
  bmi: number;
  bmiCategory: BmiCategory;
}

export interface FoodItem {
  id: string;
  nameEn: string;
  nameMm: string;
  aliases: string[];
  category: string;
  servingLabelMm: string;
  servingLabelEn: string;
  defaultServingGrams: number;
  caloriesPerServing: number;
  proteinPerServing: number;
  carbsPerServing: number;
  fatPerServing: number;
  fiberPerServing: number;
  sugarPerServing: number;
  caloriesPer100g: number;
  proteinPer100g: number;
  carbsPer100g: number;
  fatPer100g: number;
  fiberPer100g: number;
  sugarPer100g: number;
  defaultUnit: string;
  unitGrams: Record<string, number>;
  isMuscleFriendly: boolean;
  isEstimate: boolean;
  isCustom?: boolean;
  isOverride?: boolean;
  isFavorite?: boolean;
  usageCount?: number;
  lastUsedAtMillis?: number;
  lastUsedQuantity?: number;
  lastUsedUnit?: string;
}

export interface FoodUsageRecord {
  usageCount: number;
  lastUsedAtMillis: number;
  lastUsedQuantity: number;
  lastUsedUnit: string;
}

export interface NutritionTotals {
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  fiber: number;
  sugar?: number;
}

export interface ParsedFoodCandidate {
  id: string;
  rawSegment: string;
  queryName: string;
  quantity: number;
  unit: string;
  matchedFood: FoodItem | null;
  matchScore: number;
  isMatched: boolean;
}

export interface LoggedFoodItem {
  foodId: string;
  foodNameEn: string;
  foodNameMm: string;
  quantity: number;
  unit: string;
  grams: number;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  fiber: number;
  sugar?: number;
  isEstimate?: boolean;
}

export interface MealEntry {
  id: string;
  dateIso: string;
  mealType: MealType;
  createdAtMillis: number;
  items: LoggedFoodItem[];
}

export interface MuscleScoreBreakdown {
  totalScore: number;
  proteinPoints: number;
  calorieBalancePoints: number;
  proteinSpreadPoints: number;
  mealsWithAdequateProtein: number;
}

export type InsightTip =
  | { type: 'protein_needed'; gramsRemaining: number }
  | { type: 'protein_hit' }
  | { type: 'calories_over'; kcalOver: number }
  | { type: 'calories_under'; kcalUnder: number }
  | { type: 'calories_on_track' }
  | { type: 'spread_good'; mealCount: number }
  | { type: 'spread_improve' }
  | { type: 'fiber_low'; fiberGrams: number };

export interface DailyInsights {
  score: MuscleScoreBreakdown;
  tips: InsightTip[];
  suggestedFoods: FoodItem[];
}

export interface NutriMuscleBackup {
  version: number;
  exportedAtMillis: number;
  profile: UserProfile;
  customFoods: FoodItem[];
  foodOverrides?: Record<string, FoodItem>;
  favoriteIds?: string[];
  meals: MealEntry[];
}
