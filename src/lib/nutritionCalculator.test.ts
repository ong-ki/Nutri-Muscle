import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateBmi,
  calculateBmr,
  calculateTargets,
  defaultMealTypeForHour,
  getBmiCategory,
  validateProfile,
} from './nutritionCalculator';

test('Mifflin-St Jeor BMR calculates accurately for male and female', () => {
  const maleBmr = calculateBmr(70, 175, 25, 'MALE');
  assert.ok(Math.abs(maleBmr - 1673.75) < 0.01);

  const femaleBmr = calculateBmr(55, 162, 28, 'FEMALE');
  assert.ok(Math.abs(femaleBmr - 1261.5) < 0.01);
});

test('TDEE, calorie target, protein, fat (25%), and carbs calculate accurately', () => {
  const targets = calculateTargets({
    weightKg: 70,
    heightCm: 175,
    age: 25,
    gender: 'MALE',
    activityLevel: 'MODERATE',
    goal: 'MUSCLE_GAIN',
    language: 'EN',
  });
  const expectedTdee = 1673.75 * 1.55;
  const expectedCal = expectedTdee + 300;
  const expectedProtein = 140;
  const expectedFat = (expectedCal * 0.25) / 9;
  const expectedCarbs = (expectedCal - expectedProtein * 4 - expectedFat * 9) / 4;

  assert.ok(Math.abs(targets.tdee - expectedTdee) < 0.01);
  assert.ok(Math.abs(targets.calorieTarget - expectedCal) < 0.01);
  assert.equal(targets.proteinGrams, expectedProtein);
  assert.ok(Math.abs(targets.fatGrams - expectedFat) < 0.01);
  assert.ok(Math.abs(targets.carbsGrams - expectedCarbs) < 0.01);
});

test('BMI and categories match standard thresholds', () => {
  const bmi = calculateBmi(70, 175);
  assert.ok(Math.abs(bmi - 22.857) < 0.01);
  assert.equal(getBmiCategory(17.9), 'UNDERWEIGHT');
  assert.equal(getBmiCategory(22.8), 'NORMAL');
  assert.equal(getBmiCategory(27.4), 'OVERWEIGHT');
  assert.equal(getBmiCategory(31.0), 'OBESE');
});

test('Profile validation enforces age 10-100, weight 20-300, height 100-250', () => {
  assert.equal(validateProfile(65, 170, 25).isValid, true);
  assert.equal(validateProfile(20, 100, 10).isValid, true);
  assert.equal(validateProfile(300, 250, 100).isValid, true);

  const badAge = validateProfile(65, 170, 9);
  assert.equal(badAge.isValid, false);
  assert.equal(badAge.ageError, true);

  const badWeight = validateProfile(19, 170, 25);
  assert.equal(badWeight.isValid, false);
  assert.equal(badWeight.weightError, true);

  const badHeight = validateProfile(65, 251, 25);
  assert.equal(badHeight.isValid, false);
  assert.equal(badHeight.heightError, true);
});

test('defaultMealTypeForHour maps time of day to Breakfast, Lunch, Dinner, or Snack', () => {
  assert.equal(defaultMealTypeForHour(7), 'BREAKFAST');
  assert.equal(defaultMealTypeForHour(12), 'LUNCH');
  assert.equal(defaultMealTypeForHour(19), 'DINNER');
  assert.equal(defaultMealTypeForHour(23), 'SNACK');
  assert.equal(defaultMealTypeForHour(2), 'SNACK');
});
