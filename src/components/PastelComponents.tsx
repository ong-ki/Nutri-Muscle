import type { ReactNode } from 'react';

export interface PastelCardProps {
  color?: string;
  className?: string;
  onClick?: () => void;
  children: ReactNode;
}

export function PastelCard({
  color,
  className = '',
  onClick,
  children,
}: PastelCardProps) {
  const baseClass = `rounded-[28px] p-4 shadow-none ${
    color ? '' : 'bg-[var(--nm-surface)]'
  } ${onClick ? 'cursor-pointer' : ''} ${className}`;

  if (onClick) {
    return (
      <div
        role="button"
        tabIndex={0}
        onClick={onClick}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onClick();
          }
        }}
        style={color ? { backgroundColor: color } : undefined}
        className={baseClass}
      >
        {children}
      </div>
    );
  }

  return (
    <div
      style={color ? { backgroundColor: color } : undefined}
      className={baseClass}
    >
      {children}
    </div>
  );
}

export interface CalorieRingProps {
  value: number;
  target: number;
  size?: number;
  strokeWidth?: number;
}

export function CalorieRing({
  value,
  target,
  size = 176,
  strokeWidth = 10,
}: CalorieRingProps) {
  const ratio = target > 0 ? Math.min(1, Math.max(0, value / target)) : 0;
  const r = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * r;
  // Open gap (~44deg) at 12 o'clock -> arc covers 316/360 of full circle
  const arcLength = circumference * (316 / 360);
  const gapLength = circumference - arcLength;
  const progressLength = arcLength * ratio;

  return (
    <div
      className="relative inline-flex items-center justify-center shrink-0"
      style={{ width: size, height: size }}
    >
      <svg
        width={size}
        height={size}
        role="img"
        aria-label={`${Math.round(value)} of ${Math.round(target)} kcal`}
        style={{ transform: 'rotate(-68deg)' }}
      >
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke="var(--nm-surface-variant)"
          strokeWidth={strokeWidth}
          strokeDasharray={`${arcLength} ${gapLength}`}
          strokeLinecap="round"
        />
        {ratio > 0 && (
          <circle
            cx={size / 2}
            cy={size / 2}
            r={r}
            fill="none"
            stroke="var(--nm-pastel-peach)"
            strokeWidth={strokeWidth}
            strokeDasharray={`${progressLength} ${circumference}`}
            strokeLinecap="round"
            className="transition-all duration-500 ease-out"
          />
        )}
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="nm-big-number text-[var(--nm-text)] leading-none">
          {Math.round(value).toLocaleString()}
        </span>
        <span className="nm-label mt-1">kcal</span>
      </div>
    </div>
  );
}

export interface PillGaugeProps {
  label: string;
  value: string;
  progress: number;
  fillColor?: string;
}

export function PillGauge({
  label,
  value,
  progress,
  fillColor = 'var(--nm-pastel-teal)',
}: PillGaugeProps) {
  const clamped = Math.min(1, Math.max(0, progress));
  const displayPct = Math.round((0.34 + 0.66 * clamped) * 100);

  return (
    <div className="flex flex-col items-center gap-2">
      <div className="w-[44px] h-[120px] rounded-full bg-[var(--nm-surface-variant)] overflow-hidden flex items-end">
        <div
          style={{ height: `${displayPct}%`, backgroundColor: fillColor }}
          className="w-full rounded-full flex justify-center pt-[5px] transition-all duration-400 ease-out"
        >
          <div className="w-[34px] h-[34px] rounded-full bg-[var(--nm-surface)] flex items-center justify-center shrink-0">
            <span className="text-[10px] font-medium tabular-nums text-[var(--nm-text)] leading-none">
              {value}
            </span>
          </div>
        </div>
      </div>
      <span className="text-[11px] text-[var(--nm-muted)] leading-none">
        {label}
      </span>
    </div>
  );
}

export interface WeekStripProps {
  selectedDate: string; // YYYY-MM-DD
  onSelectDate?: (isoDate: string) => void;
}

const DAY_LETTERS = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

function parseIsoLocal(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, (m || 1) - 1, d || 1);
}

function formatIsoLocal(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function WeekStrip({ selectedDate, onSelectDate }: WeekStripProps) {
  const anchor = parseIsoLocal(selectedDate);
  const dow = (anchor.getDay() + 6) % 7; // Monday = 0
  const monday = new Date(anchor);
  monday.setDate(anchor.getDate() - dow);

  return (
    <div className="w-full flex items-center justify-between">
      {DAY_LETTERS.map((letter, idx) => {
        const d = new Date(monday);
        d.setDate(monday.getDate() + idx);
        const iso = formatIsoLocal(d);
        const isSelected = iso === selectedDate;
        return (
          <button
            key={iso}
            type="button"
            onClick={() => onSelectDate?.(iso)}
            className={`w-10 py-2 rounded-full flex flex-col items-center gap-1 transition-colors cursor-pointer ${
              isSelected
                ? 'bg-[var(--nm-pastel-teal)] text-[#1F2A2E] dark:text-[#EFF3F4]'
                : 'bg-transparent text-[var(--nm-text)] hover:bg-[var(--nm-surface-variant)]/60'
            }`}
          >
            <span
              className={`text-[11px] leading-none ${
                isSelected ? 'opacity-80' : 'text-[var(--nm-muted)]'
              }`}
            >
              {letter}
            </span>
            <span
              className={`text-xs tabular-nums leading-none ${
                isSelected ? 'font-semibold' : 'font-medium'
              }`}
            >
              {d.getDate()}
            </span>
            <span
              className={`w-1 h-1 rounded-full ${
                isSelected ? 'bg-current' : 'bg-transparent'
              }`}
            />
          </button>
        );
      })}
    </div>
  );
}

export interface SegmentPart {
  value: number;
  color?: string;
}

export function SegmentedBar({
  parts,
  height = 10,
}: {
  parts: SegmentPart[];
  height?: number;
}) {
  const defaults = [
    'var(--nm-pastel-teal)',
    'var(--nm-pastel-yellow)',
    'var(--nm-pastel-sage)',
  ];
  const total = parts.reduce((sum, p) => sum + Math.max(0, p.value), 0);

  return (
    <div
      style={{ height }}
      className="w-full rounded-full bg-[var(--nm-surface-variant)] overflow-hidden flex gap-[2px]"
    >
      {total > 0 &&
        parts.map((part, idx) => {
          const pct = (Math.max(0, part.value) / total) * 100;
          if (pct < 0.5) return null;
          return (
            <div
              key={idx}
              style={{
                width: `${pct}%`,
                backgroundColor: part.color ?? defaults[idx % defaults.length],
              }}
              className="h-full transition-all duration-300"
            />
          );
        })}
    </div>
  );
}

export interface FloatingNavItem {
  key: string;
  label: string;
  testId?: string;
  icon: ReactNode;
}

export function FloatingNavBar({
  items,
  selected,
  onSelect,
}: {
  items: FloatingNavItem[];
  selected: string;
  onSelect: (key: string) => void;
}) {
  return (
    <nav
      aria-label="Bottom Navigation"
      className="fixed bottom-4 left-1/2 -translate-x-1/2 z-40 rounded-full bg-[var(--nm-surface)] px-2.5 py-2 flex items-center gap-3 border border-[var(--nm-outline)]/60"
    >
      {items.map((item) => {
        const active = item.key === selected;
        return (
          <button
            key={item.key}
            type="button"
            data-testid={item.testId}
            aria-label={item.label}
            title={item.label}
            onClick={() => onSelect(item.key)}
            className={`w-12 h-12 rounded-full flex items-center justify-center transition-colors cursor-pointer ${
              active
                ? 'bg-[var(--nm-pastel-yellow)] text-[#1F2A2E] dark:text-[#EFF3F4]'
                : 'text-[var(--nm-muted)] hover:text-[var(--nm-text)]'
            }`}
          >
            {item.icon}
          </button>
        );
      })}
    </nav>
  );
}
