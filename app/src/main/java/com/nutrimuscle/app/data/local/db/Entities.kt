package com.nutrimuscle.app.data.local.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.nutrimuscle.app.domain.model.FoodItem
import com.nutrimuscle.app.domain.model.LoggedFoodItem
import com.nutrimuscle.app.domain.model.MealEntry
import com.nutrimuscle.app.domain.model.MealType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameMm: String,
    val aliasesJson: String,
    val category: String,
    val servingLabelMm: String,
    val servingLabelEn: String,
    val defaultServingGrams: Double,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val carbsPerServing: Double,
    val fatPerServing: Double,
    val fiberPerServing: Double,
    val sugarPerServing: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double,
    val sugarPer100g: Double,
    val defaultUnit: String,
    val unitGramsJson: String,
    val isMuscleFriendly: Boolean,
    val isEstimate: Boolean,
    val isCustom: Boolean,
    val isOverride: Boolean,
    val isFavorite: Boolean,
    val usageCount: Int,
    val lastUsedAtMillis: Long,
    val lastUsedQuantity: Double?,
    val lastUsedUnit: String?
)

@Entity(
    tableName = "meal_entries",
    indices = [Index(value = ["dateIso"])]
)
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,
    val mealType: MealType,
    val createdAtMillis: Long
)

@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mealEntryId"])]
)
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealEntryId: Long,
    val foodId: String,
    val foodNameEn: String,
    val foodNameMm: String,
    val quantity: Double,
    val unit: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double = 0.0,
    val isEstimate: Boolean = true
)

data class MealEntryWithItems(
    @Embedded val entry: MealEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealEntryId"
    )
    val items: List<MealItemEntity>
)

class RoomConverters {
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = runCatching { MealType.valueOf(value) }
        .getOrDefault(MealType.SNACK)
}

private val entityJson = Json { ignoreUnknownKeys = true }

fun FoodEntity.toDomain(): FoodItem = FoodItem(
    id = id,
    nameEn = nameEn,
    nameMm = nameMm,
    aliases = runCatching { entityJson.decodeFromString<List<String>>(aliasesJson) }.getOrDefault(emptyList()),
    category = category,
    servingLabelMm = servingLabelMm,
    servingLabelEn = servingLabelEn,
    defaultServingGrams = defaultServingGrams,
    caloriesPerServing = caloriesPerServing,
    proteinPerServing = proteinPerServing,
    carbsPerServing = carbsPerServing,
    fatPerServing = fatPerServing,
    fiberPerServing = fiberPerServing,
    sugarPerServing = sugarPerServing,
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    sugarPer100g = sugarPer100g,
    defaultUnit = defaultUnit,
    unitGrams = runCatching { entityJson.decodeFromString<Map<String, Double>>(unitGramsJson) }
        .getOrDefault(mapOf("g" to 1.0, defaultUnit to defaultServingGrams)),
    isMuscleFriendly = isMuscleFriendly,
    isEstimate = isEstimate,
    isCustom = isCustom,
    isOverride = isOverride,
    isFavorite = isFavorite,
    usageCount = usageCount,
    lastUsedAtMillis = lastUsedAtMillis,
    lastUsedQuantity = lastUsedQuantity,
    lastUsedUnit = lastUsedUnit
)

fun FoodItem.toEntity(): FoodEntity = FoodEntity(
    id = id,
    nameEn = nameEn,
    nameMm = nameMm,
    aliasesJson = entityJson.encodeToString(aliases),
    category = category,
    servingLabelMm = servingLabelMm,
    servingLabelEn = servingLabelEn,
    defaultServingGrams = defaultServingGrams,
    caloriesPerServing = caloriesPerServing,
    proteinPerServing = proteinPerServing,
    carbsPerServing = carbsPerServing,
    fatPerServing = fatPerServing,
    fiberPerServing = fiberPerServing,
    sugarPerServing = sugarPerServing,
    caloriesPer100g = effectiveCaloriesPer100,
    proteinPer100g = effectiveProteinPer100,
    carbsPer100g = effectiveCarbsPer100,
    fatPer100g = effectiveFatPer100,
    fiberPer100g = effectiveFiberPer100,
    sugarPer100g = effectiveSugarPer100,
    defaultUnit = defaultUnit,
    unitGramsJson = entityJson.encodeToString(unitGrams),
    isMuscleFriendly = isMuscleFriendly,
    isEstimate = isEstimate,
    isCustom = isCustom,
    isOverride = isOverride,
    isFavorite = isFavorite,
    usageCount = usageCount,
    lastUsedAtMillis = lastUsedAtMillis,
    lastUsedQuantity = lastUsedQuantity,
    lastUsedUnit = lastUsedUnit
)

fun MealEntryWithItems.toDomain(): MealEntry = MealEntry(
    id = entry.id,
    dateIso = entry.dateIso,
    mealType = entry.mealType,
    createdAtMillis = entry.createdAtMillis,
    items = items.map { item ->
        LoggedFoodItem(
            id = item.id,
            foodId = item.foodId,
            foodNameEn = item.foodNameEn,
            foodNameMm = item.foodNameMm,
            quantity = item.quantity,
            unit = item.unit,
            grams = item.grams,
            calories = item.calories,
            protein = item.protein,
            carbs = item.carbs,
            fat = item.fat,
            fiber = item.fiber,
            sugar = item.sugar,
            isEstimate = item.isEstimate
        )
    }
)
