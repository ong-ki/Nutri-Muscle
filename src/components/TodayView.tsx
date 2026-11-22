import { useMemo, useState } from 'react';
import {
  Bell,
  ChevronDown,
  Coffee,
  Cookie,
  Moon,
  Plus,
  Search,
  Sun,
  User,
  X,
} from 'lucide-react';
import { formatInsightTip, mealTypeText, UI_STRINGS } from '../lib/i18n';
import type {
  AppLanguage,
  DailyInsights,
  MealEntry,
  MealType,
  NutritionTargets,
  NutritionTotals,
} from '../lib/types';
import { CalorieRing, PastelCard } from './PastelComponents';

interface TodayViewProps {
  language: AppLanguage;
  weightKg: number;
  targets: NutritionTargets;
  totals: NutritionTotals;
  meals: MealEntry[];
  last7DaysCalories: number[];
  insights: DailyInsights;
  onOpenAddFood: (mealType?: MealType) => void;
  onEditMeal: (meal: MealEntry) => void;
  onDeleteMeal: (mealId: string) => void;
}

export function MealIcon({
  type,
  className = 'w-5 h-5',
}: {
  type: MealType;
  className?: string;
}) {
  switch (type) {
    case 'BREAKFAST':
      return <Coffee className={className} />;
    case 'LUNCH':
      return <Sun className={className} />;
    case 'DINNER':
      return <Moon className={className} />;
    case 'SNACK':
      return <Cookie className={className} />;
  }
}

function Sparkline7Days({ values }: { values: number[] }) {
  const w = 120;
  const h = 24;
  const safeVals = values.length >= 2 ? values : [0, 0];
  const maxVal = Math.max(...safeVals, 1);
  const stepX = w / (safeVals.length - 1);
  const points = safeVals
    .map((v, idx) => {
      const x = idx * stepX;
      const y = h - 3 - (v / maxVal) * (h - 6);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(' ');

  return (
    <svg
      viewBox={`0 0 ${w} ${h}`}
      className="w-full h-6 overflow-visible"
      aria-label="Last 7 days calories"
    >
      <line
        x1={0}
        y1={h - 1}
        x2={w}
        y2={h - 1}
        stroke="var(--nm-surface-variant)"
        strokeWidth={1.5}
      />
      <polyline
        fill="none"
        stroke="var(--nm-pastel-peach)"
        strokeWidth={2.2}
        strokeLinecap="round"
        strokeLinejoin="round"
        points={points}
      />
    </svg>
  );
}

export function TodayView({
  language,
  weightKg,
  targets,
  totals,
  meals,
  last7DaysCalories,
  insights,
  onOpenAddFood,
  onEditMeal,
}: TodayViewProps) {
  const t = UI_STRINGS[language];
  const [showMuscleSheet, setShowMuscleSheet] = useState(false);
  const [showMealMenu, setShowMealMenu] = useState(false);
  const [selectedIdx, setSelectedIdx] = useState(0);

  const sortedMeals = useMemo(
    () => [...meals].sort((a, b) => b.createdAtMillis - a.createdAtMillis),
    [meals]
  );

  const activeMeal =
    sortedMeals[Math.min(selectedIdx, Math.max(0, sortedMeals.length - 1))] ??
    null;

  const activeMealTotals = useMemo(() => {
    if (!activeMeal) return { calories: 0, protein: 0, carbs: 0, fat: 0 };
    return activeMeal.items.reduce(
      (acc, i) => ({
        calories: acc.calories + i.calories,
        protein: acc.protein + i.protein,
        carbs: acc.carbs + i.carbs,
        fat: acc.fat + i.fat,
      }),
      { calories: 0, protein: 0, carbs: 0, fat: 0 }
    );
  }, [activeMeal]);

  const progressPct =
    targets.calorieTarget > 0
      ? Math.round((totals.calories / targets.calorieTarget) * 100)
      : 0;

  const rdcPct =
    targets.calorieTarget > 0
      ? Math.round((activeMealTotals.calories / targets.calorieTarget) * 100)
      : 0;

  const dateLabel = useMemo(() => {
    const d = new Date();
    return d.toLocaleDateString('en-US', { day: 'numeric', month: 'short' });
  }, []);

  const weightStr =
    weightKg % 1 === 0 ? String(weightKg) : weightKg.toFixed(1);

  return (
    <div className="min-h-[calc(100dvh-1rem)] px-5 pt-4 pb-24 flex flex-col gap-3.5">
      {/* 1. Header: avatar + tiny greeting, search & bell round buttons */}
      <header className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-full bg-[var(--nm-pastel-yellow)] flex items-center justify-center text-[#1F2A2E] dark:text-[#EFF3F4]">
            <User className="w-4 h-4" />
          </div>
          <span className="text-xs font-medium text-[var(--nm-text)]">
            {language === 'MM' ? 'မင်္ဂလာပါ!' : 'Hello, Athlete!'}
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            data-testid="header-search-btn"
            aria-label="Search food"
            onClick={() => onOpenAddFood()}
            className="w-9 h-9 rounded-full bg-[var(--nm-surface)] flex items-center justify-center text-[var(--nm-text)] cursor-pointer"
          >
            <Search className="w-4 h-4" />
          </button>
          <button
            type="button"
            data-testid="muscle-score-chip"
            aria-label={t.muscleScore}
            onClick={() => setShowMuscleSheet(true)}
            className="w-9 h-9 rounded-full bg-[var(--nm-surface)] flex items-center justify-center text-[var(--nm-text)] cursor-pointer"
          >
            <Bell className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* 2. Blue-teal PastelCard "Progress" */}
      <PastelCard color="var(--nm-pastel-teal)">
        <div className="flex items-center justify-between gap-3">
          <div className="flex flex-col gap-1.5 text-[#1F2A2E] dark:text-[#EFF3F4]">
            <span className="text-xs font-medium opacity-75">
              {language === 'MM' ? 'တိုးတက်မှု' : 'Progress'}
            </span>
            <span className="text-[40px] leading-none font-medium tabular-nums tracking-tight">
              {progressPct}%
            </span>
            <span className="text-[11px] opacity-70">{dateLabel}</span>
          </div>

          <CalorieRing
            value={totals.calories}
            target={targets.calorieTarget}
            size={134}
            strokeWidth={9}
          />
        </div>
      </PastelCard>

      {/* 3. Two small white cards side by side */}
      <div className="grid grid-cols-2 gap-3">
        <PastelCard>
          <div className="text-[11px] text-[var(--nm-muted)]">
            {language === 'MM' ? 'အလေးချိန်' : 'Current Weight'}
          </div>
          <div className="mt-2.5 flex items-baseline gap-1 tabular-nums">
            <span className="text-[32px] leading-none font-medium text-[var(--nm-text)]">
              {weightStr}
            </span>
            <span className="text-xs text-[var(--nm-muted)]">kg</span>
          </div>
          <div className="mt-2 text-[11px] tabular-nums text-[var(--nm-muted)]">
            0.0 kg
          </div>
        </PastelCard>

        <PastelCard>
          <div className="text-[11px] text-[var(--nm-muted)]">
            {language === 'MM' ? 'ယနေ့ ကယ်လိုရီ' : "Today's Calories"}
          </div>
          <div className="mt-2.5 text-[32px] leading-none font-medium tabular-nums text-[var(--nm-text)]">
            {Math.round(totals.calories).toLocaleString()}
          </div>
          <div className="mt-2">
            <Sparkline7Days values={last7DaysCalories} />
          </div>
        </PastelCard>
      </div>

      {/* 4. Sage-green PastelCard for latest meal */}
      <PastelCard
        color="var(--nm-pastel-sage)"
        onClick={() => (activeMeal ? onEditMeal(activeMeal) : onOpenAddFood())}
      >
        <div className="text-[#1F2A2E] dark:text-[#EFF3F4]">
          <div className="flex items-center justify-between gap-2">
            <div>
              <div className="text-sm font-medium">
                {activeMeal
                  ? mealTypeText(activeMeal.mealType, language)
                  : mealTypeText('BREAKFAST', language)}
              </div>
              <div className="text-xl font-medium tabular-nums mt-0.5">
                {Math.round(activeMealTotals.calories).toLocaleString()} kcal
              </div>
            </div>

            <button
              type="button"
              data-testid="add-food-fab"
              aria-label={t.addEmpty}
              onClick={(e) => {
                e.stopPropagation();
                onOpenAddFood();
              }}
              className="w-10 h-10 rounded-full bg-[var(--nm-surface)] text-[var(--nm-text)] flex items-center justify-center cursor-pointer shrink-0"
            >
              <Plus className="w-5 h-5" />
            </button>
          </div>

          <div className="mt-4 grid grid-cols-4 gap-2 tabular-nums">
            <div>
              <div className="text-sm font-medium">
                {Math.round(activeMealTotals.protein)}g
              </div>
              <div className="text-[11px] opacity-70">
                {language === 'MM' ? 'ပရိုတင်း' : 'Proteins'}
              </div>
            </div>
            <div>
              <div className="text-sm font-medium">
                {Math.round(activeMealTotals.fat)}g
              </div>
              <div className="text-[11px] opacity-70">
                {language === 'MM' ? 'အဆီ' : 'Fats'}
              </div>
            </div>
            <div>
              <div className="text-sm font-medium">
                {Math.round(activeMealTotals.carbs)}g
              </div>
              <div className="text-[11px] opacity-70">
                {language === 'MM' ? 'ကာဗို' : 'Carbs'}
              </div>
            </div>
            <div>
              <div className="text-sm font-medium">{rdcPct}%</div>
              <div className="text-[11px] opacity-70">RDC</div>
            </div>
          </div>

          <div className="mt-3.5 relative inline-block">
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                if (sortedMeals.length > 1) setShowMealMenu((v) => !v);
              }}
              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-[var(--nm-surface)]/75 text-[11px] text-[var(--nm-text)] cursor-pointer"
            >
              <span>{t.navToday}</span>
              <ChevronDown className="w-3 h-3" />
            </button>

            {showMealMenu && sortedMeals.length > 1 && (
              <div
                onClick={(e) => e.stopPropagation()}
                className="absolute left-0 bottom-8 z-20 min-w-[160px] rounded-2xl bg-[var(--nm-surface)] border border-[var(--nm-outline)] py-1"
              >
                {sortedMeals.map((m, idx) => {
                  const kcal = m.items.reduce((s, i) => s + i.calories, 0);
                  return (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => {
                        setSelectedIdx(idx);
                        setShowMealMenu(false);
                      }}
                      className="w-full px-3 py-2 text-left text-xs text-[var(--nm-text)] hover:bg-[var(--nm-surface-variant)]/60 flex items-center justify-between cursor-pointer"
                    >
                      <span>{mealTypeText(m.mealType, language)}</span>
                      <span className="tabular-nums text-[var(--nm-muted)]">
                        {Math.round(kcal)} kcal
                      </span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </PastelCard>

      {/* Muscle Tips Bottom Sheet (opened from bell button) */}
      {showMuscleSheet && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-black/35"
          onClick={() => setShowMuscleSheet(false)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label={t.muscleScore}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md rounded-t-[28px] bg-[var(--nm-surface)] px-5 pt-2.5 pb-7 space-y-4 max-h-[85dvh] overflow-y-auto animate-[sheetUp_240ms_cubic-bezier(0.22,1,0.36,1)]"
          >
            <div className="w-9 h-1 rounded-full bg-[var(--nm-surface-variant)] mx-auto" />

            <div className="flex items-center justify-between">
              <h2 className="text-base font-medium text-[var(--nm-text)]">
                {t.muscleScore}
              </h2>
              <div className="flex items-center gap-2">
                <span className="px-3 py-1 rounded-full bg-[var(--nm-pastel-yellow)] text-sm font-medium tabular-nums text-[var(--nm-text)]">
                  {insights.score.totalScore} / 100
                </span>
                <button
                  type="button"
                  onClick={() => setShowMuscleSheet(false)}
                  aria-label={t.cancel}
                  className="p-1.5 rounded-full text-[var(--nm-muted)] hover:bg-[var(--nm-surface-variant)] cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-3 text-center py-3 rounded-[22px] bg-[var(--nm-bg)]">
              <div>
                <div className="text-[11px] text-[var(--nm-muted)]">
                  {t.proteinPart}
                </div>
                <div className="text-sm font-medium tabular-nums text-[var(--nm-text)] mt-0.5">
                  {insights.score.proteinPoints}/50
                </div>
              </div>
              <div>
                <div className="text-[11px] text-[var(--nm-muted)]">
                  {t.caloriesPart}
                </div>
                <div className="text-sm font-medium tabular-nums text-[var(--nm-text)] mt-0.5">
                  {insights.score.calorieBalancePoints}/30
                </div>
              </div>
              <div>
                <div className="text-[11px] text-[var(--nm-muted)]">
                  {t.spreadPart}
                </div>
                <div className="text-sm font-medium tabular-nums text-[var(--nm-text)] mt-0.5">
                  {insights.score.proteinSpreadPoints}/20
                </div>
              </div>
            </div>

            <div className="space-y-2.5">
              {insights.tips.map((tip, idx) => (
                <div
                  key={idx}
                  className="rounded-[20px] bg-[var(--nm-bg)] px-3.5 py-2.5 text-sm text-[var(--nm-text)]"
                >
                  {formatInsightTip(tip, language)}
                </div>
              ))}
            </div>

            {insights.suggestedFoods.length > 0 && (
              <div className="space-y-2.5">
                <div className="text-[11px] text-[var(--nm-muted)]">
                  {t.suggestions}
                </div>
                <div className="space-y-2">
                  {insights.suggestedFoods.slice(0, 4).map((food) => {
                    const name =
                      language === 'MM' && food.nameMm ? food.nameMm : food.nameEn;
                    const factor = food.defaultServingGrams / 100;
                    const prot =
                      food.caloriesPerServing > 0
                        ? food.proteinPerServing
                        : food.proteinPer100g * factor;
                    const kcal =
                      food.caloriesPerServing > 0
                        ? food.caloriesPerServing
                        : food.caloriesPer100g * factor;
                    const est = food.isEstimate ? '~' : '';
                    return (
                      <div
                        key={food.id}
                        className="flex items-center justify-between gap-2 rounded-[20px] bg-[var(--nm-bg)] px-3.5 py-2.5 text-xs"
                      >
                        <span className="text-[var(--nm-text)] font-medium truncate">
                          {name}
                        </span>
                        <span className="px-2.5 py-1 rounded-full bg-[var(--nm-pastel-sage)]/55 tabular-nums text-[11px] text-[var(--nm-text)] shrink-0">
                          {Math.round(prot)}g P · {est}
                          {Math.round(kcal)} kcal
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
