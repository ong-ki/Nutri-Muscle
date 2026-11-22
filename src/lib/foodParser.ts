import type { FoodItem, NutritionTotals, ParsedFoodCandidate } from './types';

const BURMESE_DIGITS: Record<string, string> = {
  '၀': '0',
  '၁': '1',
  '၂': '2',
  '၃': '3',
  '၄': '4',
  '၅': '5',
  '၆': '6',
  '၇': '7',
  '၈': '8',
  '၉': '9',
};

const NUMBER_WORD_MAP: Record<string, number> = {
  'တစ်ဝက်': 0.5,
  'တဝက်': 0.5,
  half: 0.5,
  'တစ်': 1,
  'နှစ်': 2,
  'သုံး': 3,
  'လေး': 4,
  'ငါး': 5,
  'ခြောက်': 6,
  'ခုနစ်': 7,
  'ရှစ်': 8,
  'ကိုး': 9,
  'တစ်ဆယ်': 10,
  one: 1,
  two: 2,
  three: 3,
  four: 4,
  five: 5,
};

const UNIT_ALIASES: Record<string, string> = {
  // Burmese units
  'ပန်းကန်လုံး': 'bowl',
  'ပန်းကန်': 'bowl',
  'ခွက်': 'cup',
  'လုံး': 'piece',
  'ခု': 'piece',
  'ချပ်': 'slice',
  'ထုပ်': 'pack',
  'ဘူး': 'can',
  'ပုလင်း': 'bottle',
  'ချောင်း': 'skewer',
  'ပက်': 'shot',
  'ထမင်းစားဇွန်း': 'spoon',
  'လက်ဖက်ရည်ဇွန်း': 'spoon',
  'ဇွန်း': 'spoon',
  'ပွဲ': 'serving',
  'ကီလိုဂရမ်': 'kg',
  'ကီလို': 'kg',
  'ဂရမ်': 'g',
  'မီလီလီတာ': 'ml',
  'မီလီ': 'ml',
  // English units
  bowl: 'bowl',
  bowls: 'bowl',
  plate: 'serving',
  plates: 'serving',
  cup: 'cup',
  cups: 'cup',
  glass: 'cup',
  glasses: 'cup',
  piece: 'piece',
  pieces: 'piece',
  pc: 'piece',
  pcs: 'piece',
  slice: 'slice',
  slices: 'slice',
  pack: 'pack',
  packs: 'pack',
  pkt: 'pack',
  packet: 'pack',
  packets: 'pack',
  bag: 'pack',
  bags: 'pack',
  box: 'pack',
  boxes: 'pack',
  can: 'can',
  cans: 'can',
  tin: 'can',
  tins: 'can',
  bottle: 'bottle',
  bottles: 'bottle',
  btl: 'bottle',
  skewer: 'skewer',
  skewers: 'skewer',
  stick: 'skewer',
  sticks: 'skewer',
  bar: 'bar',
  bars: 'bar',
  cone: 'cone',
  cones: 'cone',
  shot: 'shot',
  shots: 'shot',
  scoop: 'scoop',
  scoops: 'scoop',
  spoon: 'spoon',
  spoons: 'spoon',
  tbsp: 'spoon',
  tsp: 'spoon',
  serving: 'serving',
  servings: 'serving',
  g: 'g',
  gram: 'g',
  grams: 'g',
  kg: 'kg',
  kilogram: 'kg',
  ml: 'ml',
};

export function normalizeBurmeseDigits(input: string): string {
  let out = '';
  for (const ch of input) {
    out += BURMESE_DIGITS[ch] ?? ch;
  }
  return out;
}

/**
 * Normalizes Burmese Unicode & common Zawgyi-style input variants:
 * - Strips zero-width spaces / joiners
 * - Converts common Zawgyi glyphs (medial ra, u/uu, kinzi, aukmyit) to Unicode
 * - Reorders visual-order Zawgyi vowel-E (\u1031) and medial ra (\u103C) before consonants
 * - Unifies tall-aa (\u102B) with standard aa (\u102C)
 */
export function normalizeBurmeseScript(text: string): string {
  let s = text
    .replace(/[\u200B\u200C\u200D\uFEFF]/g, '')
    .replace(/[\u107E-\u1084]/g, '\u103C')
    .replace(/\u1033/g, '\u102F')
    .replace(/\u1034/g, '\u1030')
    .replace(/\u1064/g, '\u1004\u103A\u1039')
    .replace(/[\u1094\u1095]/g, '\u1037')
    .replace(/\u102B/g, '\u102C');

  s = s.replace(/\u1031([\u1000-\u1021])/g, '$1\u1031');
  s = s.replace(/\u103C([\u1000-\u1021])/g, '$1\u103C');
  return s;
}

export function parseNumberToken(token: string): number | null {
  const cleaned = normalizeBurmeseDigits(token).trim().toLowerCase();
  if (!cleaned) return null;
  if (cleaned in NUMBER_WORD_MAP) return NUMBER_WORD_MAP[cleaned];

  if (cleaned.includes('/')) {
    const parts = cleaned.split('/');
    if (parts.length === 2) {
      const num = Number(parts[0].trim());
      const den = Number(parts[1].trim());
      if (Number.isFinite(num) && Number.isFinite(den) && den !== 0) {
        return num / den;
      }
    }
  }
  const parsed = Number(cleaned);
  return Number.isFinite(parsed) ? parsed : null;
}

export function splitFoodSegments(input: string): string[] {
  if (!input.trim()) return [];
  const splitRegex = /[,+\n၊။]+|(?:\s+(?:and|နဲ့|နှင့်)\s+)|(?:နဲ့|နှင့်)/i;
  return input
    .split(splitRegex)
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}

function escapeRegex(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function normalizeUnit(raw: string): string {
  return UNIT_ALIASES[raw.toLowerCase()] ?? UNIT_ALIASES[raw] ?? 'serving';
}

export function normalizeFoodText(text: string): string {
  return normalizeBurmeseScript(text)
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\p{M}\s]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

export function levenshteinDistance(a: string, b: string): number {
  if (!a.length) return b.length;
  if (!b.length) return a.length;
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  let curr = new Array<number>(b.length + 1).fill(0);

  for (let i = 1; i <= a.length; i++) {
    curr[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      curr[j] = Math.min(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost);
    }
    const tmp = prev;
    prev = curr;
    curr = tmp;
  }
  return prev[b.length];
}

export function levenshteinSimilarity(a: string, b: string): number {
  if (a === b) return 1;
  const maxLen = Math.max(a.length, b.length);
  if (maxLen === 0) return 1;
  return 1 - levenshteinDistance(a, b) / maxLen;
}

function tokenOverlapSimilarity(a: string, b: string): number {
  const tokensA = new Set(a.split(' ').filter((t) => t.length >= 2));
  const tokensB = new Set(b.split(' ').filter((t) => t.length >= 2));
  if (tokensA.size === 0 || tokensB.size === 0) return 0;
  let intersection = 0;
  for (const t of tokensA) {
    if (tokensB.has(t)) intersection++;
  }
  if (intersection === 0) return 0;
  const union = new Set([...tokensA, ...tokensB]).size;
  return (intersection / union) * 0.85;
}

function computeUsageBoost(food: FoodItem): number {
  let boost = 0;
  if (food.usageCount && food.usageCount > 0) {
    boost += Math.min(0.08, Math.log(food.usageCount + 1) * 0.03);
  }
  if (food.lastUsedAtMillis && food.lastUsedAtMillis > 0) {
    boost += 0.03;
  }
  if (food.isFavorite) {
    boost += 0.04;
  }
  return boost;
}

function scoreCandidate(
  q: string,
  candidate: string,
  isPrimary: boolean
): number {
  const tierOffset = isPrimary ? 0.08 : 0;
  if (candidate === q) return 1.15 + tierOffset;

  // Prefix match works from 1-2 characters; reward tight first-word prefix match
  if (candidate.startsWith(q)) {
    const firstWord = candidate.split(' ')[0] || candidate;
    const ratio = Math.max(
      q.length / candidate.length,
      (q.length / firstWord.length) * 0.95
    );
    return 0.92 + tierOffset + 0.12 * ratio;
  }

  // Word-prefix match inside candidate
  const words = candidate.split(' ');
  if (words.some((w) => w.startsWith(q))) {
    const lengthRatio = q.length / candidate.length;
    return 0.82 + tierOffset * 0.6 + 0.1 * lengthRatio;
  }

  if (q.length <= 2) {
    if (q.length === 2 && candidate.includes(q)) {
      return 0.62 + tierOffset * 0.5;
    }
    return 0;
  }

  if (q.startsWith(candidate) && candidate.length >= 2) {
    return 0.84 + 0.1 * (candidate.length / q.length);
  }
  if (candidate.includes(q)) {
    return 0.74 + tierOffset * 0.5 + 0.12 * (q.length / candidate.length);
  }
  if (q.includes(candidate) && candidate.length >= 3) {
    return 0.7 + 0.12 * (candidate.length / q.length);
  }

  const levSim = levenshteinSimilarity(q, candidate);
  const tokenSim = tokenOverlapSimilarity(q, candidate);
  let bestWordLev = 0;
  for (const w of words) {
    if (w.length >= 3) {
      const s = levenshteinSimilarity(q, w) * 0.88;
      if (s > bestWordLev) bestWordLev = s;
    }
  }
  return Math.max(levSim, tokenSim, bestWordLev);
}

function scoreFoodForSearch(normalizedQuery: string, food: FoodItem): number {
  const primaryNames = [food.nameEn, food.nameMm]
    .map(normalizeFoodText)
    .filter((s) => s.length > 0);
  const aliasNames = food.aliases
    .map(normalizeFoodText)
    .filter((s) => s.length > 0);

  let maxScore = 0;
  for (const cand of primaryNames) {
    const s = scoreCandidate(normalizedQuery, cand, true);
    if (s > maxScore) maxScore = s;
  }
  for (const cand of aliasNames) {
    const s = scoreCandidate(normalizedQuery, cand, false);
    if (s > maxScore) maxScore = s;
  }
  return maxScore;
}

function findBestMatch(
  query: string,
  database: FoodItem[]
): { food: FoodItem; score: number } | null {
  const normalizedQuery = normalizeFoodText(query);
  if (!normalizedQuery || database.length === 0) return null;

  let bestFood: FoodItem | null = null;
  let bestScore = 0;
  for (const food of database) {
    const base = scoreFoodForSearch(normalizedQuery, food);
    const total = base + computeUsageBoost(food) * 0.4;
    if (total > bestScore) {
      bestScore = total;
      bestFood = food;
    }
  }
  if (bestFood && bestScore >= 0.52) {
    return { food: bestFood, score: bestScore };
  }
  return null;
}

export function computeCandidateGrams(candidate: ParsedFoodCandidate): number {
  const food = candidate.matchedFood;
  if (!food) return 0;
  const u = candidate.unit.toLowerCase();
  if (u === 'kg') return candidate.quantity * 1000;
  if (u === 'g') return candidate.quantity;
  if (u === 'ml') return candidate.quantity * (food.unitGrams.ml ?? 1);
  const perUnit = food.unitGrams[u] ?? food.defaultServingGrams;
  return candidate.quantity * perUnit;
}

export function computeCandidateNutrition(
  candidate: ParsedFoodCandidate
): NutritionTotals {
  const food = candidate.matchedFood;
  if (!food) {
    return { calories: 0, protein: 0, carbs: 0, fat: 0, fiber: 0, sugar: 0 };
  }
  const u = candidate.unit.toLowerCase();
  if (
    (u === food.defaultUnit.toLowerCase() || u === 'serving') &&
    food.caloriesPerServing > 0
  ) {
    return {
      calories: food.caloriesPerServing * candidate.quantity,
      protein: food.proteinPerServing * candidate.quantity,
      carbs: food.carbsPerServing * candidate.quantity,
      fat: food.fatPerServing * candidate.quantity,
      fiber: food.fiberPerServing * candidate.quantity,
      sugar: food.sugarPerServing * candidate.quantity,
    };
  }
  const factor = computeCandidateGrams(candidate) / 100;
  return {
    calories: food.caloriesPer100g * factor,
    protein: food.proteinPer100g * factor,
    carbs: food.carbsPer100g * factor,
    fat: food.fatPer100g * factor,
    fiber: food.fiberPer100g * factor,
    sugar: (food.sugarPer100g ?? 0) * factor,
  };
}

let candidateSeq = 0;

export function parseFoodSegment(
  segment: string,
  database: FoodItem[]
): ParsedFoodCandidate {
  let working = normalizeBurmeseScript(normalizeBurmeseDigits(segment)).trim();
  let extractedQuantity: number | null = null;
  let extractedUnit: string | null = null;

  const attachedRegex =
    /(?:^|\s)(\d+(?:\.\d+)?|\d+\/\d+)\s*(kg|kilogram|g|gram|grams|ml|cups?|glasses?|bowls?|plates?|pieces?|pcs?|slices?|packs?|packets?|pkt|bags?|boxes?|cans?|tins?|bottles?|btl|skewers?|sticks?|bars?|cones?|shots?|scoops?|spoons?|tbsp|tsp|servings?|ပန်းကန်လုံး|ပန်းကန်|ခွက်|လုံး|ခု|ချပ်|ထုပ်|ဘူး|ပုလင်း|ချောင်း|ပက်|ထမင်းစားဇွန်း|လက်ဖက်ရည်ဇွန်း|ဇွန်း|ပွဲ|ကီလိုဂရမ်|ကီလို|ဂရမ်|မီလီလီတာ|မီလီ)(?=\s|$)/i;
  const attachedMatch = working.match(attachedRegex);
  if (attachedMatch && attachedMatch.index !== undefined) {
    extractedQuantity = parseNumberToken(attachedMatch[1]);
    extractedUnit = normalizeUnit(attachedMatch[2]);
    working = (
      working.slice(0, attachedMatch.index) +
      ' ' +
      working.slice(attachedMatch.index + attachedMatch[0].length)
    ).trim();
  }

  if (extractedQuantity === null) {
    const wordRegex =
      /(?:^|\s)(တစ်ဝက်|တဝက်|half|တစ်ဆယ်|တစ်|နှစ်|သုံး|လေး|ငါး|ခြောက်|ခုနစ်|ရှစ်|ကိုး|one|two|three|four|five)(?=\s|$)/i;
    const wordMatch = working.match(wordRegex);
    if (wordMatch && wordMatch.index !== undefined) {
      const qty = parseNumberToken(wordMatch[1]);
      if (qty !== null) {
        extractedQuantity = qty;
        working = (
          working.slice(0, wordMatch.index) +
          ' ' +
          working.slice(wordMatch.index + wordMatch[0].length)
        ).trim();
      }
    }
  }

  if (extractedQuantity === null) {
    const qtyRegex = /(?:^|\s)(\d+\/\d+|\d+(?:\.\d+)?)(?=\s|$)/;
    const qtyMatch = working.match(qtyRegex);
    if (qtyMatch && qtyMatch.index !== undefined) {
      extractedQuantity = parseNumberToken(qtyMatch[1]);
      working = (
        working.slice(0, qtyMatch.index) +
        ' ' +
        working.slice(qtyMatch.index + qtyMatch[0].length)
      ).trim();
    }
  }

  if (extractedUnit === null) {
    const sortedUnits = Object.keys(UNIT_ALIASES).sort(
      (a, b) => b.length - a.length
    );
    for (const uKey of sortedUnits) {
      const isAscii = /^[\x00-\x7F]+$/.test(uKey);
      const pattern = isAscii
        ? new RegExp(`\\b${escapeRegex(uKey)}\\b`, 'i')
        : new RegExp(`(?:^|\\s)${escapeRegex(uKey)}(?=\\s|$)`);
      const m = working.match(pattern);
      if (m && m.index !== undefined) {
        extractedUnit = UNIT_ALIASES[uKey];
        working = (
          working.slice(0, m.index) +
          ' ' +
          working.slice(m.index + m[0].length)
        ).trim();
        break;
      }
    }
  }

  const cleanQuery = working
    .replace(/\s+/g, ' ')
    .replace(/^of\s+/i, '')
    .trim();

  const match = findBestMatch(cleanQuery, database);
  const matchedFood = match?.food ?? null;

  const finalQuantity = extractedQuantity ?? matchedFood?.lastUsedQuantity ?? 1;
  const finalUnit =
    extractedUnit ??
    matchedFood?.lastUsedUnit ??
    matchedFood?.defaultUnit ??
    'serving';

  candidateSeq += 1;
  return {
    id: `cand_${Date.now()}_${candidateSeq}`,
    rawSegment: segment,
    queryName: cleanQuery || segment,
    quantity: finalQuantity,
    unit: finalUnit,
    matchedFood,
    matchScore: match?.score ?? 0,
    isMatched: matchedFood !== null,
  };
}

export function parseFoodInput(
  input: string,
  database: FoodItem[]
): ParsedFoodCandidate[] {
  return splitFoodSegments(input).map((seg) => parseFoodSegment(seg, database));
}

export function suggestFoods(
  currentInput: string,
  database: FoodItem[],
  limit = 6
): FoodItem[] {
  const segments = splitFoodSegments(currentInput);
  const lastSeg = segments[segments.length - 1] ?? currentInput.trim();
  if (!lastSeg) return [];

  let stripped = normalizeBurmeseScript(normalizeBurmeseDigits(lastSeg)).replace(
    /\b\d+(?:\.\d+)?\b|\d+\/\d+/g,
    ' '
  );
  for (const uKey of Object.keys(UNIT_ALIASES).sort(
    (a, b) => b.length - a.length
  )) {
    const isAscii = /^[\x00-\x7F]+$/.test(uKey);
    stripped = isAscii
      ? stripped.replace(new RegExp(`\\b${escapeRegex(uKey)}\\b`, 'gi'), ' ')
      : stripped.replaceAll(uKey, ' ');
  }
  const q = normalizeFoodText(stripped.trim() || lastSeg);
  if (!q) return [];

  const minThreshold = q.length <= 2 ? 0.55 : 0.34;

  return database
    .map((food) => {
      const base = scoreFoodForSearch(q, food);
      return {
        food,
        base,
        score: base + computeUsageBoost(food),
      };
    })
    .filter((x) => x.base >= minThreshold)
    .sort((a, b) => b.score - a.score)
    .slice(0, limit)
    .map((x) => x.food);
}
