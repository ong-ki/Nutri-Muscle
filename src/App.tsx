/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useMemo, useState } from 'react';
import { CalendarDays, Home, User } from 'lucide-react';
import { AddFoodSheet } from './components/AddFoodView';
import { AndroidProjectSheet } from './components/AndroidProjectView';
import { HistoryView } from './components/HistoryView';
import { FloatingNavBar } from './components/PastelComponents';
import { ProfileView } from './components/ProfileView';
import { TodayView } from './components/TodayView';
import { UI_STRINGS } from './lib/i18n';
import { generateDailyInsights, sumMealTotals } from './lib/muscleInsights';
import { calculateTargets } from './lib/nutritionCalculator';
import {
  BUILTIN_FOODS,
  loadCustomFoods,
  loadFavoriteIds,
  loadFoodOverrides,
  loadFoodUsage,
  loadMeals,
  loadProfile,
  loadThemeMode,
  offsetIsoDate,
  saveCustomFoodsStorage,
  saveFavoriteIdsStorage,
  saveFoodOverridesStorage,
  saveFoodUsageStorage,
  saveMealsStorage,
  saveProfileStorage,
  saveThemeMode,
  todayIsoDate,
} from './lib/storage';
import type {
  FoodItem,
  FoodUsageRecord,
  MealEntry,
  MealType,
  NutriMuscleBackup,
  ThemeMode,
  UserProfile,
} from './lib/types';

type TabKey = 'today' | 'history' | 'profile';

export default function App() {
  const [activeTab, setActiveTab] = useState<TabKey>('today');
  const [profile, setProfile] = useState<UserProfile>(loadProfile);
  const [meals, setMeals] = useState<MealEntry[]>(loadMeals);
  const [customFoods, setCustomFoods] = useState<FoodItem[]>(loadCustomFoods);
  const [foodOverrides, setFoodOverrides] =
    useState<Record<string, FoodItem>>(loadFoodOverrides);
  const [favoriteIds, setFavoriteIds] = useState<string[]>(loadFavoriteIds);
  const [foodUsage, setFoodUsage] =
    useState<Record<string, FoodUsageRecord>>(loadFoodUsage);
  const [themeMode, setThemeMode] = useState<ThemeMode>(loadThemeMode);
  const [systemPrefersDark, setSystemPrefersDark] = useState<boolean>(() =>
    typeof window !== 'undefined'
      ? window.matchMedia('(prefers-color-scheme: dark)').matches
      : false
  );

  // Bottom sheet state
  const [addSheetOpen, setAddSheetOpen] = useState(false);
  const [editingMeal, setEditingMeal] = useState<MealEntry | null>(null);
  const [initialMealType, setInitialMealType] = useState<MealType | undefined>(
    undefined
  );
  const [addDateIso, setAddDateIso] = useState<string>(todayIsoDate);
  const [androidSheetOpen, setAndroidSheetOpen] = useState(false);

  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e: MediaQueryListEvent) => setSystemPrefersDark(e.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);

  const isDark =
    themeMode === 'DARK' || (themeMode === 'SYSTEM' && systemPrefersDark);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', isDark);
    saveThemeMode(themeMode);
  }, [isDark, themeMode]);

  const lang = profile.language;
  const t = UI_STRINGS[lang];

  const allFoods = useMemo(() => {
    const favSet = new Set(favoriteIds);
    return [...customFoods, ...BUILTIN_FOODS].map((base) => {
      const merged = foodOverrides[base.id] ?? base;
      const usage = foodUsage[base.id];
      return {
        ...merged,
        isFavorite: favSet.has(base.id),
        usageCount: usage?.usageCount ?? merged.usageCount ?? 0,
        lastUsedAtMillis: usage?.lastUsedAtMillis ?? merged.lastUsedAtMillis ?? 0,
        lastUsedQuantity: usage?.lastUsedQuantity ?? merged.lastUsedQuantity,
        lastUsedUnit: usage?.lastUsedUnit ?? merged.lastUsedUnit,
      };
    });
  }, [customFoods, foodOverrides, favoriteIds, foodUsage]);

  const targets = useMemo(() => calculateTargets(profile), [profile]);

  const todayIso = todayIsoDate();
  const todayMeals = useMemo(
    () => meals.filter((m) => m.dateIso === todayIso),
    [meals, todayIso]
  );
  const last7DaysCalories = useMemo(() => {
    const byDate = new Map<string, number>();
    for (const m of meals) {
      const kcal = m.items.reduce((s, i) => s + i.calories, 0);
      byDate.set(m.dateIso, (byDate.get(m.dateIso) ?? 0) + kcal);
    }
    const series: number[] = [];
    for (let offset = 6; offset >= 0; offset--) {
      series.push(byDate.get(offsetIsoDate(offset)) ?? 0);
    }
    return series;
  }, [meals]);
  const todayTotals = useMemo(() => sumMealTotals(todayMeals), [todayMeals]);
  const todayInsights = useMemo(
    () => generateDailyInsights(todayTotals, targets, todayMeals, allFoods),
    [todayTotals, targets, todayMeals, allFoods]
  );

  function handleUpdateProfile(next: UserProfile) {
    setProfile(next);
    saveProfileStorage(next);
  }

  function openNewFoodSheet(mealType?: MealType, dateIso?: string) {
    setEditingMeal(null);
    setInitialMealType(mealType);
    setAddDateIso(dateIso ?? todayIsoDate());
    setAddSheetOpen(true);
  }

  function openEditFoodSheet(meal: MealEntry) {
    setEditingMeal(meal);
    setInitialMealType(meal.mealType);
    setAddSheetOpen(true);
  }

  function handleSaveMeal(entry: Omit<MealEntry, 'id'> & { id?: string }) {
    const nextId = entry.id ?? `meal_${Date.now()}`;
    const fullEntry: MealEntry = { ...entry, id: nextId };
    setMeals((prev) => {
      const exists = prev.some((m) => m.id === nextId);
      const updated = exists
        ? prev.map((m) => (m.id === nextId ? fullEntry : m))
        : [...prev, fullEntry];
      saveMealsStorage(updated);
      return updated;
    });

    // Record usage & remember last-used serving per food
    const now = Date.now();
    setFoodUsage((prev) => {
      const next = { ...prev };
      for (const item of fullEntry.items) {
        const cur = next[item.foodId];
        next[item.foodId] = {
          usageCount: (cur?.usageCount ?? 0) + 1,
          lastUsedAtMillis: now,
          lastUsedQuantity: item.quantity,
          lastUsedUnit: item.unit,
        };
      }
      saveFoodUsageStorage(next);
      return next;
    });

    setAddSheetOpen(false);
    setEditingMeal(null);
  }

  function handleDeleteMeal(mealId: string) {
    setMeals((prev) => {
      const updated = prev.filter((m) => m.id !== mealId);
      saveMealsStorage(updated);
      return updated;
    });
  }

  function handleCreateCustomFood(food: FoodItem) {
    setCustomFoods((prev) => {
      const updated = [food, ...prev];
      saveCustomFoodsStorage(updated);
      return updated;
    });
  }

  function handleToggleFavorite(foodId: string) {
    setFavoriteIds((prev) => {
      const exists = prev.includes(foodId);
      const updated = exists
        ? prev.filter((id) => id !== foodId)
        : [foodId, ...prev];
      saveFavoriteIdsStorage(updated);
      return updated;
    });
  }

  function handleSaveFoodOverride(updatedFood: FoodItem) {
    setFoodOverrides((prev) => {
      const next = { ...prev, [updatedFood.id]: updatedFood };
      saveFoodOverridesStorage(next);
      return next;
    });
  }

  function handleExportBackup(): NutriMuscleBackup {
    return {
      version: 2,
      exportedAtMillis: Date.now(),
      profile,
      customFoods,
      foodOverrides,
      favoriteIds,
      meals,
    };
  }

  function handleImportBackup(backup: NutriMuscleBackup) {
    setProfile(backup.profile);
    saveProfileStorage(backup.profile);
    const cf = Array.isArray(backup.customFoods) ? backup.customFoods : [];
    setCustomFoods(cf);
    saveCustomFoodsStorage(cf);
    if (backup.foodOverrides && typeof backup.foodOverrides === 'object') {
      setFoodOverrides(backup.foodOverrides);
      saveFoodOverridesStorage(backup.foodOverrides);
    }
    if (Array.isArray(backup.favoriteIds)) {
      setFavoriteIds(backup.favoriteIds);
      saveFavoriteIdsStorage(backup.favoriteIds);
    }
    setMeals(backup.meals);
    saveMealsStorage(backup.meals);
  }

  return (
    <div className="min-h-dvh bg-[var(--nm-bg)] text-[var(--nm-text)]">
      <main className="max-w-md mx-auto min-h-dvh">
        {activeTab === 'today' && (
          <TodayView
            language={lang}
            weightKg={profile.weightKg}
            targets={targets}
            totals={todayTotals}
            meals={todayMeals}
            last7DaysCalories={last7DaysCalories}
            insights={todayInsights}
            onOpenAddFood={openNewFoodSheet}
            onEditMeal={openEditFoodSheet}
            onDeleteMeal={handleDeleteMeal}
          />
        )}

        {activeTab === 'history' && (
          <HistoryView
            language={lang}
            meals={meals}
            targets={targets}
            isDark={isDark}
            onEditMeal={openEditFoodSheet}
            onDeleteMeal={handleDeleteMeal}
            onOpenAddFood={(dateIso) => openNewFoodSheet(undefined, dateIso)}
          />
        )}

        {activeTab === 'profile' && (
          <ProfileView
            profile={profile}
            themeMode={themeMode}
            onUpdateProfile={handleUpdateProfile}
            onChangeThemeMode={setThemeMode}
            onExportBackup={handleExportBackup}
            onImportBackup={handleImportBackup}
            onOpenAndroidProject={() => setAndroidSheetOpen(true)}
          />
        )}
      </main>

      <FloatingNavBar
        selected={activeTab}
        onSelect={(key) => setActiveTab(key as TabKey)}
        items={[
          {
            key: 'today',
            label: t.navToday,
            testId: 'nav-today',
            icon: <Home className="w-5 h-5" />,
          },
          {
            key: 'history',
            label: t.navHistory,
            testId: 'nav-history',
            icon: <CalendarDays className="w-5 h-5" />,
          },
          {
            key: 'profile',
            label: t.navProfile,
            testId: 'nav-profile',
            icon: <User className="w-5 h-5" />,
          },
        ]}
      />

      {addSheetOpen && (
        <AddFoodSheet
          key={editingMeal?.id ?? `new_${initialMealType ?? 'auto'}_${addDateIso}`}
          language={lang}
          allFoods={allFoods}
          editingMeal={editingMeal}
          initialMealType={initialMealType}
          defaultDateIso={addDateIso}
          onClose={() => {
            setAddSheetOpen(false);
            setEditingMeal(null);
          }}
          onSaveMeal={handleSaveMeal}
          onCreateCustomFood={handleCreateCustomFood}
          onToggleFavorite={handleToggleFavorite}
          onSaveFoodOverride={handleSaveFoodOverride}
        />
      )}

      {androidSheetOpen && (
        <AndroidProjectSheet
          language={lang}
          onClose={() => setAndroidSheetOpen(false)}
        />
      )}
    </div>
  );
}
