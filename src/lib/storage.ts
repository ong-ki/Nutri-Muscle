import myanmarMealsJson from '../../app/src/main/assets/myanmar_meals.json';
import myanmarSnacksJson from '../../app/src/main/assets/myanmar_snacks.json';
import streetFoodJson from '../../app/src/main/assets/street_food.json';
import foodCourtJson from '../../app/src/main/assets/food_court.json';
import packaged99Json from '../../app/src/main/assets/packaged_99.json';
import drinksJson from '../../app/src/main/assets/drinks.json';
import fruitsJson from '../../app/src/main/assets/fruits.json';
import globalFoodsJson from '../../app/src/main/assets/global_foods.json';
import type {
  FoodItem,
  FoodUsageRecord,
  MealEntry,
  ThemeMode,
  UserProfile,
} from './types';

const STORAGE_KEYS = {
  PROFILE: 'nutrimuscle_profile_v1',
  MEALS: 'nutrimuscle_meals_v1',
  CUSTOM_FOODS: 'nutrimuscle_custom_foods_v1',
  FOOD_OVERRIDES: 'nutrimuscle_food_overrides_v1',
  FAVORITE_IDS: 'nutrimuscle_food_favorites_v1',
  FOOD_USAGE: 'nutrimuscle_food_usage_v1',
  THEME: 'nutrimuscle_theme_v1',
};

export interface RawAssetFood {
  id: string;
  name_my: string;
  name_en: string;
  aliases: string[];
  category: string;
  serving_label_my: string;
  serving_label_en: string;
  serving_grams_or_ml: number;
  calories: number;
  protein_g: number;
  carbs_g: number;
  fat_g: number;
  fiber_g: number;
  sugar_g: number;
  calories_per_100: number;
  protein_per_100: number;
  carbs_per_100: number;
  fat_per_100: number;
  fiber_per_100: number;
  sugar_per_100: number;
  default_unit: string;
  unit_grams: Record<string, number>;
  muscle_friendly: boolean;
  is_estimate: boolean;
}

export function mapRawJsonFood(raw: RawAssetFood): FoodItem {
  const grams = raw.serving_grams_or_ml > 0 ? raw.serving_grams_or_ml : 100;
  return {
    id: raw.id,
    nameEn: raw.name_en,
    nameMm: raw.name_my,
    aliases: Array.isArray(raw.aliases) ? raw.aliases : [],
    category: raw.category,
    servingLabelMm: raw.serving_label_my || '၁ ပွဲ',
    servingLabelEn: raw.serving_label_en || '1 serving',
    defaultServingGrams: grams,
    caloriesPerServing: raw.calories,
    proteinPerServing: raw.protein_g,
    carbsPerServing: raw.carbs_g,
    fatPerServing: raw.fat_g,
    fiberPerServing: raw.fiber_g,
    sugarPerServing: raw.sugar_g,
    caloriesPer100g: raw.calories_per_100,
    proteinPer100g: raw.protein_per_100,
    carbsPer100g: raw.carbs_per_100,
    fatPer100g: raw.fat_per_100,
    fiberPer100g: raw.fiber_per_100,
    sugarPer100g: raw.sugar_per_100,
    defaultUnit: raw.default_unit || 'serving',
    unitGrams: raw.unit_grams || { g: 1, serving: grams },
    isMuscleFriendly: Boolean(raw.muscle_friendly),
    isEstimate: Boolean(raw.is_estimate),
    isCustom: false,
    isOverride: false,
    isFavorite: false,
    usageCount: 0,
    lastUsedAtMillis: 0,
  };
}

const ALL_RAW_CATEGORIES: RawAssetFood[] = [
  ...(myanmarMealsJson as unknown as RawAssetFood[]),
  ...(myanmarSnacksJson as unknown as RawAssetFood[]),
  ...(streetFoodJson as unknown as RawAssetFood[]),
  ...(foodCourtJson as unknown as RawAssetFood[]),
  ...(packaged99Json as unknown as RawAssetFood[]),
  ...(drinksJson as unknown as RawAssetFood[]),
  ...(fruitsJson as unknown as RawAssetFood[]),
  ...(globalFoodsJson as unknown as RawAssetFood[]),
];

export const BUILTIN_FOODS: FoodItem[] = ALL_RAW_CATEGORIES.map(mapRawJsonFood);

export const DEFAULT_PROFILE: UserProfile = {
  weightKg: 68,
  heightCm: 173,
  age: 26,
  gender: 'MALE',
  activityLevel: 'MODERATE',
  goal: 'MUSCLE_GAIN',
  language: 'EN',
  themeMode: 'SYSTEM',
};

export function todayIsoDate(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function offsetIsoDate(daysAgo: number): string {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function createInitialSeedMeals(): MealEntry[] {
  const today = todayIsoDate();
  const d1 = offsetIsoDate(1);
  const d2 = offsetIsoDate(2);
  const d3 = offsetIsoDate(3);
  const d4 = offsetIsoDate(4);

  return [
    {
      id: 'seed_today_breakfast',
      dateIso: today,
      mealType: 'BREAKFAST',
      createdAtMillis: Date.now() - 3600_000 * 4,
      items: [
        {
          foodId: 'mm_boiled_egg',
          foodNameEn: 'Boiled Egg',
          foodNameMm: 'ကြက်ဥပြုတ်',
          quantity: 3,
          unit: 'piece',
          grams: 150,
          calories: 232.5,
          protein: 18.9,
          carbs: 1.65,
          fat: 15.9,
          fiber: 0,
          isEstimate: true,
        },
        {
          foodId: 'mm_steamed_rice',
          foodNameEn: 'Steamed White Rice',
          foodNameMm: 'ထမင်း',
          quantity: 1,
          unit: 'bowl',
          grams: 200,
          calories: 260,
          protein: 5.4,
          carbs: 56.4,
          fat: 0.6,
          fiber: 0.8,
          isEstimate: true,
        },
        {
          foodId: 'st_grilled_chicken',
          foodNameEn: 'Street Grilled Chicken',
          foodNameMm: 'ကြက်ကင်',
          quantity: 1,
          unit: 'serving',
          grams: 150,
          calories: 275,
          protein: 35,
          carbs: 3,
          fat: 13.5,
          fiber: 0,
          isEstimate: true,
        },
      ],
    },
    {
      id: 'seed_d1_lunch',
      dateIso: d1,
      mealType: 'LUNCH',
      createdAtMillis: Date.now() - 86400_000,
      items: [
        {
          foodId: 'mm_shan_noodles',
          foodNameEn: 'Shan Noodles',
          foodNameMm: 'ရှမ်းခေါက်ဆွဲ',
          quantity: 1,
          unit: 'bowl',
          grams: 300,
          calories: 444,
          protein: 18.6,
          carbs: 63,
          fat: 12.9,
          fiber: 4.2,
          isEstimate: true,
        },
        {
          foodId: 'gl_chicken_breast',
          foodNameEn: 'Chicken Breast (Grilled / Boiled)',
          foodNameMm: 'ကြက်ရင်ပုံသား',
          quantity: 200,
          unit: 'g',
          grams: 200,
          calories: 330,
          protein: 62,
          carbs: 0,
          fat: 7.2,
          fiber: 0,
          isEstimate: false,
        },
      ],
    },
    {
      id: 'seed_d1_dinner',
      dateIso: d1,
      mealType: 'DINNER',
      createdAtMillis: Date.now() - 80000_000,
      items: [
        {
          foodId: 'mm_steamed_rice',
          foodNameEn: 'Steamed White Rice',
          foodNameMm: 'ထမင်း',
          quantity: 2,
          unit: 'bowl',
          grams: 400,
          calories: 520,
          protein: 10.8,
          carbs: 112.8,
          fat: 1.2,
          fiber: 1.6,
          isEstimate: true,
        },
        {
          foodId: 'mm_fish_curry',
          foodNameEn: 'Myanmar Fish Curry',
          foodNameMm: 'ငါးဟင်း',
          quantity: 1.5,
          unit: 'serving',
          grams: 225,
          calories: 322.5,
          protein: 36,
          carbs: 5.5,
          fat: 17,
          fiber: 1.1,
          isEstimate: true,
        },
      ],
    },
    {
      id: 'seed_d2_lunch',
      dateIso: d2,
      mealType: 'LUNCH',
      createdAtMillis: Date.now() - 172800_000,
      items: [
        {
          foodId: 'mm_mohinga',
          foodNameEn: 'Mohinga (Fish Noodle Soup)',
          foodNameMm: 'မုန့်ဟင်းခါး',
          quantity: 1,
          unit: 'bowl',
          grams: 350,
          calories: 413,
          protein: 18.9,
          carbs: 57.75,
          fat: 11.55,
          fiber: 4.2,
          isEstimate: true,
        },
        {
          foodId: 'gl_whey_protein',
          foodNameEn: 'Whey Protein Isolate (1 Scoop)',
          foodNameMm: 'ဝေပရိုတင်းမှုန့်',
          quantity: 2,
          unit: 'scoop',
          grams: 60,
          calories: 224,
          protein: 50,
          carbs: 3,
          fat: 1,
          fiber: 0,
          isEstimate: false,
        },
      ],
    },
    {
      id: 'seed_d3_dinner',
      dateIso: d3,
      mealType: 'DINNER',
      createdAtMillis: Date.now() - 259200_000,
      items: [
        {
          foodId: 'gl_salmon',
          foodNameEn: 'Atlantic Salmon (Baked / Grilled)',
          foodNameMm: 'ဆယ်လ်မွန်ငါး',
          quantity: 200,
          unit: 'g',
          grams: 200,
          calories: 412,
          protein: 44.2,
          carbs: 0,
          fat: 24.8,
          fiber: 0,
          isEstimate: false,
        },
        {
          foodId: 'gl_sweet_potato',
          foodNameEn: 'Baked Sweet Potato',
          foodNameMm: 'ကန်စွန်းဥဖုတ်',
          quantity: 2,
          unit: 'piece',
          grams: 300,
          calories: 270,
          protein: 6,
          carbs: 62,
          fat: 0.4,
          fiber: 9,
          isEstimate: false,
        },
      ],
    },
    {
      id: 'seed_d4_lunch',
      dateIso: d4,
      mealType: 'LUNCH',
      createdAtMillis: Date.now() - 345600_000,
      items: [
        {
          foodId: 'mm_chicken_curry',
          foodNameEn: 'Myanmar Chicken Curry',
          foodNameMm: 'ကြက်သားဟင်း',
          quantity: 2,
          unit: 'serving',
          grams: 300,
          calories: 530,
          protein: 52,
          carbs: 10,
          fat: 30,
          fiber: 2.4,
          isEstimate: true,
        },
        {
          foodId: 'mm_steamed_rice',
          foodNameEn: 'Steamed White Rice',
          foodNameMm: 'ထမင်း',
          quantity: 2,
          unit: 'bowl',
          grams: 400,
          calories: 520,
          protein: 10.8,
          carbs: 112.8,
          fat: 1.2,
          fiber: 1.6,
          isEstimate: true,
        },
      ],
    },
  ];
}

function createInitialFoodUsage(): Record<string, FoodUsageRecord> {
  const now = Date.now();
  return {
    mm_steamed_rice: {
      usageCount: 5,
      lastUsedAtMillis: now - 3600_000,
      lastUsedQuantity: 1,
      lastUsedUnit: 'bowl',
    },
    mm_boiled_egg: {
      usageCount: 4,
      lastUsedAtMillis: now - 7200_000,
      lastUsedQuantity: 2,
      lastUsedUnit: 'piece',
    },
    mm_chicken_curry: {
      usageCount: 3,
      lastUsedAtMillis: now - 10800_000,
      lastUsedQuantity: 1,
      lastUsedUnit: 'serving',
    },
    gl_whey_protein: {
      usageCount: 3,
      lastUsedAtMillis: now - 14400_000,
      lastUsedQuantity: 1,
      lastUsedUnit: 'scoop',
    },
  };
}

export function loadProfile(): UserProfile {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.PROFILE);
    if (!raw) return DEFAULT_PROFILE;
    return { ...DEFAULT_PROFILE, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_PROFILE;
  }
}

export function saveProfileStorage(profile: UserProfile): void {
  localStorage.setItem(STORAGE_KEYS.PROFILE, JSON.stringify(profile));
}

export function loadMeals(): MealEntry[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.MEALS);
    if (!raw) {
      const seeded = createInitialSeedMeals();
      saveMealsStorage(seeded);
      return seeded;
    }
    return JSON.parse(raw) as MealEntry[];
  } catch {
    return createInitialSeedMeals();
  }
}

export function saveMealsStorage(meals: MealEntry[]): void {
  localStorage.setItem(STORAGE_KEYS.MEALS, JSON.stringify(meals));
}

export function loadCustomFoods(): FoodItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.CUSTOM_FOODS);
    if (!raw) return [];
    return JSON.parse(raw) as FoodItem[];
  } catch {
    return [];
  }
}

export function saveCustomFoodsStorage(foods: FoodItem[]): void {
  localStorage.setItem(STORAGE_KEYS.CUSTOM_FOODS, JSON.stringify(foods));
}

export function loadFoodOverrides(): Record<string, FoodItem> {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.FOOD_OVERRIDES);
    if (!raw) return {};
    return JSON.parse(raw) as Record<string, FoodItem>;
  } catch {
    return {};
  }
}

export function saveFoodOverridesStorage(
  overrides: Record<string, FoodItem>
): void {
  localStorage.setItem(STORAGE_KEYS.FOOD_OVERRIDES, JSON.stringify(overrides));
}

export function loadFavoriteIds(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.FAVORITE_IDS);
    if (!raw) return ['mm_boiled_egg', 'mm_steamed_rice', 'gl_chicken_breast'];
    return JSON.parse(raw) as string[];
  } catch {
    return ['mm_boiled_egg', 'mm_steamed_rice', 'gl_chicken_breast'];
  }
}

export function saveFavoriteIdsStorage(ids: string[]): void {
  localStorage.setItem(STORAGE_KEYS.FAVORITE_IDS, JSON.stringify(ids));
}

export function loadFoodUsage(): Record<string, FoodUsageRecord> {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.FOOD_USAGE);
    if (!raw) {
      const seeded = createInitialFoodUsage();
      saveFoodUsageStorage(seeded);
      return seeded;
    }
    return JSON.parse(raw) as Record<string, FoodUsageRecord>;
  } catch {
    return createInitialFoodUsage();
  }
}

export function saveFoodUsageStorage(
  usage: Record<string, FoodUsageRecord>
): void {
  localStorage.setItem(STORAGE_KEYS.FOOD_USAGE, JSON.stringify(usage));
}

export function loadThemeMode(): ThemeMode {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.THEME);
    if (raw === 'DARK' || raw === 'dark') return 'DARK';
    if (raw === 'LIGHT' || raw === 'light') return 'LIGHT';
    return 'SYSTEM';
  } catch {
    return 'SYSTEM';
  }
}

export function saveThemeMode(mode: ThemeMode): void {
  localStorage.setItem(STORAGE_KEYS.THEME, mode);
}
