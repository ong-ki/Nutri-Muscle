import { useEffect, useMemo, useState } from 'react';
import { CornerDownLeft, Minus, Plus, Search, X } from 'lucide-react';
import {
  computeCandidateGrams,
  computeCandidateNutrition,
  parseFoodInput,
  suggestFoods,
} from '../lib/foodParser';
import { mealTypeText, UI_STRINGS } from '../lib/i18n';
import { defaultMealTypeForHour } from '../lib/nutritionCalculator';
import type {
  AppLanguage,
  FoodItem,
  LoggedFoodItem,
  MealEntry,
  MealType,
  ParsedFoodCandidate,
} from '../lib/types';
import { MealIcon } from './TodayView';

interface AddFoodSheetProps {
  language: AppLanguage;
  allFoods: FoodItem[];
  editingMeal: MealEntry | null;
  initialMealType?: MealType;
  defaultDateIso: string;
  onClose: () => void;
  onSaveMeal: (entry: Omit<MealEntry, 'id'> & { id?: string }) => void;
  onCreateCustomFood: (food: FoodItem) => void;
  onToggleFavorite: (foodId: string) => void;
  onSaveFoodOverride: (food: FoodItem) => void;
}

const MEAL_TYPES: MealType[] = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];

interface BrowseCategoryDef {
  key: string;
  labelEn: string;
  labelMm: string;
}

const BROWSE_CATEGORIES: BrowseCategoryDef[] = [
  { key: 'recent', labelEn: 'Recent', labelMm: 'မကြာသေးမီ' },
  { key: 'favorites', labelEn: 'Favorites', labelMm: 'အကြိုက်ဆုံး' },
  { key: 'myanmar_meals', labelEn: 'Myanmar meals', labelMm: 'မြန်မာထမင်းဟင်း' },
  { key: 'myanmar_snacks', labelEn: 'Snacks', labelMm: 'မုန့်' },
  { key: 'street_food', labelEn: 'Street food', labelMm: 'လမ်းဘေးစာ' },
  { key: 'food_court', labelEn: 'Food court', labelMm: 'Food court' },
  { key: 'drinks', labelEn: 'Drinks', labelMm: 'အအေး/ကော်ဖီ' },
  { key: 'packaged_99', labelEn: '99 shop', labelMm: '99 ဆိုင်' },
  { key: 'fruits', labelEn: 'Fruits', labelMm: 'သစ်သီး' },
  { key: 'global_foods', labelEn: 'Gym', labelMm: 'Gym/Global' },
];

function buildInitialCandidates(
  editingMeal: MealEntry | null,
  allFoods: FoodItem[]
): ParsedFoodCandidate[] {
  if (!editingMeal) return [];
  const map = new Map(allFoods.map((f) => [f.id, f]));
  return editingMeal.items.map((item, idx) => {
    const matched = map.get(item.foodId) ?? null;
    return {
      id: `edit_${idx}_${item.foodId}`,
      rawSegment: item.foodNameEn,
      queryName: item.foodNameEn,
      quantity: item.quantity,
      unit: item.unit,
      matchedFood: matched,
      matchScore: matched ? 1 : 0,
      isMatched: matched != null,
    };
  });
}

function foodDisplayKcal(food: FoodItem): number {
  if (food.caloriesPerServing > 0) return Math.round(food.caloriesPerServing);
  return Math.round((food.caloriesPer100g * food.defaultServingGrams) / 100);
}

export function AddFoodSheet({
  language,
  allFoods,
  editingMeal,
  initialMealType,
  defaultDateIso,
  onClose,
  onSaveMeal,
  onCreateCustomFood,
  onSaveFoodOverride,
}: AddFoodSheetProps) {
  const t = UI_STRINGS[language];
  const isBurmese = language === 'MM';
  const [mealType, setMealType] = useState<MealType>(
    editingMeal?.mealType ??
      initialMealType ??
      defaultMealTypeForHour(new Date().getHours())
  );
  const [rawInput, setRawInput] = useState('');
  const [candidates, setCandidates] = useState<ParsedFoodCandidate[]>(() =>
    buildInitialCandidates(editingMeal, allFoods)
  );
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [customFoodQuery, setCustomFoodQuery] = useState<string | null>(null);
  const [customForCandidateId, setCustomForCandidateId] = useState<
    string | null
  >(null);
  const [editingFoodOverride, setEditingFoodOverride] =
    useState<FoodItem | null>(null);

  useEffect(() => {
    const map = new Map(allFoods.map((f) => [f.id, f]));
    setCandidates((prev) =>
      prev.map((c) => {
        if (!c.matchedFood) return c;
        const fresh = map.get(c.matchedFood.id);
        return fresh && fresh !== c.matchedFood
          ? { ...c, matchedFood: fresh }
          : c;
      })
    );
  }, [allFoods]);

  const recentFoods = useMemo(
    () =>
      allFoods
        .filter((f) => (f.lastUsedAtMillis ?? 0) > 0)
        .sort((a, b) => (b.lastUsedAtMillis ?? 0) - (a.lastUsedAtMillis ?? 0))
        .slice(0, 25),
    [allFoods]
  );

  const favoriteFoods = useMemo(
    () =>
      allFoods
        .filter((f) => f.isFavorite)
        .sort((a, b) => (b.usageCount ?? 0) - (a.usageCount ?? 0)),
    [allFoods]
  );

  const activeCategory =
    selectedCategory ?? (recentFoods.length > 0 ? 'recent' : 'myanmar_meals');

  const isSearching = rawInput.trim().length > 0;

  const browsableFoods = useMemo(() => {
    if (isSearching) {
      return suggestFoods(rawInput, allFoods, 30);
    }
    if (activeCategory === 'recent') return recentFoods;
    if (activeCategory === 'favorites') return favoriteFoods;
    return allFoods.filter(
      (f) => f.category.toLowerCase() === activeCategory.toLowerCase()
    );
  }, [isSearching, rawInput, allFoods, activeCategory, recentFoods, favoriteFoods]);

  const selectedByFoodId = useMemo(() => {
    const map = new Map<string, ParsedFoodCandidate>();
    for (const c of candidates) {
      if (c.matchedFood) map.set(c.matchedFood.id, c);
    }
    return map;
  }, [candidates]);

  const previewCandidates = useMemo(
    () =>
      isSearching && candidates.length === 0
        ? parseFoodInput(rawInput, allFoods)
        : [],
    [isSearching, candidates.length, rawInput, allFoods]
  );

  const runningCalories = useMemo(() => {
    const source = candidates.length > 0 ? candidates : previewCandidates;
    return source.reduce((sum, c) => {
      if (!c.isMatched || !c.matchedFood) return sum;
      return sum + computeCandidateNutrition(c).calories;
    }, 0);
  }, [candidates, previewCandidates]);

  const selectedCount = candidates.filter(
    (c) => c.isMatched && c.matchedFood
  ).length || previewCandidates.filter((c) => c.isMatched && c.matchedFood).length;
  const canSave = selectedCount > 0;

  function commitRawInput() {
    if (!rawInput.trim()) return;
    const parsed = parseFoodInput(rawInput, allFoods);
    setCandidates((prev) => [...prev, ...parsed]);
    setRawInput('');
  }

  function addFood(food: FoodItem) {
    setCandidates((prev) => {
      const existing = prev.find((c) => c.matchedFood?.id === food.id);
      if (existing) {
        const step = existing.unit === 'g' || existing.unit === 'ml' ? 25 : 0.5;
        return prev.map((c) =>
          c.id === existing.id
            ? { ...c, quantity: Number((c.quantity + step).toFixed(2)) }
            : c
        );
      }
      const qty = food.lastUsedQuantity ?? 1;
      const unit = food.lastUsedUnit ?? food.defaultUnit;
      return [
        ...prev,
        {
          id: `sug_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
          rawSegment: food.nameEn,
          queryName: food.nameEn,
          quantity: qty,
          unit,
          matchedFood: food,
          matchScore: 1,
          isMatched: true,
        },
      ];
    });
  }

  function stepFood(food: FoodItem, delta: number) {
    setCandidates((prev) => {
      const existing = prev.find((c) => c.matchedFood?.id === food.id);
      if (!existing) {
        if (delta <= 0) return prev;
        const qty = food.lastUsedQuantity ?? 1;
        const unit = food.lastUsedUnit ?? food.defaultUnit;
        return [
          ...prev,
          {
            id: `sug_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
            rawSegment: food.nameEn,
            queryName: food.nameEn,
            quantity: qty,
            unit,
            matchedFood: food,
            matchScore: 1,
            isMatched: true,
          },
        ];
      }
      const step =
        existing.unit === 'g' || existing.unit === 'ml'
          ? delta * 25
          : delta * 0.5;
      const next = Number((existing.quantity + step).toFixed(2));
      if (next <= 0) {
        return prev.filter((c) => c.id !== existing.id);
      }
      return prev.map((c) =>
        c.id === existing.id ? { ...c, quantity: next } : c
      );
    });
  }

  function removeCandidate(id: string) {
    setCandidates((prev) => prev.filter((c) => c.id !== id));
  }

  function handleSaveMeal() {
    const combined = candidates.length > 0 ? candidates : previewCandidates;
    const items: LoggedFoodItem[] = [];
    for (const c of combined) {
      if (!c.matchedFood) continue;
      const nut = computeCandidateNutrition(c);
      items.push({
        foodId: c.matchedFood.id,
        foodNameEn: c.matchedFood.nameEn,
        foodNameMm: c.matchedFood.nameMm,
        quantity: c.quantity,
        unit: c.unit,
        grams: computeCandidateGrams(c),
        calories: nut.calories,
        protein: nut.protein,
        carbs: nut.carbs,
        fat: nut.fat,
        fiber: nut.fiber,
        sugar: nut.sugar ?? 0,
        isEstimate: c.matchedFood.isEstimate,
      });
    }
    if (items.length === 0) return;

    onSaveMeal({
      id: editingMeal?.id,
      dateIso: editingMeal?.dateIso ?? defaultDateIso,
      mealType,
      createdAtMillis: editingMeal?.createdAtMillis ?? Date.now(),
      items,
    });
  }

  function handleSaveCustomFood(food: FoodItem) {
    onCreateCustomFood(food);
    if (customForCandidateId) {
      setCandidates((prev) =>
        prev.map((c) =>
          c.id === customForCandidateId
            ? {
                ...c,
                matchedFood: food,
                isMatched: true,
                matchScore: 1,
                unit: food.defaultUnit,
              }
            : c
        )
      );
    } else {
      addFood(food);
      setRawInput('');
    }
    setCustomFoodQuery(null);
    setCustomForCandidateId(null);
  }

  function handleSaveOverride(updated: FoodItem) {
    onSaveFoodOverride(updated);
    setEditingFoodOverride(null);
  }

  const totalStr = Math.round(runningCalories).toLocaleString();
  const addButtonLabel = !canSave
    ? t.addEmpty
    : editingMeal
    ? t.updateWithTotal(totalStr)
    : t.addWithTotal(totalStr);

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/35"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={t.addEmpty}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-t-[28px] bg-[var(--nm-surface)] px-5 pt-2.5 pb-5 space-y-3 h-[85dvh] max-h-[85dvh] flex flex-col animate-[sheetUp_240ms_cubic-bezier(0.22,1,0.36,1)]"
      >
        <div className="w-9 h-1 rounded-full bg-[var(--nm-surface-variant)] mx-auto shrink-0" />

        {/* Meal type pill switcher */}
        <div
          role="radiogroup"
          aria-label="Meal type"
          className="grid grid-cols-4 gap-1 p-1 rounded-full bg-[var(--nm-surface-variant)] shrink-0"
        >
          {MEAL_TYPES.map((mt) => {
            const active = mealType === mt;
            return (
              <button
                key={mt}
                type="button"
                role="radio"
                aria-checked={active}
                aria-label={mealTypeText(mt, language)}
                title={mealTypeText(mt, language)}
                onClick={() => setMealType(mt)}
                className={`h-8 rounded-full flex items-center justify-center transition-colors cursor-pointer ${
                  active
                    ? 'bg-[var(--nm-pastel-yellow)] text-[var(--nm-text)]'
                    : 'text-[var(--nm-muted)] hover:text-[var(--nm-text)]'
                }`}
              >
                <MealIcon type={mt} className="w-[18px] h-[18px]" />
              </button>
            );
          })}
        </div>

        {/* Rounded pill search field with a soft blue-teal fill */}
        <div className="relative shrink-0">
          <Search className="w-[18px] h-[18px] text-[var(--nm-text)]/65 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            data-testid="food-input"
            value={rawInput}
            onChange={(e) => setRawInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                commitRawInput();
              }
            }}
            placeholder={t.foodInputPlaceholder}
            className="w-full h-12 pl-11 pr-20 text-sm rounded-full bg-[var(--nm-pastel-teal)]/30 text-[var(--nm-text)] placeholder:text-[var(--nm-text)]/60 focus:outline-none focus:ring-2 focus:ring-[var(--nm-pastel-teal)]"
          />
          {isSearching && (
            <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1">
              <button
                type="button"
                data-testid="parse-commit-btn"
                onClick={commitRawInput}
                aria-label="Parse"
                className="w-8 h-8 rounded-full flex items-center justify-center bg-[var(--nm-surface)] text-[var(--nm-text)] cursor-pointer"
              >
                <CornerDownLeft className="w-4 h-4" />
              </button>
              <button
                type="button"
                aria-label="Clear search"
                onClick={() => setRawInput('')}
                className="w-7 h-7 rounded-full flex items-center justify-center text-[var(--nm-text)]/65 hover:text-[var(--nm-text)] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>

        {/* Multi-select picked chips summary */}
        {candidates.length > 0 && (
          <div
            data-testid="parsed-candidates"
            className="flex gap-2 overflow-x-auto pb-0.5 no-scrollbar shrink-0"
          >
            {candidates.map((cand) => {
              const food = cand.matchedFood;
              const name = food
                ? isBurmese && food.nameMm
                  ? food.nameMm
                  : food.nameEn
                : cand.queryName;
              const qty =
                cand.quantity % 1 === 0
                  ? String(cand.quantity)
                  : cand.quantity.toFixed(1);
              return (
                <span
                  key={cand.id}
                  data-testid={
                    food ? `candidate-${food.id}` : 'candidate-unmatched'
                  }
                  className="shrink-0 inline-flex items-center gap-1.5 pl-3 pr-1.5 py-1 rounded-full bg-[var(--nm-pastel-sage)] text-xs font-medium text-[var(--nm-text)]"
                >
                  <button
                    type="button"
                    onClick={() => {
                      if (food) {
                        setEditingFoodOverride(food);
                      } else {
                        setCustomForCandidateId(cand.id);
                        setCustomFoodQuery(cand.queryName);
                      }
                    }}
                    className="max-w-[140px] truncate cursor-pointer"
                  >
                    {name} ×{qty}
                  </button>
                  <button
                    type="button"
                    aria-label="Remove"
                    onClick={() => removeCandidate(cand.id)}
                    className="w-5 h-5 rounded-full flex items-center justify-center hover:bg-black/10 cursor-pointer"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </span>
              );
            })}
          </div>
        )}

        {/* Category pill chips (selected = soft yellow, others = light gray) */}
        {!isSearching && (
          <div
            data-testid="category-chips"
            className="flex gap-2 overflow-x-auto pb-0.5 no-scrollbar shrink-0"
          >
            {BROWSE_CATEGORIES.map((cat) => {
              const active = activeCategory === cat.key;
              return (
                <button
                  key={cat.key}
                  type="button"
                  data-testid={`cat-chip-${cat.key}`}
                  onClick={() => setSelectedCategory(cat.key)}
                  className={`shrink-0 px-3.5 py-1.5 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                    active
                      ? 'bg-[var(--nm-pastel-yellow)] text-[var(--nm-text)]'
                      : 'bg-[var(--nm-surface-variant)] text-[var(--nm-muted)] hover:text-[var(--nm-text)]'
                  }`}
                >
                  {isBurmese ? cat.labelMm : cat.labelEn}
                </button>
              );
            })}
          </div>
        )}

        {/* Food rows: no dividers, 12px spacing, soft rounded row bg, kcal pastel chip, green + button */}
        <div
          data-testid="browsable-food-list"
          className="flex-1 min-h-0 overflow-y-auto space-y-3 pr-0.5"
        >
          {browsableFoods.length === 0 ? (
            <button
              type="button"
              data-testid="no-result-add-custom"
              onClick={() => {
                setCustomForCandidateId(null);
                setCustomFoodQuery(rawInput.trim());
              }}
              className="w-full px-4 py-4 rounded-[22px] bg-[var(--nm-bg)] flex items-center justify-between text-left cursor-pointer"
            >
              <span className="text-sm text-[var(--nm-muted)]">
                {isSearching
                  ? isBurmese
                    ? 'မတွေ့ပါ · အစားအစာအသစ်ထည့်'
                    : 'No result · Add custom food'
                  : isBurmese
                  ? 'စာရင်းမရှိသေးပါ'
                  : 'No items yet'}
              </span>
              {isSearching && (
                <span className="w-9 h-9 rounded-full bg-[var(--nm-pastel-sage)] text-[var(--nm-text)] flex items-center justify-center shrink-0">
                  <Plus className="w-4 h-4" />
                </span>
              )}
            </button>
          ) : (
            browsableFoods.map((food) => {
              const picked = selectedByFoodId.get(food.id);
              const primaryName = food.nameMm || food.nameEn;
              const secondaryName =
                food.nameMm &&
                food.nameEn.toLowerCase() !== food.nameMm.toLowerCase()
                  ? food.nameEn
                  : null;
              const serving =
                isBurmese && food.servingLabelMm
                  ? food.servingLabelMm
                  : food.servingLabelEn;
              const kcal = picked
                ? Math.round(computeCandidateNutrition(picked).calories)
                : foodDisplayKcal(food);
              const est = food.isEstimate ? '~' : '';
              const qtyText = picked
                ? picked.quantity % 1 === 0
                  ? String(picked.quantity)
                  : picked.quantity.toFixed(1)
                : '';

              return (
                <div
                  key={food.id}
                  data-testid={`food-row-${food.id}`}
                  className={`rounded-[22px] px-3.5 py-3 flex items-center justify-between gap-2 transition-colors animate-[fadeInItem_180ms_ease-out] ${
                    picked
                      ? 'bg-[var(--nm-pastel-teal)]/25'
                      : 'bg-[var(--nm-bg)]'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => setEditingFoodOverride(food)}
                    className="flex-1 min-w-0 text-left cursor-pointer"
                  >
                    <div className="text-sm font-medium text-[var(--nm-text)] truncate">
                      {primaryName}
                    </div>
                    {secondaryName && (
                      <div className="text-[11px] text-[var(--nm-muted)] truncate">
                        {secondaryName}
                      </div>
                    )}
                    <div className="text-[11px] text-[var(--nm-muted)]">
                      {serving}
                    </div>
                  </button>

                  {/* Kcal pastel chip on the right */}
                  <span className="shrink-0 px-2.5 py-1 rounded-full bg-[var(--nm-pastel-teal)]/30 text-[11px] font-medium tabular-nums text-[var(--nm-text)]">
                    {est}
                    {kcal} kcal
                  </span>

                  {picked ? (
                    <div className="flex items-center gap-1 shrink-0">
                      <button
                        type="button"
                        aria-label="Decrease quantity"
                        onClick={() => stepFood(food, -1)}
                        className="w-8 h-8 rounded-full bg-[var(--nm-surface)] text-[var(--nm-text)] flex items-center justify-center cursor-pointer"
                      >
                        <Minus className="w-3.5 h-3.5" />
                      </button>
                      <span className="text-xs font-medium tabular-nums min-w-[20px] text-center text-[var(--nm-text)]">
                        {qtyText}
                      </span>
                      <button
                        type="button"
                        aria-label="Increase quantity"
                        onClick={() => stepFood(food, 1)}
                        className="w-8 h-8 rounded-full bg-[var(--nm-pastel-sage)] text-[var(--nm-text)] flex items-center justify-center cursor-pointer"
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      data-testid={`add-food-row-${food.id}`}
                      aria-label={`Add ${food.nameEn}`}
                      onClick={() => addFood(food)}
                      className="w-9 h-9 rounded-full bg-[var(--nm-pastel-sage)] text-[var(--nm-text)] flex items-center justify-center shrink-0 cursor-pointer"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Sticky bottom bar: wide soft-yellow pill button with count badge */}
        <button
          type="button"
          data-testid="save-meal-btn"
          disabled={!canSave}
          onClick={handleSaveMeal}
          className="w-full h-[54px] rounded-full bg-[var(--nm-pastel-yellow)] disabled:bg-[var(--nm-surface-variant)] text-[var(--nm-text)] disabled:text-[var(--nm-muted)] text-sm font-medium tabular-nums transition-colors shrink-0 cursor-pointer disabled:cursor-not-allowed flex items-center justify-center gap-2.5"
        >
          <span>{addButtonLabel}</span>
          {selectedCount > 0 && (
            <span className="px-2.5 py-0.5 rounded-full bg-[var(--nm-surface)] text-[11px] font-medium text-[var(--nm-text)]">
              {selectedCount}
            </span>
          )}
        </button>
      </div>

      {customFoodQuery !== null && (
        <CompactFoodEditorModal
          language={language}
          title={t.customFood}
          initialName={customFoodQuery}
          allowRename
          initialServingGrams={100}
          initialCalories={200}
          initialProtein={15}
          initialCarbs={20}
          initialFat={6}
          initialSugar={0}
          onClose={() => {
            setCustomFoodQuery(null);
            setCustomForCandidateId(null);
          }}
          onSave={(vals) => {
            const grams = Math.max(1, vals.servingGrams);
            const scale = 100 / grams;
            const cal100 = vals.calories * scale;
            const prot100 = vals.protein * scale;
            const clean = vals.name.trim() || 'Custom';
            handleSaveCustomFood({
              id: `custom_${Date.now()}`,
              nameEn: clean,
              nameMm: clean,
              aliases: [clean],
              category: 'custom',
              servingLabelMm: '၁ ပွဲ',
              servingLabelEn: '1 serving',
              defaultServingGrams: grams,
              caloriesPerServing: vals.calories,
              proteinPerServing: vals.protein,
              carbsPerServing: vals.carbs,
              fatPerServing: vals.fat,
              fiberPerServing: 0,
              sugarPerServing: vals.sugar,
              caloriesPer100g: cal100,
              proteinPer100g: prot100,
              carbsPer100g: vals.carbs * scale,
              fatPer100g: vals.fat * scale,
              fiberPer100g: 0,
              sugarPer100g: vals.sugar * scale,
              defaultUnit: 'serving',
              unitGrams: { g: 1, serving: grams },
              isMuscleFriendly:
                cal100 > 0 && (prot100 / cal100) * 100 >= 7.5 && prot100 >= 6,
              isEstimate: false,
              isCustom: true,
            });
          }}
        />
      )}

      {editingFoodOverride && (
        <CompactFoodEditorModal
          language={language}
          title={t.editFood}
          initialName={
            isBurmese && editingFoodOverride.nameMm
              ? editingFoodOverride.nameMm
              : editingFoodOverride.nameEn
          }
          allowRename={false}
          initialServingGrams={editingFoodOverride.defaultServingGrams}
          initialCalories={editingFoodOverride.caloriesPerServing}
          initialProtein={editingFoodOverride.proteinPerServing}
          initialCarbs={editingFoodOverride.carbsPerServing}
          initialFat={editingFoodOverride.fatPerServing}
          initialSugar={editingFoodOverride.sugarPerServing}
          onClose={() => setEditingFoodOverride(null)}
          onSave={(vals) => {
            const grams = Math.max(1, vals.servingGrams);
            const scale = 100 / grams;
            handleSaveOverride({
              ...editingFoodOverride,
              defaultServingGrams: grams,
              caloriesPerServing: vals.calories,
              proteinPerServing: vals.protein,
              carbsPerServing: vals.carbs,
              fatPerServing: vals.fat,
              sugarPerServing: vals.sugar,
              caloriesPer100g: vals.calories * scale,
              proteinPer100g: vals.protein * scale,
              carbsPer100g: vals.carbs * scale,
              fatPer100g: vals.fat * scale,
              sugarPer100g: vals.sugar * scale,
              unitGrams: {
                ...editingFoodOverride.unitGrams,
                [editingFoodOverride.defaultUnit]: grams,
                serving: grams,
              },
              isEstimate: false,
              isOverride: true,
            });
          }}
        />
      )}
    </div>
  );
}

interface FoodEditorValues {
  name: string;
  servingGrams: number;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  sugar: number;
}

function CompactFoodEditorModal({
  language,
  title,
  initialName,
  allowRename,
  initialServingGrams,
  initialCalories,
  initialProtein,
  initialCarbs,
  initialFat,
  initialSugar,
  onClose,
  onSave,
}: {
  language: AppLanguage;
  title: string;
  initialName: string;
  allowRename: boolean;
  initialServingGrams: number;
  initialCalories: number;
  initialProtein: number;
  initialCarbs: number;
  initialFat: number;
  initialSugar: number;
  onClose: () => void;
  onSave: (vals: FoodEditorValues) => void;
}) {
  const t = UI_STRINGS[language];
  const [name, setName] = useState(initialName);
  const [servingG, setServingG] = useState(
    String(Math.round(initialServingGrams))
  );
  const [kcal, setKcal] = useState(String(Math.round(initialCalories)));
  const [p, setP] = useState(String(Number(initialProtein.toFixed(1))));
  const [c, setC] = useState(String(Number(initialCarbs.toFixed(1))));
  const [f, setF] = useState(String(Number(initialFat.toFixed(1))));
  const [sugar, setSugar] = useState(String(Number(initialSugar.toFixed(1))));

  function submit() {
    onSave({
      name: name.trim() || initialName || 'Custom',
      servingGrams: Math.max(1, parseFloat(servingG) || 100),
      calories: Math.max(0, parseFloat(kcal) || 0),
      protein: Math.max(0, parseFloat(p) || 0),
      carbs: Math.max(0, parseFloat(c) || 0),
      fat: Math.max(0, parseFloat(f) || 0),
      sugar: Math.max(0, parseFloat(sugar) || 0),
    });
  }

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/45 p-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-xs rounded-[28px] bg-[var(--nm-surface)] p-5 space-y-3 animate-[fadeInItem_180ms_ease-out]"
      >
        <h3 className="text-base font-medium text-[var(--nm-text)]">{title}</h3>

        <input
          type="text"
          value={name}
          disabled={!allowRename}
          onChange={(e) => setName(e.target.value)}
          placeholder={t.nameLabel}
          className="w-full px-3.5 py-2 text-sm rounded-full bg-[var(--nm-bg)] text-[var(--nm-text)] disabled:opacity-65 focus:outline-none"
        />
        <div className="grid grid-cols-2 gap-2">
          <label className="space-y-1">
            <span className="text-[11px] text-[var(--nm-muted)]">
              {t.servingG}
            </span>
            <input
              type="number"
              data-testid="editor-serving"
              value={servingG}
              onChange={(e) => setServingG(e.target.value)}
              className="w-full px-3 py-1.5 text-sm rounded-full bg-[var(--nm-bg)] text-[var(--nm-text)] focus:outline-none"
            />
          </label>
          <label className="space-y-1">
            <span className="text-[11px] text-[var(--nm-muted)]">{t.kcal}</span>
            <input
              type="number"
              data-testid="editor-kcal"
              value={kcal}
              onChange={(e) => setKcal(e.target.value)}
              className="w-full px-3 py-1.5 text-sm rounded-full bg-[var(--nm-bg)] text-[var(--nm-text)] focus:outline-none"
            />
          </label>
        </div>
        <div className="grid grid-cols-3 gap-2">
          <label className="space-y-1">
            <span className="text-[11px] text-[var(--nm-muted)]">
              {t.pShort}
            </span>
            <input
              type="number"
              value={p}
              onChange={(e) => setP(e.target.value)}
              className="w-full px-3 py-1.5 text-sm rounded-full bg-[var(--nm-bg)] text-[var(--nm-text)] focus:outline-none"
            />
          </label>
          <label className="space-y-1">
            <span className="text-[11px] text-[var(--nm-muted)]">
              {t.cShort}
            </span>
            <input
              type="number"
              value={c}
              onChange={(e) => setC(e.target.value)}
              className="w-full px-3 py-1.5 text-sm rounded-full bg-[var(--nm-bg)] text-[var(--nm-text)] focus:outline-none"
            />
          </label>
          <label className="space-y-1">
            <span className="text-[11px] text-[var(--nm-muted)]">
              {t.fShort}
            </span>
            <input
              type="number"
              value={f}
              onChange={(e) => setF(e.target.value)}
              className="w-full px-3 py-1.5 text-sm rounded-full bg-[var(--nm-bg)] text-[var(--nm-text)] focus:outline-none"
            />
          </label>
        </div>
        <label className="block space-y-1">
          <span className="text-[11px] text-[var(--nm-muted)]">
            {t.sugarShort}
          </span>
          <input
            type="number"
            value={sugar}
            onChange={(e) => setSugar(e.target.value)}
            className="w-full px-3 py-1.5 text-sm rounded-full bg-[var(--nm-bg)] text-[var(--nm-text)] focus:outline-none"
          />
        </label>

        <div className="flex justify-end gap-2 pt-1">
          <button
            type="button"
            onClick={onClose}
            className="px-3 py-1.5 text-xs text-[var(--nm-muted)] cursor-pointer"
          >
            {t.cancel}
          </button>
          <button
            type="button"
            data-testid="editor-save-btn"
            onClick={submit}
            className="px-4 py-1.5 text-xs font-medium rounded-full bg-[var(--nm-pastel-yellow)] text-[var(--nm-text)] cursor-pointer"
          >
            {t.save}
          </button>
        </div>
      </div>
    </div>
  );
}
