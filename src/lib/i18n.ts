import type {
  ActivityLevel,
  AppLanguage,
  BmiCategory,
  FitnessGoal,
  InsightTip,
  MealType,
  ThemeMode,
} from './types';

export const UI_STRINGS = {
  EN: {
    navToday: 'Today',
    navHistory: 'History',
    navProfile: 'Profile',
    estimatesOnly: 'Estimates only',
    calShort: 'Cal',
    protein: 'Protein',
    carbs: 'Carbs',
    fat: 'Fat',
    muscleChip: (score: number) => `Muscle ${score}`,
    muscleScore: 'Muscle Score',
    proteinPart: 'Protein',
    caloriesPart: 'Calories',
    spreadPart: 'Spread',
    suggestions: 'Suggestions',
    edit: 'Edit',
    delete: 'Delete',
    foodInputPlaceholder: 'ဘာစားခဲ့လဲ?',
    addWithTotal: (kcal: string) => `Add · ${kcal} kcal`,
    updateWithTotal: (kcal: string) => `Update · ${kcal} kcal`,
    addEmpty: 'Add',
    save: 'Save',
    recent: 'Recent',
    favorites: 'Favorites',
    customFood: 'Custom food',
    editFood: 'Edit food',
    nameLabel: 'Name',
    servingG: 'Serving (g/ml)',
    kcal: 'kcal',
    pShort: 'P (g)',
    cShort: 'C (g)',
    fShort: 'F (g)',
    sugarShort: 'Sugar (g)',
    cancel: 'Cancel',
    range7: '7D',
    range30: '30D',
    noMealsDay: 'No meals',
    sectionBody: 'Body',
    sectionGoal: 'Goal',
    sectionApp: 'App',
    weightKg: 'Weight (kg)',
    heightCm: 'Height (cm)',
    age: 'Age',
    male: 'Male',
    female: 'Female',
    langEn: 'EN',
    langMm: 'မြန်မာ',
    exportJson: 'Export',
    importJson: 'Import',
    exportedMsg: 'Exported',
    importedMsg: 'Imported',
    backupErrorMsg: 'Failed',
    androidProject: 'Android Project',
    downloadZip: 'Gradle ZIP',
    androidSubtitle: 'Offline Kotlin + Jetpack Compose + Material 3 codebase',
    errAge: '10–100',
    errWeight: '20–300 kg',
    errHeight: '100–250 cm',
  },
  MM: {
    navToday: 'ယနေ့',
    navHistory: 'မှတ်တမ်း',
    navProfile: 'ကိုယ်ရေး',
    estimatesOnly: 'ခန့်မှန်းချက်သာ',
    calShort: 'ကယ်လိုရီ',
    protein: 'ပရိုတင်း',
    carbs: 'ကာဗို',
    fat: 'အဆီ',
    muscleChip: (score: number) => `ကြွက်သား ${score}`,
    muscleScore: 'ကြွက်သားရမှတ်',
    proteinPart: 'ပရိုတင်း',
    caloriesPart: 'ကယ်လိုရီ',
    spreadPart: 'ခွဲဝေမှု',
    suggestions: 'အကြံပြု',
    edit: 'ပြင်',
    delete: 'ဖျက်',
    foodInputPlaceholder: 'ဘာစားခဲ့လဲ?',
    addWithTotal: (kcal: string) => `ထည့် · ${kcal} kcal`,
    updateWithTotal: (kcal: string) => `ပြင် · ${kcal} kcal`,
    addEmpty: 'ထည့်',
    save: 'သိမ်း',
    recent: 'မကြာသေးမီ',
    favorites: 'အကြိုက်ဆုံး',
    customFood: 'အစားသစ်',
    editFood: 'အစားပြင်',
    nameLabel: 'အမည်',
    servingG: 'ပမာဏ (g/ml)',
    kcal: 'kcal',
    pShort: 'ပရို (g)',
    cShort: 'ကာဗို (g)',
    fShort: 'အဆီ (g)',
    sugarShort: 'သကြား (g)',
    cancel: 'ပယ်ဖျက်',
    range7: '၇ ရက်',
    range30: '၃၀ ရက်',
    noMealsDay: 'မှတ်တမ်းမရှိ',
    sectionBody: 'ကိုယ်ခန္ဓာ',
    sectionGoal: 'ပန်းတိုင်',
    sectionApp: 'အက်ပ်',
    weightKg: 'အလေးချိန် (kg)',
    heightCm: 'အရပ် (cm)',
    age: 'အသက်',
    male: 'ကျား',
    female: 'မ',
    langEn: 'EN',
    langMm: 'မြန်မာ',
    exportJson: 'ထုတ်',
    importJson: 'သွင်း',
    exportedMsg: 'ထုတ်ပြီး',
    importedMsg: 'သွင်းပြီး',
    backupErrorMsg: 'မအောင်မြင်ပါ',
    androidProject: 'Android ကုဒ်',
    downloadZip: 'Gradle ZIP',
    androidSubtitle: 'အော့ဖ်လိုင်း Kotlin + Jetpack Compose + Material 3',
    errAge: '၁၀–၁၀၀',
    errWeight: '၂၀–၃၀၀ kg',
    errHeight: '၁၀၀–၂၅၀ cm',
  },
} as const;

export function mealTypeText(type: MealType, lang: AppLanguage): string {
  const map: Record<AppLanguage, Record<MealType, string>> = {
    EN: { BREAKFAST: 'Breakfast', LUNCH: 'Lunch', DINNER: 'Dinner', SNACK: 'Snack' },
    MM: { BREAKFAST: 'မနက်စာ', LUNCH: 'နေ့လယ်စာ', DINNER: 'ညစာ', SNACK: 'သရေစာ' },
  };
  return map[lang][type];
}

export function unitLabel(unit: string, lang: AppLanguage): string {
  if (lang === 'EN') return unit;
  const mmMap: Record<string, string> = {
    bowl: 'ပန်းကန်',
    cup: 'ခွက်',
    piece: 'လုံး',
    slice: 'ချပ်',
    pack: 'ထုပ်',
    can: 'ဘူး',
    bottle: 'ပုလင်း',
    skewer: 'ချောင်း',
    shot: 'ပက်',
    bar: 'ခု',
    cone: 'ခု',
    scoop: 'ဇွန်း',
    spoon: 'ဇွန်း',
    serving: 'ပွဲ',
    g: 'ဂရမ်',
    kg: 'ကီလို',
    ml: 'မီလီ',
  };
  return mmMap[unit.toLowerCase()] ?? unit;
}

export function activityText(level: ActivityLevel, lang: AppLanguage): string {
  const map: Record<AppLanguage, Record<ActivityLevel, string>> = {
    EN: {
      SEDENTARY: 'Sedentary',
      LIGHT: 'Light',
      MODERATE: 'Moderate',
      ACTIVE: 'Active',
      VERY_ACTIVE: 'Very Active',
    },
    MM: {
      SEDENTARY: 'အနည်း',
      LIGHT: 'ပေါ့ပါး',
      MODERATE: 'အလယ်',
      ACTIVE: 'တက်ကြွ',
      VERY_ACTIVE: 'အများဆုံး',
    },
  };
  return map[lang][level];
}

export function goalText(goal: FitnessGoal, lang: AppLanguage): string {
  const map: Record<AppLanguage, Record<FitnessGoal, string>> = {
    EN: {
      MUSCLE_GAIN: 'Muscle',
      FAT_LOSS: 'Cut',
      MAINTAIN: 'Maintain',
    },
    MM: {
      MUSCLE_GAIN: 'ကြွက်သား',
      FAT_LOSS: 'အဆီချ',
      MAINTAIN: 'ထိန်း',
    },
  };
  return map[lang][goal];
}

export function themeModeText(mode: ThemeMode, lang: AppLanguage): string {
  const map: Record<AppLanguage, Record<ThemeMode, string>> = {
    EN: { SYSTEM: 'System', LIGHT: 'Light', DARK: 'Dark' },
    MM: { SYSTEM: 'စနစ်', LIGHT: 'အလင်း', DARK: 'အမှောင်' },
  };
  return map[lang][mode];
}

export function bmiCategoryText(cat: BmiCategory, lang: AppLanguage): string {
  const map: Record<AppLanguage, Record<BmiCategory, string>> = {
    EN: {
      UNDERWEIGHT: 'Under',
      NORMAL: 'Normal',
      OVERWEIGHT: 'Over',
      OBESE: 'Obese',
    },
    MM: {
      UNDERWEIGHT: 'နည်း',
      NORMAL: 'ပုံမှန်',
      OVERWEIGHT: 'ပို',
      OBESE: 'အဝလွန်',
    },
  };
  return map[lang][cat];
}

export function formatInsightTip(tip: InsightTip, lang: AppLanguage): string {
  if (lang === 'MM') {
    switch (tip.type) {
      case 'protein_needed':
        return `ပရိုတင်း ${tip.gramsRemaining} g ထပ်လို`;
      case 'protein_hit':
        return 'ပရိုတင်း ပြည့်မီပြီ';
      case 'calories_over':
        return `ကယ်လိုရီ ${tip.kcalOver} kcal ကျော်`;
      case 'calories_under':
        return `ကယ်လိုရီ ${tip.kcalUnder} kcal လို`;
      case 'calories_on_track':
        return 'ကယ်လိုရီ ကိုက်ညီ';
      case 'spread_good':
        return `ပရိုတင်း ${tip.mealCount} ကြိမ် ခွဲစားပြီး`;
      case 'spread_improve':
        return 'ပရိုတင်း ၃ ကြိမ် ခွဲစားပါ (20g+)';
      case 'fiber_low':
        return `အမျှင်ဓာတ်နည်း (${tip.fiberGrams} g)`;
    }
  }
  switch (tip.type) {
    case 'protein_needed':
      return `${tip.gramsRemaining} g more protein needed`;
    case 'protein_hit':
      return 'Protein target reached';
    case 'calories_over':
      return `${tip.kcalOver} kcal over target`;
    case 'calories_under':
      return `${tip.kcalUnder} kcal under target`;
    case 'calories_on_track':
      return 'Calories on target';
    case 'spread_good':
      return `Protein spread across ${tip.mealCount} meals`;
    case 'spread_improve':
      return 'Aim for 20g+ protein in 3 meals';
    case 'fiber_low':
      return `Low fiber (${tip.fiberGrams} g)`;
  }
}
