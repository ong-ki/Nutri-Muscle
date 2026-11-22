import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {
  computeCandidateGrams,
  computeCandidateNutrition,
  normalizeBurmeseDigits,
  normalizeBurmeseScript,
  parseFoodInput,
  parseNumberToken,
  suggestFoods,
} from './foodParser';
import { mapRawJsonFood, type RawAssetFood } from './storage';
import type { FoodItem } from './types';

const CATEGORY_FILES = [
  'myanmar_meals.json',
  'myanmar_snacks.json',
  'street_food.json',
  'food_court.json',
  'packaged_99.json',
  'drinks.json',
  'fruits.json',
  'global_foods.json',
];

const assetsDir = path.resolve(process.cwd(), 'app/src/main/assets');
const rawEntries: RawAssetFood[] = CATEGORY_FILES.flatMap((file) =>
  JSON.parse(fs.readFileSync(path.join(assetsDir, file), 'utf-8')) as RawAssetFood[]
);
const allFoods: FoodItem[] = rawEntries.map(mapRawJsonFood);

test('8 category JSON files ship 600+ distinct items with complete schema', () => {
  assert.ok(
    allFoods.length >= 600,
    `Expected >= 600 foods across 8 category files, got ${allFoods.length}`
  );
  const ids = new Set(allFoods.map((f) => f.id));
  assert.equal(ids.size, allFoods.length, 'Duplicate food IDs found');

  const categories = new Set(allFoods.map((f) => f.category));
  assert.equal(categories.size, 8);

  const namesMm = new Set(allFoods.map((f) => f.nameMm));
  assert.ok(namesMm.has('ထမင်း'));
  assert.ok(namesMm.has('မုန့်ဟင်းခါး'));
  assert.ok(namesMm.has('ရှမ်းခေါက်ဆွဲ'));
  assert.ok(namesMm.has('လက်ဖက်သုပ်'));
  assert.ok(namesMm.has('ကြက်သားဟင်း'));

  // Verify packaged items carry label values (is_estimate = false) and dishes carry is_estimate = true
  const mama = allFoods.find((f) => f.id === 'pk_mama_pork');
  assert.ok(mama, 'Expected pk_mama_pork in packaged_99.json');
  assert.equal(mama.isEstimate, false);
  assert.ok(mama.caloriesPerServing > 0 && mama.caloriesPer100g > 0);

  const friedRice = allFoods.find((f) => f.id === 'mm_fried_rice');
  assert.ok(friedRice, 'Expected mm_fried_rice in myanmar_meals.json');
  assert.equal(friedRice.isEstimate, true);
});

test('Burmese digits, fractions, and Zawgyi-style script normalize accurately', () => {
  assert.equal(normalizeBurmeseDigits('၂၅'), '25');
  assert.equal(parseNumberToken('တစ်ဝက်'), 0.5);
  assert.equal(parseNumberToken('1/2'), 0.5);
  assert.equal(parseNumberToken('၂.၅'), 2.5);
  // Zero-width space and tall-aa normalization
  assert.equal(normalizeBurmeseScript('မာ\u200Bမာ'), 'မာမာ');
  // Zawgyi visual-order vowel-E (\u1031) before consonant -> Unicode order
  assert.equal(normalizeBurmeseScript('\u1031က'), 'က\u1031');
});

test('Parses Burmese food text with Burmese digits, units, and default serving', () => {
  const results = parseFoodInput(
    'ထမင်း ၂ ပန်းကန်၊ ကြက်ဥ ၂ လုံး၊ ကြက်သားဟင်း',
    allFoods
  );
  assert.equal(results.length, 3);

  assert.equal(results[0].matchedFood?.id, 'mm_steamed_rice');
  assert.equal(results[0].quantity, 2);
  assert.equal(results[0].unit, 'bowl');
  assert.equal(computeCandidateGrams(results[0]), 400);
  assert.equal(Math.round(computeCandidateNutrition(results[0]).calories), 520);

  assert.equal(results[1].matchedFood?.nameMm, 'ကြက်ဥပြုတ်');
  assert.equal(results[1].quantity, 2);
  assert.equal(results[1].unit, 'piece');

  assert.equal(results[2].matchedFood?.id, 'mm_chicken_curry');
  assert.equal(results[2].quantity, 1);
  assert.equal(results[2].unit, 'serving');
});

test('Parses mixed packaged, drink, and Myanmar meal sentence with new units', () => {
  const results = parseFoodInput(
    'မာမာ ၁ ထုပ်၊ coke ၁ ဘူး၊ ထမင်းကြော် ၁ ပန်းကန်',
    allFoods
  );
  assert.equal(results.length, 3);

  assert.equal(results[0].matchedFood?.id, 'pk_mama_pork');
  assert.equal(results[0].quantity, 1);
  assert.equal(results[0].unit, 'pack');
  assert.equal(results[0].matchedFood?.isEstimate, false);

  assert.equal(results[1].matchedFood?.id, 'dr_coke_can');
  assert.equal(results[1].quantity, 1);
  assert.equal(results[1].unit, 'can');
  assert.equal(results[1].matchedFood?.isEstimate, false);

  assert.equal(results[2].matchedFood?.id, 'mm_fried_rice');
  assert.equal(results[2].quantity, 1);
  assert.equal(results[2].unit, 'bowl');
  assert.equal(results[2].matchedFood?.isEstimate, true);
});

test('Parses English mixed text with fractions, grams, and unmatched fallback', () => {
  const results = parseFoodInput(
    '2 eggs, 1/2 cup rice + 200g chicken breast and xyzunknownfood99',
    allFoods
  );
  assert.equal(results.length, 4);
  assert.equal(results[0].quantity, 2);
  assert.equal(results[1].matchedFood?.id, 'mm_steamed_rice');
  assert.equal(results[1].quantity, 0.5);
  assert.equal(results[1].unit, 'cup');
  assert.equal(computeCandidateGrams(results[1]), 90);

  assert.equal(results[2].matchedFood?.id, 'gl_chicken_breast');
  assert.equal(results[2].quantity, 200);
  assert.equal(results[2].unit, 'g');

  assert.equal(results[3].isMatched, false);
  assert.equal(results[3].matchedFood, null);
});

test('Live autocomplete triggers on 1-2 characters and ranks exact/prefix/alias/frequent', () => {
  const cokeSuggestions = suggestFoods('co', allFoods, 6);
  assert.ok(
    cokeSuggestions.some((f) => f.id === 'dr_coke_can'),
    `Expected Coca-Cola in 'co' suggestions, got ${cokeSuggestions.map((f) => f.id).join(', ')}`
  );

  const mamaSuggestions = suggestFoods('မာ', allFoods, 6);
  assert.ok(
    mamaSuggestions.some((f) => f.id.startsWith('pk_mama')),
    `Expected Mama in 'မာ' suggestions, got ${mamaSuggestions.map((f) => f.id).join(', ')}`
  );

  const mmSuggestions = suggestFoods('မုန့်ဟင်း', allFoods);
  assert.ok(mmSuggestions.some((f) => f.id === 'mm_mohinga'));

  const enSuggestions = suggestFoods('2 eggs, salm', allFoods);
  assert.ok(enSuggestions.some((f) => f.id === 'gl_salmon'));
});

test('Remembers last-used serving when quantity and unit are omitted', () => {
  const withUsage = allFoods.map((f) =>
    f.id === 'mm_boiled_egg'
      ? { ...f, lastUsedQuantity: 3, lastUsedUnit: 'piece', usageCount: 10 }
      : f
  );
  const res = parseFoodInput('ကြက်ဥပြုတ်', withUsage);
  assert.equal(res.length, 1);
  assert.equal(res[0].matchedFood?.id, 'mm_boiled_egg');
  assert.equal(res[0].quantity, 3);
  assert.equal(res[0].unit, 'piece');
});
