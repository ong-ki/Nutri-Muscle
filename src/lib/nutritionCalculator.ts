import type {
  ActivityLevel,
  BmiCategory,
  FitnessGoal,
  Gender,
  NutritionTargets,
  ProfileValidationResult,
  UserProfile,
} from './types';

export const ACTIVITY_FACTORS: Record<ActivityLevel, number> = {
  SEDENTARY: 1.2,
  LIGHT: 1.375,
  MODERATE: 1.55,
  ACTIVE: 1.725,
  VERY_ACTIVE: 1.9,
};

export const GOAL_CONFIG: Record<
  FitnessGoal,
  { calorieDelta: number; proteinPerKg: number }
> = {
  MUSCLE_GAIN: { calorieDelta: 300, proteinPerKg: 2.0 },
  FAT_LOSS: { calorieDelta: -400, proteinPerKg: 1.6 },
  MAINTAIN: { calorieDelta: 0, proteinPerKg: 1.2 },
};

export const MIN_AGE = 10;
export const MAX_AGE = 100;
export const MIN_WEIGHT_KG = 20;
export const MAX_WEIGHT_KG = 300;
export const MIN_HEIGHT_CM = 100;
export const MAX_HEIGHT_CM = 250;

export function validateProfile(
  weightKg: number | null | undefined,
  heightCm: number | null | undefined,
  age: number | null | undefined
): ProfileValidationResult {
  const ageError =
    age == null || !Number.isFinite(age) || age < MIN_AGE || age > MAX_AGE;
  const weightError =
    weightKg == null ||
    !Number.isFinite(weightKg) ||
    weightKg < MIN_WEIGHT_KG ||
    weightKg > MAX_WEIGHT_KG;
  const heightError =
    heightCm == null ||
    !Number.isFinite(heightCm) ||
    heightCm < MIN_HEIGHT_CM ||
    heightCm > MAX_HEIGHT_CM;

  return {
    isValid: !ageError && !weightError && !heightError,
    ageError,
    weightError,
    heightError,
  };
}

export function calculateBmr(
  weightKg: number,
  heightCm: number,
  age: number,
  gender: Gender
): number {
  const genderOffset = gender === 'MALE' ? 5 : -161;
  return 10 * weightKg + 6.25 * heightCm - 5 * age + genderOffset;
}

export function calculateTdee(bmr: number, activityLevel: ActivityLevel): number {
  return bmr * ACTIVITY_FACTORS[activityLevel];
}

export function calculateCalorieTarget(tdee: number, goal: FitnessGoal): number {
  return Math.max(0, tdee + GOAL_CONFIG[goal].calorieDelta);
}

export function calculateProteinTargetGrams(
  weightKg: number,
  goal: FitnessGoal
): number {
  return weightKg * GOAL_CONFIG[goal].proteinPerKg;
}

export function calculateFatTargetGrams(calorieTarget: number): number {
  return (calorieTarget * 0.25) / 9;
}

export function calculateCarbsTargetGrams(
  calorieTarget: number,
  proteinGrams: number,
  fatGrams: number
): number {
  const remainingCalories = Math.max(
    0,
    calorieTarget - proteinGrams * 4 - fatGrams * 9
  );
  return remainingCalories / 4;
}

export function calculateBmi(weightKg: number, heightCm: number): number {
  if (heightCm <= 0) return 0;
  const heightM = heightCm / 100;
  return weightKg / (heightM * heightM);
}

export function getBmiCategory(bmi: number): BmiCategory {
  if (bmi < 18.5) return 'UNDERWEIGHT';
  if (bmi < 25.0) return 'NORMAL';
  if (bmi < 30.0) return 'OVERWEIGHT';
  return 'OBESE';
}

export function defaultMealTypeForHour(hour24: number): 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK' {
  if (hour24 >= 5 && hour24 <= 10) return 'BREAKFAST';
  if (hour24 >= 11 && hour24 <= 15) return 'LUNCH';
  if (hour24 >= 16 && hour24 <= 21) return 'DINNER';
  return 'SNACK';
}

export function calculateTargets(profile: UserProfile): NutritionTargets {
  const bmr = calculateBmr(
    profile.weightKg,
    profile.heightCm,
    profile.age,
    profile.gender
  );
  const tdee = calculateTdee(bmr, profile.activityLevel);
  const calorieTarget = calculateCalorieTarget(tdee, profile.goal);
  const proteinGrams = calculateProteinTargetGrams(
    profile.weightKg,
    profile.goal
  );
  const fatGrams = calculateFatTargetGrams(calorieTarget);
  const carbsGrams = calculateCarbsTargetGrams(
    calorieTarget,
    proteinGrams,
    fatGrams
  );
  const bmi = calculateBmi(profile.weightKg, profile.heightCm);
  return {
    bmr,
    tdee,
    calorieTarget,
    proteinGrams,
    carbsGrams,
    fatGrams,
    bmi,
    bmiCategory: getBmiCategory(bmi),
  };
}
