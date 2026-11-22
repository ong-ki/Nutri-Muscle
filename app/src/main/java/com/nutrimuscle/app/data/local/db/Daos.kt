package com.nutrimuscle.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY isFavorite DESC, usageCount DESC, isCustom DESC, nameEn ASC")
    fun observeAllFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE category = :category ORDER BY isFavorite DESC, usageCount DESC, nameEn ASC")
    fun observeFoodsByCategory(category: String): Flow<List<FoodEntity>>

    @Query(
        "SELECT * FROM foods WHERE (:query = '' OR nameEn LIKE '%' || :query || '%' " +
            "OR nameMm LIKE '%' || :query || '%' OR aliasesJson LIKE '%' || :query || '%') " +
            "ORDER BY isFavorite DESC, usageCount DESC, nameEn ASC"
    )
    fun searchFoods(query: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods")
    suspend fun getAllFoodsSnapshot(): List<FoodEntity>

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun countFoods(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity)

    @Query("SELECT * FROM foods WHERE isCustom = 1 OR isOverride = 1")
    suspend fun getCustomFoodsSnapshot(): List<FoodEntity>

    @Query("UPDATE foods SET isFavorite = :favorite WHERE id = :foodId")
    suspend fun setFavorite(foodId: String, favorite: Boolean)

    @Query(
        "UPDATE foods SET usageCount = usageCount + 1, lastUsedAtMillis = :usedAtMillis, " +
            "lastUsedQuantity = :quantity, lastUsedUnit = :unit WHERE id = :foodId"
    )
    suspend fun recordFoodUsage(foodId: String, usedAtMillis: Long, quantity: Double, unit: String)
}

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meal_entries WHERE dateIso = :dateIso ORDER BY createdAtMillis ASC")
    fun observeMealsForDate(dateIso: String): Flow<List<MealEntryWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE dateIso BETWEEN :startDateIso AND :endDateIso ORDER BY dateIso ASC, createdAtMillis ASC")
    fun observeMealsInRange(startDateIso: String, endDateIso: String): Flow<List<MealEntryWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_entries ORDER BY dateIso ASC, createdAtMillis ASC")
    suspend fun getAllMealsSnapshot(): List<MealEntryWithItems>

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE id = :entryId")
    suspend fun getMealById(entryId: Long): MealEntryWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealEntry(entry: MealEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItems(items: List<MealItemEntity>)

    @Query("DELETE FROM meal_items WHERE mealEntryId = :mealEntryId")
    suspend fun deleteItemsForMeal(mealEntryId: Long)

    @Query("DELETE FROM meal_entries WHERE id = :entryId")
    suspend fun deleteMealEntryById(entryId: Long)

    @Query("DELETE FROM meal_entries")
    suspend fun deleteAllMeals()

    @Transaction
    suspend fun upsertMealWithItems(
        entry: MealEntryEntity,
        items: List<MealItemEntity>
    ): Long {
        val mealId = if (entry.id == 0L) {
            insertMealEntry(entry)
        } else {
            insertMealEntry(entry)
            deleteItemsForMeal(entry.id)
            entry.id
        }
        if (items.isNotEmpty()) {
            insertMealItems(items.map { it.copy(mealEntryId = mealId, id = 0L) })
        }
        return mealId
    }
}
