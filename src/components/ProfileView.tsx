import { useMemo, useRef, useState } from 'react';
import {
  Bell,
  ChevronDown,
  ChevronUp,
  Download,
  FolderCode,
  Upload,
} from 'lucide-react';
import {
  activityText,
  bmiCategoryText,
  goalText,
  themeModeText,
  UI_STRINGS,
} from '../lib/i18n';
import {
  ACTIVITY_FACTORS,
  calculateTargets,
  validateProfile,
} from '../lib/nutritionCalculator';
import type {
  ActivityLevel,
  AppLanguage,
  FitnessGoal,
  Gender,
  NutriMuscleBackup,
  ThemeMode,
  UserProfile,
} from '../lib/types';
import { PastelCard } from './PastelComponents';

interface ProfileViewProps {
  profile: UserProfile;
  themeMode: ThemeMode;
  onUpdateProfile: (profile: UserProfile) => void;
  onChangeThemeMode: (mode: ThemeMode) => void;
  onExportBackup: () => NutriMuscleBackup;
  onImportBackup: (backup: NutriMuscleBackup) => void;
  onOpenAndroidProject: () => void;
}

type SectionKey = 'body' | 'goal' | 'app';

const ACTIVITY_LEVELS: ActivityLevel[] = [
  'SEDENTARY',
  'LIGHT',
  'MODERATE',
  'ACTIVE',
  'VERY_ACTIVE',
];

const GOALS: FitnessGoal[] = ['MUSCLE_GAIN', 'FAT_LOSS', 'MAINTAIN'];

const THEMES: ThemeMode[] = ['SYSTEM', 'LIGHT', 'DARK'];

interface WebReminderSlot {
  key: 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK';
  labelEn: string;
  labelMm: string;
  enabled: boolean;
  time: string;
}

interface WebReminderSettings {
  masterEnabled: boolean;
  slots: WebReminderSlot[];
}

const REMINDERS_KEY = 'nutrimuscle_reminders_v1';

const DEFAULT_REMINDERS: WebReminderSettings = {
  masterEnabled: false,
  slots: [
    { key: 'BREAKFAST', labelEn: 'Breakfast', labelMm: 'မနက်စာ', enabled: true, time: '08:00' },
    { key: 'LUNCH', labelEn: 'Lunch', labelMm: 'နေ့လယ်စာ', enabled: true, time: '12:30' },
    { key: 'DINNER', labelEn: 'Dinner', labelMm: 'ညနေစာ', enabled: true, time: '19:00' },
    { key: 'SNACK', labelEn: 'Evening snack', labelMm: 'ညနေသရေစာ', enabled: false, time: '21:00' },
  ],
};

function loadWebReminders(): WebReminderSettings {
  try {
    const raw = localStorage.getItem(REMINDERS_KEY);
    if (!raw) return DEFAULT_REMINDERS;
    const parsed = JSON.parse(raw) as WebReminderSettings;
    if (!Array.isArray(parsed.slots) || parsed.slots.length !== 4) {
      return DEFAULT_REMINDERS;
    }
    return parsed;
  } catch {
    return DEFAULT_REMINDERS;
  }
}

export function ProfileView({
  profile,
  themeMode,
  onUpdateProfile,
  onChangeThemeMode,
  onExportBackup,
  onImportBackup,
  onOpenAndroidProject,
}: ProfileViewProps) {
  const t = UI_STRINGS[profile.language];
  const [expanded, setExpanded] = useState<SectionKey | null>('body');
  const [weightStr, setWeightStr] = useState(String(profile.weightKg));
  const [heightStr, setHeightStr] = useState(String(profile.heightCm));
  const [ageStr, setAgeStr] = useState(String(profile.age));
  const [backupStatus, setBackupStatus] = useState<string | null>(null);
  const [reminders, setReminders] = useState<WebReminderSettings>(loadWebReminders);
  const [remindersOpen, setRemindersOpen] = useState(false);
  const [editingSlotKey, setEditingSlotKey] = useState<WebReminderSlot['key'] | null>(null);
  const [tempTime, setTempTime] = useState('08:00');
  const [testBanner, setTestBanner] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  function saveReminders(next: WebReminderSettings) {
    setReminders(next);
    try {
      localStorage.setItem(REMINDERS_KEY, JSON.stringify(next));
    } catch {
      // ignore storage errors
    }
  }

  function triggerTestNotification() {
    const kcalStr = Math.round(targets.calorieTarget).toLocaleString();
    const msg =
      profile.language === 'MM'
        ? `ညနေစာ မှတ်ဖို့ မမေ့နဲ့ · ${kcalStr} kcal left`
        : `Don't forget Dinner · ${kcalStr} kcal left`;
    setTestBanner(msg);
    setTimeout(() => setTestBanner(null), 3500);
  }

  const w = parseFloat(weightStr);
  const h = parseFloat(heightStr);
  const a = parseInt(ageStr, 10);

  const validation = useMemo(
    () =>
      validateProfile(
        Number.isNaN(w) ? null : w,
        Number.isNaN(h) ? null : h,
        Number.isNaN(a) ? null : a
      ),
    [w, h, a]
  );

  const targets = useMemo(() => {
    if (validation.isValid) {
      return calculateTargets({
        ...profile,
        weightKg: w,
        heightCm: h,
        age: a,
      });
    }
    return calculateTargets(profile);
  }, [validation.isValid, profile, w, h, a]);

  function autoPersist(
    nextWeightStr: string,
    nextHeightStr: string,
    nextAgeStr: string,
    overrides?: Partial<UserProfile>
  ) {
    const nw = parseFloat(nextWeightStr);
    const nh = parseFloat(nextHeightStr);
    const na = parseInt(nextAgeStr, 10);
    const v = validateProfile(
      Number.isNaN(nw) ? null : nw,
      Number.isNaN(nh) ? null : nh,
      Number.isNaN(na) ? null : na
    );
    if (v.isValid) {
      onUpdateProfile({
        ...profile,
        weightKg: nw,
        heightCm: nh,
        age: na,
        ...overrides,
      });
    } else if (overrides) {
      onUpdateProfile({ ...profile, ...overrides });
    }
  }

  function handleExport() {
    const data = onExportBackup();
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json',
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'nutrimuscle-backup.json';
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    setBackupStatus(t.exportedMsg);
    setTimeout(() => setBackupStatus(null), 2000);
  }

  async function handleImport(file: File | null) {
    if (!file) return;
    try {
      const text = await file.text();
      const parsed = JSON.parse(text) as NutriMuscleBackup;
      if (!parsed.profile || !Array.isArray(parsed.meals)) {
        throw new Error('Invalid');
      }
      onImportBackup(parsed);
      setWeightStr(String(parsed.profile.weightKg));
      setHeightStr(String(parsed.profile.heightCm));
      setAgeStr(String(parsed.profile.age));
      setBackupStatus(t.importedMsg);
    } catch {
      setBackupStatus(t.backupErrorMsg);
    }
    setTimeout(() => setBackupStatus(null), 2500);
  }

  function toggleSection(key: SectionKey) {
    setExpanded((prev) => (prev === key ? null : key));
  }

  return (
    <div className="px-5 pt-5 pb-28 space-y-3">
      {/* Top: 4 small target numbers in one row */}
      <PastelCard>
        <div
          aria-label="Daily targets"
          data-testid="profile-targets-row"
          className="grid grid-cols-4 gap-2 text-center"
        >
          <div>
            <div
              data-testid="target-calories"
              className="text-base font-medium tabular-nums text-[var(--nm-text)]"
            >
              {Math.round(targets.calorieTarget).toLocaleString()}
            </div>
            <div className="text-[11px] text-[var(--nm-muted)] mt-0.5">
              {t.calShort}
            </div>
          </div>
          <div>
            <div
              data-testid="target-protein"
              className="text-base font-medium tabular-nums text-[var(--nm-text)]"
            >
              {Math.round(targets.proteinGrams)}g
            </div>
            <div className="text-[11px] text-[var(--nm-muted)] mt-0.5">
              {t.protein}
            </div>
          </div>
          <div>
            <div className="text-base font-medium tabular-nums text-[var(--nm-text)]">
              {Math.round(targets.carbsGrams)}g
            </div>
            <div className="text-[11px] text-[var(--nm-muted)] mt-0.5">
              {t.carbs}
            </div>
          </div>
          <div>
            <div className="text-base font-medium tabular-nums text-[var(--nm-text)]">
              {Math.round(targets.fatGrams)}g
            </div>
            <div className="text-[11px] text-[var(--nm-muted)] mt-0.5">
              {t.fat}
            </div>
          </div>
        </div>
      </PastelCard>

      {/* 1. Body collapsible white card */}
      <PastelCard>
        <button
          type="button"
          data-testid="section-body"
          onClick={() => toggleSection('body')}
          className="w-full flex items-center justify-between text-left cursor-pointer"
        >
          <div>
            <div className="text-sm font-medium text-[var(--nm-text)]">
              {t.sectionBody}
            </div>
            {expanded !== 'body' && (
              <div className="text-[11px] tabular-nums text-[var(--nm-muted)] mt-0.5">
                {weightStr} kg · {heightStr} cm · BMI {targets.bmi.toFixed(1)}
              </div>
            )}
          </div>
          {expanded === 'body' ? (
            <ChevronUp className="w-4 h-4 text-[var(--nm-muted)]" />
          ) : (
            <ChevronDown className="w-4 h-4 text-[var(--nm-muted)]" />
          )}
        </button>

        {expanded === 'body' && (
          <div className="pt-4 space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <label className="block space-y-1">
                <span className="text-[11px] text-[var(--nm-muted)]">
                  {t.weightKg}
                </span>
                <input
                  type="number"
                  data-testid="input-weight"
                  value={weightStr}
                  onChange={(e) => {
                    setWeightStr(e.target.value);
                    autoPersist(e.target.value, heightStr, ageStr);
                  }}
                  className={`w-full h-10 px-3.5 text-sm tabular-nums rounded-full bg-[var(--nm-bg)] text-[var(--nm-text)] focus:outline-none ${
                    validation.weightError ? 'ring-1 ring-red-500' : ''
                  }`}
                />
                {validation.weightError && (
                  <span className="text-[11px] text-red-600 dark:text-red-400">
                    {t.errWeight}
                  </span>
                )}
              </label>

              <label className="block space-y-1">
                <span className="text-[11px] text-[var(--nm-muted)]">
                  {t.heightCm}
                </span>
                <input
                  type="number"
                  data-testid="input-height"
                  value={heightStr}
                  onChange={(e) => {
                    setHeightStr(e.target.value);
                    autoPersist(weightStr, e.target.value, ageStr);
                  }}
                  className={`w-full h-10 px-3.5 text-sm tabular-nums rounded-full bg-[var(--nm-bg)] text-[var(--nm-text)] focus:outline-none ${
                    validation.heightError ? 'ring-1 ring-red-500' : ''
                  }`}
                />
                {validation.heightError && (
                  <span className="text-[11px] text-red-600 dark:text-red-400">
                    {t.errHeight}
                  </span>
                )}
              </label>
            </div>

            <div className="grid grid-cols-2 gap-3 items-end">
              <label className="block space-y-1">
                <span className="text-[11px] text-[var(--nm-muted)]">
                  {t.age}
                </span>
                <input
                  type="number"
                  data-testid="input-age"
                  value={ageStr}
                  onChange={(e) => {
                    setAgeStr(e.target.value);
                    autoPersist(weightStr, heightStr, e.target.value);
                  }}
                  className={`w-full h-10 px-3.5 text-sm tabular-nums rounded-full bg-[var(--nm-bg)] text-[var(--nm-text)] focus:outline-none ${
                    validation.ageError ? 'ring-1 ring-red-500' : ''
                  }`}
                />
                {validation.ageError && (
                  <span className="text-[11px] text-red-600 dark:text-red-400">
                    {t.errAge}
                  </span>
                )}
              </label>

              <div className="grid grid-cols-2 gap-1 p-1 h-10 rounded-full bg-[var(--nm-bg)]">
                {(['MALE', 'FEMALE'] as Gender[]).map((g) => {
                  const active = profile.gender === g;
                  return (
                    <button
                      key={g}
                      type="button"
                      onClick={() =>
                        autoPersist(weightStr, heightStr, ageStr, { gender: g })
                      }
                      className={`rounded-full text-xs font-medium transition-colors cursor-pointer ${
                        active
                          ? 'bg-[var(--nm-pastel-teal)] text-[var(--nm-text)]'
                          : 'text-[var(--nm-muted)]'
                      }`}
                    >
                      {g === 'MALE' ? t.male : t.female}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="text-[11px] tabular-nums text-[var(--nm-muted)]">
              BMI {targets.bmi.toFixed(1)} ·{' '}
              {bmiCategoryText(targets.bmiCategory, profile.language)}
            </div>
          </div>
        )}
      </PastelCard>

      {/* 2. Goal collapsible white card */}
      <PastelCard>
        <button
          type="button"
          data-testid="section-goal"
          onClick={() => toggleSection('goal')}
          className="w-full flex items-center justify-between text-left cursor-pointer"
        >
          <div>
            <div className="text-sm font-medium text-[var(--nm-text)]">
              {t.sectionGoal}
            </div>
            {expanded !== 'goal' && (
              <div className="text-[11px] text-[var(--nm-muted)] mt-0.5">
                {goalText(profile.goal, profile.language)} ·{' '}
                {activityText(profile.activityLevel, profile.language)}
              </div>
            )}
          </div>
          {expanded === 'goal' ? (
            <ChevronUp className="w-4 h-4 text-[var(--nm-muted)]" />
          ) : (
            <ChevronDown className="w-4 h-4 text-[var(--nm-muted)]" />
          )}
        </button>

        {expanded === 'goal' && (
          <div className="pt-4 space-y-3">
            <div className="grid grid-cols-3 gap-1 p-1 rounded-full bg-[var(--nm-bg)]">
              {GOALS.map((g) => {
                const active = profile.goal === g;
                return (
                  <button
                    key={g}
                    type="button"
                    onClick={() =>
                      autoPersist(weightStr, heightStr, ageStr, { goal: g })
                    }
                    className={`py-2 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                      active
                        ? 'bg-[var(--nm-pastel-teal)] text-[var(--nm-text)]'
                        : 'text-[var(--nm-muted)]'
                    }`}
                  >
                    {goalText(g, profile.language)}
                  </button>
                );
              })}
            </div>

            <div className="space-y-1.5">
              {ACTIVITY_LEVELS.map((level) => {
                const active = profile.activityLevel === level;
                return (
                  <button
                    key={level}
                    type="button"
                    onClick={() =>
                      autoPersist(weightStr, heightStr, ageStr, {
                        activityLevel: level,
                      })
                    }
                    className={`w-full px-3.5 py-2.5 rounded-full flex items-center justify-between text-xs font-medium transition-colors cursor-pointer ${
                      active
                        ? 'bg-[var(--nm-pastel-teal)] text-[var(--nm-text)]'
                        : 'bg-[var(--nm-bg)] text-[var(--nm-text)]'
                    }`}
                  >
                    <span>{activityText(level, profile.language)}</span>
                    <span className="tabular-nums opacity-70">
                      ×{ACTIVITY_FACTORS[level]}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        )}
      </PastelCard>

      {/* 3. App collapsible white card */}
      <PastelCard>
        <button
          type="button"
          data-testid="section-app"
          onClick={() => toggleSection('app')}
          className="w-full flex items-center justify-between text-left cursor-pointer"
        >
          <div>
            <div className="text-sm font-medium text-[var(--nm-text)]">
              {t.sectionApp}
            </div>
            {expanded !== 'app' && (
              <div className="text-[11px] text-[var(--nm-muted)] mt-0.5">
                {profile.language === 'MM' ? t.langMm : t.langEn} ·{' '}
                {themeModeText(themeMode, profile.language)}
              </div>
            )}
          </div>
          {expanded === 'app' ? (
            <ChevronUp className="w-4 h-4 text-[var(--nm-muted)]" />
          ) : (
            <ChevronDown className="w-4 h-4 text-[var(--nm-muted)]" />
          )}
        </button>

        {expanded === 'app' && (
          <div className="pt-4 space-y-3">
            {/* Language EN / မြန်မာ */}
            <div className="grid grid-cols-2 gap-1 p-1 rounded-full bg-[var(--nm-bg)]">
              {(['EN', 'MM'] as AppLanguage[]).map((lang) => {
                const active = profile.language === lang;
                return (
                  <button
                    key={lang}
                    type="button"
                    data-testid={`lang-${lang.toLowerCase()}`}
                    onClick={() =>
                      onUpdateProfile({ ...profile, language: lang })
                    }
                    className={`py-2 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                      active
                        ? 'bg-[var(--nm-pastel-teal)] text-[var(--nm-text)]'
                        : 'text-[var(--nm-muted)]'
                    }`}
                  >
                    {lang === 'EN' ? t.langEn : t.langMm}
                  </button>
                );
              })}
            </div>

            {/* Theme System / Light / Dark */}
            <div className="grid grid-cols-3 gap-1 p-1 rounded-full bg-[var(--nm-bg)]">
              {THEMES.map((mode) => {
                const active = themeMode === mode;
                return (
                  <button
                    key={mode}
                    type="button"
                    data-testid={`theme-${mode.toLowerCase()}`}
                    onClick={() => onChangeThemeMode(mode)}
                    className={`py-2 rounded-full text-xs font-medium transition-colors cursor-pointer ${
                      active
                        ? 'bg-[var(--nm-pastel-teal)] text-[var(--nm-text)]'
                        : 'text-[var(--nm-muted)]'
                    }`}
                  >
                    {themeModeText(mode, profile.language)}
                  </button>
                );
              })}
            </div>

            {/* Reminders nested collapsible pastel card */}
            <div className="rounded-[22px] bg-[var(--nm-bg)] p-3.5 space-y-3">
              <button
                type="button"
                data-testid="reminders-toggle"
                onClick={() => setRemindersOpen((v) => !v)}
                className="w-full flex items-center justify-between text-left cursor-pointer"
              >
                <div className="flex items-center gap-2">
                  <Bell className="w-4 h-4 text-[var(--nm-text)]" />
                  <div>
                    <div className="text-xs font-medium text-[var(--nm-text)]">
                      {profile.language === 'MM' ? 'အသိပေးချက်များ' : 'Reminders'}
                    </div>
                    {!remindersOpen && (
                      <div className="text-[11px] text-[var(--nm-muted)]">
                        {reminders.masterEnabled
                          ? profile.language === 'MM'
                            ? 'ဖွင့်ထားသည်'
                            : 'On'
                          : profile.language === 'MM'
                          ? 'ပိတ်ထားသည်'
                          : 'Off'}
                      </div>
                    )}
                  </div>
                </div>
                {remindersOpen ? (
                  <ChevronUp className="w-4 h-4 text-[var(--nm-muted)]" />
                ) : (
                  <ChevronDown className="w-4 h-4 text-[var(--nm-muted)]" />
                )}
              </button>

              {remindersOpen && (
                <div className="space-y-2.5 pt-1">
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-[var(--nm-text)]">
                      {profile.language === 'MM'
                        ? 'အစားအချိန် သတိပေးချက်'
                        : 'Meal reminders'}
                    </span>
                    <button
                      type="button"
                      role="switch"
                      aria-checked={reminders.masterEnabled}
                      data-testid="reminders-master-switch"
                      onClick={() =>
                        saveReminders({
                          ...reminders,
                          masterEnabled: !reminders.masterEnabled,
                        })
                      }
                      className={`w-11 h-6 rounded-full p-0.5 transition-colors cursor-pointer ${
                        reminders.masterEnabled
                          ? 'bg-[var(--nm-pastel-sage)]'
                          : 'bg-[var(--nm-surface-variant)]'
                      }`}
                    >
                      <span
                        className={`block w-5 h-5 rounded-full bg-[var(--nm-surface)] transition-transform ${
                          reminders.masterEnabled ? 'translate-x-5' : ''
                        }`}
                      />
                    </button>
                  </div>

                  <div className="space-y-2">
                    {reminders.slots.map((slot) => (
                      <div
                        key={slot.key}
                        className="rounded-[18px] bg-[var(--nm-surface)] px-3 py-2 flex items-center justify-between gap-2"
                      >
                        <span
                          className={`text-xs font-medium flex-1 ${
                            reminders.masterEnabled
                              ? 'text-[var(--nm-text)]'
                              : 'text-[var(--nm-muted)]'
                          }`}
                        >
                          {profile.language === 'MM' ? slot.labelMm : slot.labelEn}
                        </span>
                        <button
                          type="button"
                          disabled={!reminders.masterEnabled}
                          onClick={() => {
                            setEditingSlotKey(slot.key);
                            setTempTime(slot.time);
                          }}
                          className={`px-2.5 py-1 rounded-full text-[11px] font-medium tabular-nums cursor-pointer disabled:opacity-50 ${
                            reminders.masterEnabled && slot.enabled
                              ? 'bg-[var(--nm-pastel-teal)]/35 text-[var(--nm-text)]'
                              : 'bg-[var(--nm-surface-variant)] text-[var(--nm-muted)]'
                          }`}
                        >
                          {slot.time}
                        </button>
                        <button
                          type="button"
                          role="switch"
                          aria-checked={slot.enabled}
                          disabled={!reminders.masterEnabled}
                          onClick={() =>
                            saveReminders({
                              ...reminders,
                              slots: reminders.slots.map((s) =>
                                s.key === slot.key
                                  ? { ...s, enabled: !s.enabled }
                                  : s
                              ),
                            })
                          }
                          className={`w-9 h-5 rounded-full p-0.5 transition-colors cursor-pointer disabled:opacity-45 ${
                            slot.enabled
                              ? 'bg-[var(--nm-pastel-sage)]'
                              : 'bg-[var(--nm-surface-variant)]'
                          }`}
                        >
                          <span
                            className={`block w-4 h-4 rounded-full bg-[var(--nm-surface)] transition-transform ${
                              slot.enabled ? 'translate-x-4' : ''
                            }`}
                          />
                        </button>
                      </div>
                    ))}
                  </div>

                  <button
                    type="button"
                    data-testid="send-test-notification"
                    onClick={triggerTestNotification}
                    className="w-full h-10 rounded-full bg-[var(--nm-pastel-yellow)] text-xs font-medium text-[var(--nm-text)] cursor-pointer"
                  >
                    {profile.language === 'MM'
                      ? 'စမ်းသပ်အသိပေးချက် ပို့မည်'
                      : 'Send test notification'}
                  </button>

                  {testBanner && (
                    <div className="rounded-[18px] bg-[var(--nm-surface)] px-3.5 py-2.5 text-xs text-[var(--nm-text)] animate-[fadeInItem_180ms_ease-out]">
                      <div className="font-medium">NutriMuscle</div>
                      <div className="text-[var(--nm-muted)] mt-0.5">
                        {testBanner}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* JSON Backup Export / Import */}
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={handleExport}
                className="h-10 px-3 rounded-full bg-[var(--nm-bg)] text-xs font-medium text-[var(--nm-text)] inline-flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <Upload className="w-3.5 h-3.5" />
                <span>{t.exportJson}</span>
              </button>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="h-10 px-3 rounded-full bg-[var(--nm-bg)] text-xs font-medium text-[var(--nm-text)] inline-flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <Download className="w-3.5 h-3.5" />
                <span>{t.importJson}</span>
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="application/json"
                className="hidden"
                onChange={(e) => {
                  void handleImport(e.target.files?.[0] ?? null);
                  e.target.value = '';
                }}
              />
            </div>

            {/* Android Gradle Project browser & ZIP */}
            <button
              type="button"
              data-testid="open-android-project"
              onClick={onOpenAndroidProject}
              className="w-full h-10 px-3 rounded-full bg-[var(--nm-bg)] text-xs font-medium text-[var(--nm-text)] inline-flex items-center justify-center gap-2 cursor-pointer"
            >
              <FolderCode className="w-3.5 h-3.5 text-[var(--nm-muted)]" />
              <span>{t.androidProject}</span>
            </button>

            {backupStatus && (
              <div className="text-xs text-[var(--nm-text)]">
                {backupStatus}
              </div>
            )}

            {/* Disclaimer appears ONCE, inside Profile > App */}
            <div
              data-testid="estimates-only-note"
              className="pt-1 text-[11px] text-[var(--nm-muted)]"
            >
              {t.estimatesOnly}
            </div>
          </div>
        )}
      </PastelCard>

      {/* Styled 28px pastel Time Picker modal */}
      {editingSlotKey && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-5"
          onClick={() => setEditingSlotKey(null)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Reminder time"
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-xs rounded-[28px] bg-[var(--nm-surface)] p-5 space-y-4 animate-[fadeInItem_180ms_ease-out]"
          >
            <h3 className="text-base font-medium text-[var(--nm-text)]">
              {(() => {
                const slot = reminders.slots.find((s) => s.key === editingSlotKey);
                if (!slot) return '';
                return profile.language === 'MM' ? slot.labelMm : slot.labelEn;
              })()}
            </h3>
            <input
              type="time"
              value={tempTime}
              onChange={(e) => setTempTime(e.target.value)}
              className="w-full h-12 px-4 rounded-full bg-[var(--nm-bg)] text-base font-medium tabular-nums text-[var(--nm-text)] focus:outline-none"
            />
            <div className="flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setEditingSlotKey(null)}
                className="px-3.5 py-2 text-xs font-medium text-[var(--nm-muted)] cursor-pointer"
              >
                {t.cancel}
              </button>
              <button
                type="button"
                onClick={() => {
                  saveReminders({
                    ...reminders,
                    slots: reminders.slots.map((s) =>
                      s.key === editingSlotKey ? { ...s, time: tempTime } : s
                    ),
                  });
                  setEditingSlotKey(null);
                }}
                className="px-5 py-2 rounded-full bg-[var(--nm-pastel-yellow)] text-xs font-medium text-[var(--nm-text)] cursor-pointer"
              >
                {t.save}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
