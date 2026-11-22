package com.nutrimuscle.app

import com.nutrimuscle.app.domain.model.FoodItem
import com.nutrimuscle.app.domain.parser.BurmeseNumberParser
import com.nutrimuscle.app.domain.parser.FoodParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodParserTest {

    private val sampleFoods = listOf(
        FoodItem(
            id = "mm_steamed_rice",
            nameEn = "Steamed White Rice",
            nameMm = "ထမင်း",
            aliases = listOf("rice", "white rice", "htamin"),
            category = "myanmar_meals",
            servingLabelMm = "၁ ပန်းကန်",
            servingLabelEn = "1 bowl",
            defaultServingGrams = 200.0,
            caloriesPerServing = 260.0,
            proteinPerServing = 5.4,
            carbsPerServing = 56.4,
            fatPerServing = 0.6,
            caloriesPer100g = 130.0,
            proteinPer100g = 2.7,
            carbsPer100g = 28.2,
            fatPer100g = 0.3,
            defaultUnit = "bowl",
            unitGrams = mapOf("g" to 1.0, "bowl" to 200.0, "cup" to 160.0),
            isEstimate = true
        ),
        FoodItem(
            id = "mm_fried_rice",
            nameEn = "Myanmar Fried Rice",
            nameMm = "ထမင်းကြော်",
            aliases = listOf("fried rice", "htamin kyaw"),
            category = "myanmar_meals",
            servingLabelMm = "၁ ပန်းကန်",
            servingLabelEn = "1 plate",
            defaultServingGrams = 250.0,
            caloriesPerServing = 445.0,
            proteinPerServing = 10.5,
            carbsPerServing = 62.0,
            fatPerServing = 16.5,
            caloriesPer100g = 178.0,
            proteinPer100g = 4.2,
            carbsPer100g = 24.8,
            fatPer100g = 6.6,
            defaultUnit = "bowl",
            unitGrams = mapOf("g" to 1.0, "bowl" to 250.0, "serving" to 250.0),
            isEstimate = true
        ),
        FoodItem(
            id = "mm_boiled_egg",
            nameEn = "Boiled Egg",
            nameMm = "ကြက်ဥပြုတ်",
            aliases = listOf("egg", "eggs", "ကြက်ဥ"),
            category = "myanmar_meals",
            servingLabelMm = "၁ လုံး",
            servingLabelEn = "1 egg",
            defaultServingGrams = 50.0,
            caloriesPerServing = 78.0,
            proteinPerServing = 6.3,
            carbsPerServing = 0.6,
            fatPerServing = 5.3,
            caloriesPer100g = 155.0,
            proteinPer100g = 12.6,
            carbsPer100g = 1.1,
            fatPer100g = 10.6,
            defaultUnit = "piece",
            unitGrams = mapOf("g" to 1.0, "piece" to 50.0),
            isMuscleFriendly = true,
            isEstimate = false
        ),
        FoodItem(
            id = "mm_chicken_curry",
            nameEn = "Myanmar Chicken Curry",
            nameMm = "ကြက်သားဟင်း",
            aliases = listOf("chicken curry"),
            category = "myanmar_meals",
            servingLabelMm = "၁ ပွဲ",
            servingLabelEn = "1 serving",
            defaultServingGrams = 150.0,
            caloriesPerServing = 263.0,
            proteinPerServing = 26.3,
            carbsPerServing = 4.8,
            fatPerServing = 15.3,
            caloriesPer100g = 175.0,
            proteinPer100g = 17.5,
            carbsPer100g = 3.2,
            fatPer100g = 10.2,
            defaultUnit = "serving",
            unitGrams = mapOf("g" to 1.0, "serving" to 150.0, "bowl" to 180.0),
            isMuscleFriendly = true,
            isEstimate = true
        ),
        FoodItem(
            id = "pk_mama_pork",
            nameEn = "Mama Instant Noodles (Pork)",
            nameMm = "မာမာခေါက်ဆွဲ (ဝက်သား)",
            aliases = listOf("mama", "မာမာ", "mama noodles"),
            category = "packaged_99",
            servingLabelMm = "၁ ထုပ် (60g)",
            servingLabelEn = "1 pack (60g)",
            defaultServingGrams = 60.0,
            caloriesPerServing = 280.0,
            proteinPerServing = 6.0,
            carbsPerServing = 38.0,
            fatPerServing = 11.0,
            caloriesPer100g = 467.0,
            proteinPer100g = 10.0,
            carbsPer100g = 63.3,
            fatPer100g = 18.3,
            defaultUnit = "pack",
            unitGrams = mapOf("g" to 1.0, "pack" to 60.0, "serving" to 60.0),
            isEstimate = false
        ),
        FoodItem(
            id = "dr_coca_cola",
            nameEn = "Coca-Cola",
            nameMm = "ကိုကာကိုလာ",
            aliases = listOf("coke", "cola", "coca cola"),
            category = "drinks",
            servingLabelMm = "၁ ဘူး (330ml)",
            servingLabelEn = "1 can (330ml)",
            defaultServingGrams = 330.0,
            caloriesPerServing = 139.0,
            proteinPerServing = 0.0,
            carbsPerServing = 35.0,
            fatPerServing = 0.0,
            sugarPerServing = 35.0,
            caloriesPer100g = 42.0,
            proteinPer100g = 0.0,
            carbsPer100g = 10.6,
            fatPer100g = 0.0,
            sugarPer100g = 10.6,
            defaultUnit = "can",
            unitGrams = mapOf("ml" to 1.0, "can" to 330.0, "serving" to 330.0),
            isEstimate = false
        ),
        FoodItem(
            id = "gl_chicken_breast",
            nameEn = "Chicken Breast (Grilled/Boiled)",
            nameMm = "ကြက်ရင်ပုံသား",
            aliases = listOf("chicken breast"),
            category = "global_foods",
            servingLabelMm = "၁ ပွဲ (150g)",
            servingLabelEn = "1 serving (150g)",
            defaultServingGrams = 150.0,
            caloriesPerServing = 248.0,
            proteinPerServing = 46.5,
            carbsPerServing = 0.0,
            fatPerServing = 5.4,
            caloriesPer100g = 165.0,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatPer100g = 3.6,
            defaultUnit = "serving",
            unitGrams = mapOf("g" to 1.0, "serving" to 150.0),
            isMuscleFriendly = true,
            isEstimate = false
        )
    )

    @Test
    fun `burmese digits and fractions parse properly`() {
        assertEquals("25", BurmeseNumberParser.normalizeDigits("၂၅"))
        assertEquals(0.5, BurmeseNumberParser.parseNumberToken("တစ်ဝက်")!!, 0.001)
        assertEquals(0.5, BurmeseNumberParser.parseNumberToken("1/2")!!, 0.001)
        assertEquals(2.5, BurmeseNumberParser.parseNumberToken("၂.၅")!!, 0.001)
    }

    @Test
    fun `parses burmese mixed meal with digits units and default serving`() {
        val input = "ထမင်း ၂ ပန်းကန်၊ ကြက်ဥ ၂ လုံး၊ ကြက်သားဟင်း"
        val results = FoodParser.parse(input, sampleFoods)

        assertEquals(3, results.size)
        assertEquals("mm_steamed_rice", results[0].matchedFood?.id)
        assertEquals(2.0, results[0].quantity, 0.01)
        assertEquals("bowl", results[0].unit)
        assertEquals(520.0, results[0].nutrition.calories, 0.1)

        assertEquals("mm_boiled_egg", results[1].matchedFood?.id)
        assertEquals(2.0, results[1].quantity, 0.01)
        assertEquals("piece", results[1].unit)

        assertEquals("mm_chicken_curry", results[2].matchedFood?.id)
        assertEquals(1.0, results[2].quantity, 0.01)
    }

    @Test
    fun `parses mixed packaged drinks and Myanmar meal sentence`() {
        val input = "မာမာ ၁ ထုပ်၊ coke ၁ ဘူး၊ ထမင်းကြော် ၁ ပန်းကန်"
        val results = FoodParser.parse(input, sampleFoods)

        assertEquals(3, results.size)
        assertEquals("pk_mama_pork", results[0].matchedFood?.id)
        assertEquals("pack", results[0].unit)
        assertEquals(280.0, results[0].nutrition.calories, 0.1)
        assertFalse(results[0].matchedFood!!.isEstimate)

        assertEquals("dr_coca_cola", results[1].matchedFood?.id)
        assertEquals("can", results[1].unit)
        assertEquals(139.0, results[1].nutrition.calories, 0.1)

        assertEquals("mm_fried_rice", results[2].matchedFood?.id)
        assertEquals("bowl", results[2].unit)
        assertTrue(results[2].matchedFood!!.isEstimate)
    }

    @Test
    fun `suggests foods on 1 to 2 characters ranked by prefix and alias`() {
        val cokeHits = FoodParser.suggestFoods("co", sampleFoods)
        assertTrue(cokeHits.isNotEmpty())
        assertEquals("dr_coca_cola", cokeHits.first().id)

        val mamaHits = FoodParser.suggestFoods("မာ", sampleFoods)
        assertTrue(mamaHits.isNotEmpty())
        assertEquals("pk_mama_pork", mamaHits.first().id)
    }

    @Test
    fun `remembers last used serving when quantity and unit omitted`() {
        val foodsWithUsage = sampleFoods.map {
            if (it.id == "mm_boiled_egg") it.copy(lastUsedQuantity = 3.0, lastUsedUnit = "piece") else it
        }
        val res = FoodParser.parse("ကြက်ဥပြုတ်", foodsWithUsage)
        assertEquals(1, res.size)
        assertEquals(3.0, res[0].quantity, 0.01)
        assertEquals("piece", res[0].unit)
    }

    @Test
    fun `fuzzy matching handles slight typos and flags unknown items`() {
        val typo = FoodParser.parse("chikn brest 150g, xyzunmatched999", sampleFoods)
        assertEquals(2, typo.size)
        assertNotNull(typo[0].matchedFood)
        assertEquals("gl_chicken_breast", typo[0].matchedFood?.id)

        assertFalse(typo[1].isMatched)
        assertTrue(typo[1].matchedFood == null)
    }
}
