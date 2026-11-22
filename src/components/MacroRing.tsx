import { useEffect, useRef, useState } from 'react';

interface CalorieHeroRingProps {
  consumed: number;
  target: number;
  size?: number;
}

function useCountUp(value: number, durationMs = 450): number {
  const targetVal = Math.round(value);
  const [display, setDisplay] = useState(targetVal);
  const prevRef = useRef(targetVal);

  useEffect(() => {
    const start = prevRef.current;
    const end = targetVal;
    if (start === end) return;
    let rafId = 0;
    const startedAt = performance.now();

    const step = (now: number) => {
      const elapsed = now - startedAt;
      const progress = Math.min(1, elapsed / durationMs);
      const eased = 1 - Math.pow(1 - progress, 3);
      setDisplay(Math.round(start + (end - start) * eased));
      if (progress < 1) {
        rafId = requestAnimationFrame(step);
      } else {
        prevRef.current = end;
      }
    };

    rafId = requestAnimationFrame(step);
    return () => cancelAnimationFrame(rafId);
  }, [targetVal, durationMs]);

  return display;
}

export function CalorieHeroRing({
  consumed,
  target,
  size = 196,
}: CalorieHeroRingProps) {
  const strokeWidth = 11;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const ratio = target > 0 ? Math.min(1.25, Math.max(0, consumed / target)) : 0;
  const clampedProgress = Math.min(1, ratio);
  const isOver = ratio > 1.1;

  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    const id = requestAnimationFrame(() => setMounted(true));
    return () => cancelAnimationFrame(id);
  }, []);

  const animatedConsumed = useCountUp(consumed);
  const offset = circumference * (1 - (mounted ? clampedProgress : 0));

  return (
    <div
      className="relative flex items-center justify-center shrink-0"
      style={{ width: size, height: size }}
    >
      <svg
        width={size}
        height={size}
        className="-rotate-90"
        aria-label={`${Math.round(consumed)} of ${Math.round(target)} calories`}
      >
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          strokeWidth={strokeWidth}
          className="stroke-slate-200 dark:stroke-slate-800"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          className={`transition-[stroke-dashoffset] duration-500 ease-out ${
            isOver
              ? 'stroke-red-700 dark:stroke-red-400'
              : 'stroke-teal-700 dark:stroke-teal-400'
          }`}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center select-none">
        <span
          data-testid="calorie-eaten"
          className="text-4xl font-bold tracking-tight tabular-nums text-slate-900 dark:text-slate-100 leading-none"
        >
          {animatedConsumed.toLocaleString()}
        </span>
        <span className="mt-1.5 text-sm font-normal tabular-nums text-slate-500 dark:text-slate-400 leading-none">
          / {Math.round(target).toLocaleString()}
        </span>
      </div>
    </div>
  );
}

interface SlimMacroBarsProps {
  protein: number;
  proteinTarget: number;
  carbs: number;
  carbsTarget: number;
  fat: number;
  fatTarget: number;
  proteinLabel: string;
  carbsLabel: string;
  fatLabel: string;
}

export function SlimMacroBars({
  protein,
  proteinTarget,
  carbs,
  carbsTarget,
  fat,
  fatTarget,
  proteinLabel,
  carbsLabel,
  fatLabel,
}: SlimMacroBarsProps) {
  const items = [
    {
      label: proteinLabel,
      consumed: protein,
      target: proteinTarget,
      fillClass: 'bg-teal-700 dark:bg-teal-400',
    },
    {
      label: carbsLabel,
      consumed: carbs,
      target: carbsTarget,
      fillClass: 'bg-teal-700/70 dark:bg-teal-400/70',
    },
    {
      label: fatLabel,
      consumed: fat,
      target: fatTarget,
      fillClass: 'bg-teal-700/45 dark:bg-teal-400/45',
    },
  ];

  return (
    <div className="grid grid-cols-3 gap-4 w-full">
      {items.map((item) => {
        const pct =
          item.target > 0
            ? Math.min(100, Math.max(0, (item.consumed / item.target) * 100))
            : 0;
        return (
          <div key={item.label} className="space-y-1.5">
            <div className="text-[11px] font-normal text-slate-500 dark:text-slate-400 truncate">
              {item.label}
            </div>
            <div className="h-1.5 w-full rounded-full bg-slate-200 dark:bg-slate-800 overflow-hidden">
              <div
                className={`h-full rounded-full transition-[width] duration-500 ease-out ${item.fillClass}`}
                style={{ width: `${pct}%` }}
              />
            </div>
            <div className="text-xs font-bold tabular-nums text-slate-900 dark:text-slate-100 whitespace-nowrap">
              {Math.round(item.consumed)} / {Math.round(item.target)} g
            </div>
          </div>
        );
      })}
    </div>
  );
}
