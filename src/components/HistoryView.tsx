import { useEffect, useMemo, useRef, useState } from 'react';
import { Calendar, MoreVertical, Pencil, Trash2 } from 'lucide-react';
import { UI_STRINGS } from '../lib/i18n';
import { sumMealTotals } from '../lib/muscleInsights';
import { offsetIsoDate, todayIsoDate } from '../lib/storage';
import type {
  AppLanguage,
  LoggedFoodItem,
  MealEntry,
  NutritionTargets,
  NutritionTotals,
} from '../lib/types';
import {
  PastelCard,
  PillGauge,
  SegmentedBar,
  WeekStrip,
} from './PastelComponents';

interface HistoryViewProps {
  language: AppLanguage;
  meals: MealEntry[];
  targets: NutritionTargets;
  isDark: boolean;
  onEditMeal: (meal: MealEntry) => void;
  onDeleteMeal: (mealId: string) => void;
  onOpenAddFood: (dateIso: string) => void;
}

type HistorySubTab = 'log' | 'stats';

interface DayFoodCardEntry {
  key: string;
  meal: MealEntry;
  item: LoggedFoodItem;
}

interface DayPoint {
  dateIso: string;
  shortLabel: string;
  totals: NutritionTotals;
  meals: MealEntry[];
}

export function HistoryView({
  language,
  meals,
  targets,
  isDark,
  onEditMeal,
  onDeleteMeal,
  onOpenAddFood,
}: HistoryViewProps) {
  const t = UI_STRINGS[language];
  const [subTab, setSubTab] = useState<HistorySubTab>('log');
  const [rangeDays, setRangeDays] = useState<7 | 30>(7);
  const [selectedDateIso, setSelectedDateIso] = useState<string>(todayIsoDate);
  const [openMenuKey, setOpenMenuKey] = useState<string | null>(null);
  const [pendingDeleteMealId, setPendingDeleteMealId] = useState<string | null>(
    null
  );
  const dateInputRef = useRef<HTMLInputElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const dayMeals = useMemo(
    () => meals.filter((m) => m.dateIso === selectedDateIso),
    [meals, selectedDateIso]
  );

  const totals = useMemo(() => sumMealTotals(dayMeals), [dayMeals]);

  const caloriesLeft = Math.max(
    0,
    Math.round(targets.calorieTarget - totals.calories)
  );

  const foodEntries: DayFoodCardEntry[] = useMemo(() => {
    const out: DayFoodCardEntry[] = [];
    for (const meal of dayMeals) {
      meal.items.forEach((item, idx) => {
        out.push({
          key: `${meal.id}_${idx}_${item.foodId}`,
          meal,
          item,
        });
      });
    }
    return out;
  }, [dayMeals]);

  const segmentedParts = useMemo(
    () => [
      { value: Math.max(0, totals.protein * 4), color: 'var(--nm-pastel-teal)' },
      { value: Math.max(0, totals.carbs * 4), color: 'var(--nm-pastel-yellow)' },
      { value: Math.max(0, totals.fat * 9), color: 'var(--nm-pastel-sage)' },
    ],
    [totals.protein, totals.carbs, totals.fat]
  );

  const points: DayPoint[] = useMemo(() => {
    const map = new Map<string, MealEntry[]>();
    for (const m of meals) {
      const list = map.get(m.dateIso) ?? [];
      list.push(m);
      map.set(m.dateIso, list);
    }
    const result: DayPoint[] = [];
    for (let offset = rangeDays - 1; offset >= 0; offset--) {
      const iso = offsetIsoDate(offset);
      const list = map.get(iso) ?? [];
      const [, m, d] = iso.split('-');
      result.push({
        dateIso: iso,
        shortLabel: `${Number(m)}/${Number(d)}`,
        totals: sumMealTotals(list),
        meals: list,
      });
    }
    return result;
  }, [meals, rangeDays]);

  const reversedPoints = useMemo(() => [...points].reverse(), [points]);

  useEffect(() => {
    if (subTab !== 'stats') return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    const width = canvas.clientWidth || 320;
    const height = canvas.clientHeight || 136;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, width, height);

    const targetVal = targets.calorieTarget;
    const maxData = Math.max(
      targetVal * 1.2,
      ...points.map((p) => p.totals.calories * 1.15),
      100
    );

    const topPad = 8;
    const bottomPad = 4;
    const chartH = height - topPad - bottomPad;
    const slotW = width / points.length;
    const barW = Math.max(3, Math.min(24, slotW * 0.56));

    const barTeal = isDark ? '#5A8590' : '#A8CED6';
    const barSage = isDark ? '#6E8854' : '#C6DBA8';
    const emptyColor = isDark ? '#283034' : '#EAECE8';
    const targetColor = isDark ? '#B97852' : '#EBA77C';

    points.forEach((p, idx) => {
      const val = p.totals.calories;
      const ratio = Math.min(1, val / maxData);
      const barH = Math.max(val > 0 ? 6 : 3, chartH * ratio);
      const x = idx * slotW + (slotW - barW) / 2;
      const y = topPad + chartH - barH;

      ctx.fillStyle =
        val <= 0
          ? emptyColor
          : val >= targetVal * 0.9
          ? barSage
          : barTeal;
      ctx.beginPath();
      ctx.roundRect(x, y, barW, barH, Math.min(barW / 2, 10));
      ctx.fill();
    });

    const targetRatio = Math.min(1, targetVal / maxData);
    const targetY = topPad + chartH - chartH * targetRatio;
    ctx.save();
    ctx.setLineDash([6, 5]);
    ctx.strokeStyle = targetColor;
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(0, targetY);
    ctx.lineTo(width, targetY);
    ctx.stroke();
    ctx.restore();
  }, [subTab, points, targets.calorieTarget, isDark]);

  function openDatePicker() {
    const input = dateInputRef.current;
    if (!input) return;
    if (typeof input.showPicker === 'function') {
      input.showPicker();
    } else {
      input.focus();
      input.click();
    }
  }

  const calVal = Math.round(totals.calories);
  const calLabel =
    calVal >= 1000 ? `${(calVal / 1000).toFixed(1)}k` : String(calVal);
  const calProg =
    targets.calorieTarget > 0 ? totals.calories / targets.calorieTarget : 0;
  const protProg =
    targets.proteinGrams > 0 ? totals.protein / targets.proteinGrams : 0;
  const carbProg =
    targets.carbsGrams > 0 ? totals.carbs / targets.carbsGrams : 0;
  const fatProg = targets.fatGrams > 0 ? totals.fat / targets.fatGrams : 0;
  const rdcPct = Math.max(0, Math.round(calProg * 100));

  return (
    <div
      className={`px-5 pt-5 space-y-4 min-h-dvh ${
        subTab === 'log' ? 'pb-40' : 'pb-28'
      }`}
      onClick={() => openMenuKey && setOpenMenuKey(null)}
    >
      {/* Top row: Title + Log/Stats pill switch + calendar icon button */}
      <header className="flex items-center justify-between gap-2">
        <h1 className="text-[28px] leading-tight font-medium tracking-tight text-[var(--nm-text)]">
          {subTab === 'log' ? 'Calories' : t.navHistory}
        </h1>

        <div className="flex items-center gap-2">
          <div className="inline-flex p-0.5 rounded-full bg-[var(--nm-surface)]">
            <button
              type="button"
              data-testid="history-tab-log"
              onClick={() => setSubTab('log')}
              className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                subTab === 'log'
                  ? 'bg-[var(--nm-pastel-yellow)] text-[var(--nm-text)]'
                  : 'text-[var(--nm-muted)]'
              }`}
            >
              {language === 'MM' ? 'မှတ်တမ်း' : 'Log'}
            </button>
            <button
              type="button"
              data-testid="history-tab-stats"
              onClick={() => setSubTab('stats')}
              className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                subTab === 'stats'
                  ? 'bg-[var(--nm-pastel-yellow)] text-[var(--nm-text)]'
                  : 'text-[var(--nm-muted)]'
              }`}
            >
              {language === 'MM' ? 'စာရင်း' : 'Stats'}
            </button>
          </div>

          <div className="relative">
            <button
              type="button"
              data-testid="calendar-picker-btn"
              aria-label="Pick date"
              onClick={openDatePicker}
              className="w-10 h-10 rounded-full bg-[var(--nm-surface)] text-[var(--nm-text)] flex items-center justify-center cursor-pointer"
            >
              <Calendar className="w-[18px] h-[18px]" />
            </button>
            <input
              ref={dateInputRef}
              type="date"
              aria-label="Select date"
              value={selectedDateIso}
              onChange={(e) => {
                if (e.target.value) setSelectedDateIso(e.target.value);
              }}
              className="sr-only"
            />
          </div>
        </div>
      </header>

      {subTab === 'log' ? (
        <>
          {/* WeekStrip */}
          <WeekStrip
            selectedDate={selectedDateIso}
            onSelectDate={(iso) => {
              setSelectedDateIso(iso);
              setOpenMenuKey(null);
            }}
          />

          {/* Calorie left + big number + SegmentedBar */}
          <section className="space-y-3 pt-1">
            <div className="space-y-0.5">
              <div className="text-[12px] text-[var(--nm-muted)]">
                Calorie left
              </div>
              <div className="flex items-baseline gap-1.5 tabular-nums">
                <span
                  data-testid="calories-left-value"
                  className="text-[38px] leading-none font-medium tracking-tight text-[var(--nm-text)]"
                >
                  {caloriesLeft.toLocaleString()}
                </span>
                <span className="text-[12px] text-[var(--nm-muted)]">kcal</span>
              </div>
            </div>
            <SegmentedBar parts={segmentedParts} height={10} />
          </section>

          {/* 2-column grid of food cards (soft blue and white alternating) */}
          <section
            aria-label="Daily log"
            className="grid grid-cols-2 gap-3 pt-1"
          >
            {foodEntries.map((entry, idx) => {
              const isBlue = (Math.floor(idx / 2) + (idx % 2)) % 2 === 0;
              const foodName =
                language === 'MM' && entry.item.foodNameMm
                  ? entry.item.foodNameMm
                  : entry.item.foodNameEn;
              const kcal = Math.round(entry.item.calories);
              const carbs = Math.round(entry.item.carbs);
              const est = entry.item.isEstimate ? '~' : '';
              const menuOpen = openMenuKey === entry.key;

              return (
                <PastelCard
                  key={entry.key}
                  color={isBlue ? 'var(--nm-pastel-teal)' : 'var(--nm-surface)'}
                  onClick={() => onEditMeal(entry.meal)}
                  className="relative flex flex-col justify-between min-h-[126px] cursor-pointer"
                >
                  <div className="flex items-start justify-between gap-1">
                    <span className="text-[14px] font-medium leading-snug text-[var(--nm-text)] line-clamp-2 min-w-0">
                      {foodName}
                    </span>
                    <button
                      type="button"
                      aria-label="More options"
                      data-testid={`food-card-menu-${idx}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        setOpenMenuKey(menuOpen ? null : entry.key);
                      }}
                      className="w-7 h-7 -mr-1.5 -mt-1 rounded-full flex items-center justify-center text-[var(--nm-text)]/70 hover:bg-black/5 dark:hover:bg-white/10 shrink-0 cursor-pointer"
                    >
                      <MoreVertical className="w-4 h-4" />
                    </button>
                  </div>

                  <div className="flex items-end justify-between gap-2 pt-3 tabular-nums">
                    <div>
                      <div className="text-[18px] leading-tight font-medium text-[var(--nm-text)]">
                        {est}
                        {kcal.toLocaleString()}
                      </div>
                      <div className="text-[11px] text-[var(--nm-text)]/60">
                        kcal
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-[18px] leading-tight font-medium text-[var(--nm-text)]">
                        {carbs}g
                      </div>
                      <div className="text-[11px] text-[var(--nm-text)]/60">
                        Carbs
                      </div>
                    </div>
                  </div>

                  {menuOpen && (
                    <div
                      onClick={(e) => e.stopPropagation()}
                      className="absolute right-3 top-10 z-20 min-w-[112px] rounded-2xl bg-[var(--nm-surface)] border border-[var(--nm-outline)] py-1 overflow-hidden"
                    >
                      <button
                        type="button"
                        onClick={() => {
                          setOpenMenuKey(null);
                          onEditMeal(entry.meal);
                        }}
                        className="w-full px-3 py-2 flex items-center gap-2 text-xs text-[var(--nm-text)] hover:bg-[var(--nm-bg)] cursor-pointer"
                      >
                        <Pencil className="w-3.5 h-3.5" />
                        <span>{t.edit}</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setOpenMenuKey(null);
                          setPendingDeleteMealId(entry.meal.id);
                        }}
                        className="w-full px-3 py-2 flex items-center gap-2 text-xs text-red-600 dark:text-red-400 hover:bg-[var(--nm-bg)] cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        <span>{t.delete}</span>
                      </button>
                    </div>
                  )}
                </PastelCard>
              );
            })}
          </section>

          {/* Bottom wide soft-yellow pill button "Add food >>>" above FloatingNavBar */}
          <div className="fixed bottom-[86px] left-1/2 -translate-x-1/2 z-30 w-full max-w-md px-5 pointer-events-none">
            <button
              type="button"
              data-testid="daily-log-add-btn"
              onClick={() => onOpenAddFood(selectedDateIso)}
              className="pointer-events-auto w-full h-[52px] rounded-full bg-[var(--nm-pastel-yellow)] text-[var(--nm-text)] text-[14px] font-medium flex items-center justify-center gap-2 cursor-pointer"
            >
              <span>Add food &gt;&gt;&gt;</span>
            </button>
          </div>
        </>
      ) : (
        <div className="space-y-3">
          {/* 1. Nutrient overview white card: 5 PillGauges */}
          <PastelCard data-testid="nutrient-overview-card">
            <div className="flex items-end justify-between gap-1">
              <PillGauge
                label="Cal"
                value={calLabel}
                progress={calProg}
                fillColor="var(--nm-pastel-peach)"
              />
              <PillGauge
                label="Prot"
                value={`${Math.round(totals.protein)}g`}
                progress={protProg}
                fillColor="var(--nm-pastel-teal)"
              />
              <PillGauge
                label="Carb"
                value={`${Math.round(totals.carbs)}g`}
                progress={carbProg}
                fillColor="var(--nm-pastel-yellow)"
              />
              <PillGauge
                label="Fats"
                value={`${Math.round(totals.fat)}g`}
                progress={fatProg}
                fillColor="var(--nm-pastel-sage)"
              />
              <PillGauge
                label="RDC"
                value={`${rdcPct}%`}
                progress={calProg}
                fillColor="var(--nm-pastel-teal)"
              />
            </div>
          </PastelCard>

          {/* 2. 7/30-day pastel bar chart with dashed target line */}
          <PastelCard className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[12px] text-[var(--nm-muted)]">kcal</span>
              <div className="inline-flex p-0.5 rounded-full bg-[var(--nm-bg)]">
                <button
                  type="button"
                  data-testid="range-7d"
                  onClick={() => setRangeDays(7)}
                  className={`px-3 py-1 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                    rangeDays === 7
                      ? 'bg-[var(--nm-pastel-yellow)] text-[var(--nm-text)]'
                      : 'text-[var(--nm-muted)]'
                  }`}
                >
                  {t.range7}
                </button>
                <button
                  type="button"
                  data-testid="range-30d"
                  onClick={() => setRangeDays(30)}
                  className={`px-3 py-1 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                    rangeDays === 30
                      ? 'bg-[var(--nm-pastel-yellow)] text-[var(--nm-text)]'
                      : 'text-[var(--nm-muted)]'
                  }`}
                >
                  {t.range30}
                </button>
              </div>
            </div>

            <canvas
              ref={canvasRef}
              aria-label="Daily calories chart"
              className="w-full h-[136px] block"
            />

            {rangeDays === 7 && (
              <div className="grid grid-cols-7 text-center">
                {points.map((p) => (
                  <span
                    key={p.dateIso}
                    className="text-[11px] tabular-nums text-[var(--nm-muted)]"
                  >
                    {p.shortLabel}
                  </span>
                ))}
              </div>
            )}
          </PastelCard>

          {/* 3. Compact day rows on white cards */}
          <div className="space-y-2.5">
            {reversedPoints.map((p) => {
              const kcal = Math.round(p.totals.calories);
              const prot = Math.round(p.totals.protein);
              const isSelected = p.dateIso === selectedDateIso;

              return (
                <PastelCard
                  key={p.dateIso}
                  color={isSelected ? 'var(--nm-pastel-teal)' : undefined}
                  onClick={() => {
                    setSelectedDateIso(p.dateIso);
                    setSubTab('log');
                  }}
                >
                  <div
                    data-testid={`history-day-${p.dateIso}`}
                    className="flex items-center justify-between"
                  >
                    <span className="text-sm font-medium tabular-nums text-[var(--nm-text)]">
                      {p.shortLabel}
                    </span>
                    <div className="flex items-baseline gap-4 tabular-nums">
                      <span className="text-xs text-[var(--nm-muted)]">
                        {prot}g P
                      </span>
                      <span
                        className={`text-sm font-medium min-w-[68px] text-right ${
                          kcal > 0
                            ? 'text-[var(--nm-text)]'
                            : 'text-[var(--nm-muted)]'
                        }`}
                      >
                        {kcal.toLocaleString()} kcal
                      </span>
                    </div>
                  </div>
                </PastelCard>
              );
            })}
          </div>
        </div>
      )}

      {/* Pastel 28px Delete Confirm modal */}
      {pendingDeleteMealId && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-5"
          onClick={() => setPendingDeleteMealId(null)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label={t.delete}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-xs rounded-[28px] bg-[var(--nm-surface)] p-5 space-y-4 animate-[fadeInItem_180ms_ease-out]"
          >
            <div className="space-y-1.5">
              <h3 className="text-base font-medium text-[var(--nm-text)]">
                {t.delete}
              </h3>
              <p className="text-sm text-[var(--nm-muted)]">
                {language === 'MM'
                  ? 'ဒီမှတ်တမ်းကို ဖျက်မှာသေချာပါသလား?'
                  : 'Remove this entry from your log?'}
              </p>
            </div>
            <div className="flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setPendingDeleteMealId(null)}
                className="px-3.5 py-2 text-xs font-medium text-[var(--nm-muted)] cursor-pointer"
              >
                {t.cancel}
              </button>
              <button
                type="button"
                data-testid="confirm-delete-btn"
                onClick={() => {
                  onDeleteMeal(pendingDeleteMealId);
                  setPendingDeleteMealId(null);
                }}
                className="px-5 py-2 rounded-full bg-[var(--nm-pastel-yellow)] text-xs font-medium text-[var(--nm-text)] cursor-pointer"
              >
                {t.delete}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
