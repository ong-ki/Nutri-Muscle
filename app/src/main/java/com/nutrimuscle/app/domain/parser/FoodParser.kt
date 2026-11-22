package com.nutrimuscle.app.domain.parser

import com.nutrimuscle.app.domain.model.FoodItem
import com.nutrimuscle.app.domain.model.ParsedFoodCandidate
import java.util.UUID
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

object BurmeseNumberParser {
    private val burmeseDigits = mapOf(
        '၀' to '0', '၁' to '1', '၂' to '2', '၃' to '3', '၄' to '4',
        '၅' to '5', '၆' to '6', '၇' to '7', '၈' to '8', '၉' to '9'
    )

    private val numberWordMap = linkedMapOf(
        "တစ်ဝက်" to 0.5,
        "တဝက်" to 0.5,
        "half" to 0.5,
        "တစ်" to 1.0,
        "နှစ်" to 2.0,
        "သုံး" to 3.0,
        "လေး" to 4.0,
        "ငါး" to 5.0,
        "ခြောက်" to 6.0,
        "ခုနစ်" to 7.0,
        "ရှစ်" to 8.0,
        "ကိုး" to 9.0,
        "တစ်ဆယ်" to 10.0,
        "one" to 1.0,
        "two" to 2.0,
        "three" to 3.0,
        "four" to 4.0,
        "five" to 5.0
    )

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(burmeseDigits[ch] ?: ch)
        }
        return sb.toString()
    }

    fun parseNumberToken(token: String): Double? {
        val cleaned = normalizeDigits(token).trim().lowercase()
        if (cleaned.isEmpty()) return null
        numberWordMap[cleaned]?.let { return it }

        if ('/' in cleaned) {
            val parts = cleaned.split('/')
            if (parts.size == 2) {
                val num = parts[0].trim().toDoubleOrNull()
                val den = parts[1].trim().toDoubleOrNull()
                if (num != null && den != null && den != 0.0) {
                    return num / den
                }
            }
        }
        return cleaned.toDoubleOrNull()
    }
}

object FoodParser {

    private val unitAliases: Map<String, String> = mapOf(
        // Burmese units
        "ပန်းကန်လုံး" to "bowl",
        "ပန်းကန်" to "bowl",
        "ခွက်" to "cup",
        "လုံး" to "piece",
        "ခု" to "piece",
        "ချပ်" to "slice",
        "ထုပ်" to "pack",
        "ဘူး" to "can",
        "ပုလင်း" to "bottle",
        "ချောင်း" to "skewer",
        "ပက်" to "shot",
        "ထမင်းစားဇွန်း" to "spoon",
        "လက်ဖက်ရည်ဇွန်း" to "spoon",
        "ဇွန်း" to "spoon",
        "ပွဲ" to "serving",
        "ကီလိုဂရမ်" to "kg",
        "ကီလို" to "kg",
        "ဂရမ်" to "g",
        "မီလီလီတာ" to "ml",
        "မီလီ" to "ml",
        // English units
        "bowl" to "bowl",
        "bowls" to "bowl",
        "plate" to "serving",
        "plates" to "serving",
        "cup" to "cup",
        "cups" to "cup",
        "glass" to "cup",
        "glasses" to "cup",
        "piece" to "piece",
        "pieces" to "piece",
        "pc" to "piece",
        "pcs" to "piece",
        "slice" to "slice",
        "slices" to "slice",
        "pack" to "pack",
        "packs" to "pack",
        "pkt" to "pack",
        "packet" to "pack",
        "packets" to "pack",
        "bag" to "pack",
        "bags" to "pack",
        "box" to "pack",
        "boxes" to "pack",
        "can" to "can",
        "cans" to "can",
        "tin" to "can",
        "tins" to "can",
        "bottle" to "bottle",
        "bottles" to "bottle",
        "btl" to "bottle",
        "skewer" to "skewer",
        "skewers" to "skewer",
        "stick" to "skewer",
        "sticks" to "skewer",
        "bar" to "bar",
        "bars" to "bar",
        "cone" to "cone",
        "cones" to "cone",
        "shot" to "shot",
        "shots" to "shot",
        "scoop" to "scoop",
        "scoops" to "scoop",
        "spoon" to "spoon",
        "spoons" to "spoon",
        "tbsp" to "spoon",
        "tsp" to "spoon",
        "serving" to "serving",
        "servings" to "serving",
        "g" to "g",
        "gram" to "g",
        "grams" to "g",
        "kg" to "kg",
        "kilogram" to "kg",
        "ml" to "ml"
    )

    private val splitRegex = Regex("""[,+\n၊။]+|(?:\s+(?:and|နဲ့|နှင့်)\s+)|(?:နဲ့|နှင့်)""")

    private val attachedRegex = Regex(
        """(?<![\w.])(\d+(?:\.\d+)?|\d+/\d+)\s*(kg|kilogram|g|gram|grams|ml|cups?|glasses?|bowls?|plates?|pieces?|pcs?|slices?|packs?|packets?|pkt|bags?|boxes?|cans?|tins?|bottles?|btl|skewers?|sticks?|bars?|cones?|shots?|scoops?|spoons?|tbsp|tsp|servings?|ပန်းကန်လုံး|ပန်းကန်|ခွက်|လုံး|ခု|ချပ်|ထုပ်|ဘူး|ပုလင်း|ချောင်း|ပက်|ထမင်းစားဇွန်း|လက်ဖက်ရည်ဇွန်း|ဇွန်း|ပွဲ|ကီလိုဂရမ်|ကီလို|ဂရမ်|မီလီလီတာ|မီလီ)(?=\s|$)""",
        RegexOption.IGNORE_CASE
    )

    fun splitSegments(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        return input.split(splitRegex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun parse(input: String, database: List<FoodItem>): List<ParsedFoodCandidate> {
        val segments = splitSegments(input)
        return segments.map { segment -> parseSegment(segment, database) }
    }

    fun parseSegment(segment: String, database: List<FoodItem>): ParsedFoodCandidate {
        val normalized = normalizeBurmeseScript(BurmeseNumberParser.normalizeDigits(segment)).trim()

        var working = normalized
        var extractedQuantity: Double? = null
        var extractedUnit: String? = null

        // 1. Handle attached number+unit like "200g", "1.5kg", "250ml", "၁ ထုပ်", "1 can"
        val attachedMatch = attachedRegex.find(working)
        if (attachedMatch != null) {
            extractedQuantity = BurmeseNumberParser.parseNumberToken(attachedMatch.groupValues[1])
            extractedUnit = normalizeUnit(attachedMatch.groupValues[2])
            working = working.removeRange(attachedMatch.range).trim()
        }

        // 2. Handle Burmese half/number word + optional unit: e.g., "တစ်ဝက်", "နှစ် လုံး"
        if (extractedQuantity == null) {
            val numberWordRegex = Regex(
                """(?:^|\s)(တစ်ဝက်|တဝက်|half|တစ်ဆယ်|တစ်|နှစ်|သုံး|လေး|ငါး|ခြောက်|ခုနစ်|ရှစ်|ကိုး|one|two|three|four|five)(?=\s|$)""",
                RegexOption.IGNORE_CASE
            )
            val wordMatch = numberWordRegex.find(working)
            if (wordMatch != null) {
                val qty = BurmeseNumberParser.parseNumberToken(wordMatch.groupValues[1])
                if (qty != null) {
                    extractedQuantity = qty
                    working = working.removeRange(wordMatch.range).trim()
                }
            }
        }

        // 3. Handle standalone numeric quantity (e.g., "2", "1/2", "1.5")
        if (extractedQuantity == null) {
            val qtyRegex = Regex("""(?<![\w.])(\d+/\d+|\d+(?:\.\d+)?)(?=\s|$)""")
            val qtyMatch = qtyRegex.find(working)
            if (qtyMatch != null) {
                extractedQuantity = BurmeseNumberParser.parseNumberToken(qtyMatch.groupValues[1])
                working = working.removeRange(qtyMatch.range).trim()
            }
        }

        // 4. Handle standalone unit token if not attached
        if (extractedUnit == null) {
            val sortedUnits = unitAliases.keys.sortedByDescending { it.length }
            for (uKey in sortedUnits) {
                val unitPattern = if (uKey.all { it.code < 128 }) {
                    Regex("""\b${Regex.escape(uKey)}\b""", RegexOption.IGNORE_CASE)
                } else {
                    Regex("""(?<=\s|^)${Regex.escape(uKey)}(?=\s|$)""")
                }
                val match = unitPattern.find(working)
                if (match != null) {
                    extractedUnit = unitAliases[uKey]
                    working = working.removeRange(match.range).trim()
                    break
                }
            }
        }

        val cleanQuery = working
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""^of\s+""", RegexOption.IGNORE_CASE), "")
            .trim()

        val match = findBestMatch(cleanQuery, database)
        val matchedFood = match?.first
        val score = match?.second ?: 0.0

        val finalQuantity = extractedQuantity ?: matchedFood?.lastUsedQuantity ?: 1.0
        val finalUnit = extractedUnit ?: matchedFood?.lastUsedUnit ?: matchedFood?.defaultUnit ?: "serving"

        return ParsedFoodCandidate(
            id = UUID.randomUUID().toString(),
            rawSegment = segment,
            queryName = cleanQuery.ifEmpty { segment },
            quantity = finalQuantity,
            unit = finalUnit,
            matchedFood = matchedFood,
            matchScore = score,
            isMatched = matchedFood != null
        )
    }

    /**
     * Live suggestions from 1-2+ characters ranked by:
     * 1. Exact / prefix match on name
     * 2. Alias match (exact / prefix / contains)
     * 3. Fuzzy match (Levenshtein / token overlap)
     * 4. Recent / frequent / favorite boost
     */
    fun suggestFoods(currentInput: String, database: List<FoodItem>, limit: Int = 8): List<FoodItem> {
        val lastSegment = splitSegments(currentInput).lastOrNull()
            ?: currentInput.trim()
        if (lastSegment.isBlank()) return emptyList()

        val stripped = stripQtyAndUnitForAutocomplete(lastSegment)
        val q = normalizeText(stripped.ifBlank { lastSegment })
        if (q.isEmpty()) return emptyList()

        val minThreshold = if (q.length <= 2) 0.55 else 0.34

        return database
            .mapNotNull { food ->
                val baseScore = scoreFoodForSearch(q, food)
                if (baseScore >= minThreshold) {
                    val recencyBoost = computeUsageBoost(food)
                    food to (baseScore + recencyBoost)
                } else null
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun computeUsageBoost(food: FoodItem): Double {
        var boost = 0.0
        if (food.usageCount > 0) {
            boost += min(0.08, ln(food.usageCount.toDouble() + 1.0) * 0.03)
        }
        if (food.lastUsedAtMillis > 0L) {
            boost += 0.03
        }
        if (food.isFavorite) {
            boost += 0.04
        }
        return boost
    }

    private fun stripQtyAndUnitForAutocomplete(text: String): String {
        var working = normalizeBurmeseScript(BurmeseNumberParser.normalizeDigits(text))
        working = working.replace(Regex("""\b\d+(?:\.\d+)?\b|\d+/\d+"""), " ")
        for (uKey in unitAliases.keys.sortedByDescending { it.length }) {
            working = if (uKey.all { it.code < 128 }) {
                working.replace(Regex("""\b${Regex.escape(uKey)}\b""", RegexOption.IGNORE_CASE), " ")
            } else {
                working.replace(uKey, " ")
            }
        }
        return working.replace(Regex("""\s+"""), " ").trim()
    }

    private fun findBestMatch(query: String, database: List<FoodItem>): Pair<FoodItem, Double>? {
        val normalizedQuery = normalizeText(query)
        if (normalizedQuery.isEmpty() || database.isEmpty()) return null

        var bestFood: FoodItem? = null
        var bestScore = 0.0

        for (food in database) {
            val base = scoreFoodForSearch(normalizedQuery, food)
            val total = base + computeUsageBoost(food) * 0.4
            if (total > bestScore) {
                bestScore = total
                bestFood = food
            }
        }

        return if (bestFood != null && bestScore >= 0.52) {
            bestFood to bestScore
        } else {
            null
        }
    }

    /**
     * Ranked scoring:
     * - Tier 1 (1.00 - 1.25): Exact & prefix matches on primary names (EN/MM)
     * - Tier 2 (0.85 - 1.10): Exact & prefix matches on aliases
     * - Tier 3 (0.65 - 0.85): Substring contains & word-start matches
     * - Tier 4 (0.00 - 0.68): Fuzzy Levenshtein & token overlap
     */
    private fun scoreFoodForSearch(normalizedQuery: String, food: FoodItem): Double {
        val primaryNames = listOf(food.nameEn, food.nameMm)
            .map { normalizeText(it) }
            .filter { it.isNotEmpty() }
        val aliasNames = food.aliases
            .map { normalizeText(it) }
            .filter { it.isNotEmpty() }

        var maxScore = 0.0

        for (candidate in primaryNames) {
            val s = scoreCandidate(normalizedQuery, candidate, isPrimary = true)
            if (s > maxScore) maxScore = s
        }
        for (candidate in aliasNames) {
            val s = scoreCandidate(normalizedQuery, candidate, isPrimary = false)
            if (s > maxScore) maxScore = s
        }
        return maxScore
    }

    private fun scoreCandidate(q: String, candidate: String, isPrimary: Boolean): Double {
        val tierOffset = if (isPrimary) 0.08 else 0.0
        if (candidate == q) return 1.15 + tierOffset

        // Prefix match — works from 1-2 characters; reward tight first-word prefix match
        if (candidate.startsWith(q)) {
            val firstWord = candidate.substringBefore(' ').ifEmpty { candidate }
            val ratio = max(
                q.length.toDouble() / candidate.length.toDouble(),
                (q.length.toDouble() / firstWord.length.toDouble()) * 0.95
            )
            return 0.92 + tierOffset + 0.12 * ratio
        }

        // Any word inside candidate starts with q (e.g. "cola" in "Coca-Cola", "rice" in "Fried Rice")
        val words = candidate.split(' ')
        if (words.any { it.startsWith(q) }) {
            val lengthRatio = q.length.toDouble() / candidate.length.toDouble()
            return 0.82 + tierOffset * 0.6 + 0.10 * lengthRatio
        }

        if (q.length <= 2) {
            // For 1-2 char queries, avoid noisy fuzzy matches; allow substring only for 2-char Burmese
            if (q.length == 2 && candidate.contains(q)) {
                return 0.62 + tierOffset * 0.5
            }
            return 0.0
        }

        if (q.startsWith(candidate) && candidate.length >= 2) {
            return 0.84 + 0.10 * (candidate.length.toDouble() / q.length.toDouble())
        }
        if (candidate.contains(q)) {
            return 0.74 + tierOffset * 0.5 + 0.12 * (q.length.toDouble() / candidate.length.toDouble())
        }
        if (q.contains(candidate) && candidate.length >= 3) {
            return 0.70 + 0.12 * (candidate.length.toDouble() / q.length.toDouble())
        }

        val levSim = levenshteinSimilarity(q, candidate)
        val tokenSim = tokenOverlapSimilarity(q, candidate)
        val bestWordLev = words
            .filter { it.length >= 3 }
            .maxOfOrNull { levenshteinSimilarity(q, it) * 0.88 } ?: 0.0

        return max(max(levSim, tokenSim), bestWordLev)
    }

    private fun normalizeUnit(raw: String): String {
        return unitAliases[raw.lowercase()] ?: unitAliases[raw] ?: "serving"
    }

    /**
     * Normalizes Burmese Unicode & common Zawgyi-style input variants:
     * - Strips zero-width spaces / joiners (\u200B, \u200C, \u200D, \uFEFF)
     * - Maps common Zawgyi glyphs (medial ra, u/uu, ya-yit) to Unicode equivalents
     * - Reorders leading Zawgyi vowel-E (\u1031) placed before a consonant
     * - Unifies tall-aa (\u102B) with standard aa (\u102C) for forgiving spelling match
     */
    fun normalizeBurmeseScript(text: String): String {
        var s = text
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace("\uFEFF", "")
            // Zawgyi medial ra variants -> Unicode \u103C
            .replace(Regex("""[\u107E-\u1084]"""), "\u103C")
            // Zawgyi u / uu variants -> Unicode \u102F / \u1030
            .replace('\u1033', '\u102F')
            .replace('\u1034', '\u1030')
            // Zawgyi kinzi
            .replace("\u1064", "\u1004\u103A\u1039")
            // Zawgyi aukmyit variant
            .replace('\u1094', '\u1037')
            .replace('\u1095', '\u1037')
            // Unify tall-aa and standard aa for forgiving lookup
            .replace('\u102B', '\u102C')

        // Reorder Zawgyi visual-order vowel E (\u1031) placed before a Myanmar consonant ([\u1000-\u1021])
        s = s.replace(Regex("""\u1031([\u1000-\u1021])"""), "$1\u1031")
        // Reorder Zawgyi medial ra (\u103C) placed before a Myanmar consonant
        s = s.replace(Regex("""\u103C([\u1000-\u1021])"""), "$1\u103C")
        return s
    }

    fun normalizeText(text: String): String {
        return normalizeBurmeseScript(text)
            .lowercase()
            .replace(Regex("""[^\p{L}\p{N}\p{M}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun tokenOverlapSimilarity(a: String, b: String): Double {
        val tokensA = a.split(' ').filter { it.length >= 2 }.toSet()
        val tokensB = b.split(' ').filter { it.length >= 2 }.toSet()
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0
        val intersection = tokensA.intersect(tokensB).size
        val union = tokensA.union(tokensB).size
        if (intersection == 0) return 0.0
        return (intersection.toDouble() / union.toDouble()) * 0.85
    }

    fun levenshteinSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val maxLen = max(a.length, b.length)
        if (maxLen == 0) return 1.0
        val dist = levenshteinDistance(a, b)
        return 1.0 - (dist.toDouble() / maxLen.toDouble())
    }

    fun levenshteinDistance(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }
}
