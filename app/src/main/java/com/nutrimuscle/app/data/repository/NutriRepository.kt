package com.nutrimuscle.app.data.repository

import com.nutrimuscle.app.data.backup.BackupManager
import com.nutrimuscle.app.data.backup.NutriMuscleBackup
import com.nutrimuscle.app.data.local.db.FoodDao
import com.nutrimuscle.app.data.local.db.MealDao
import com.nutrimuscle.app.data.local.db.MealEntryEntity
import com.nutrimuscle.app.data.local.db.MealItemEntity
import com.nutrimuscle.app.data.local.db.toDomain
import com.nutrimuscle.app.data.local.db.toEntity
import com.nutrimuscle.app.data.local.prefs.ProfilePreferencesDataSource
import com.nutrimuscle.app.data.local.seed.FoodSeedLoader
import com.nutrimuscle.app.domain.model.AppLanguage
import com.nutrimuscle.app.domain.model.FoodItem
import com.nutrimuscle.app.domain.model.MealEntry
import com.nutrimuscle.app.domain.model.ThemeMode
import com.nutrimuscle.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface NutriRepository {
    val profileFlow: Flow<UserProfile>
    val foodsFlow: Flow<List<FoodItem>>

    suspend fun initializeDatabase()
    suspend fun saveProfile(profile: UserProfile)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setThemeMode(themeMode: ThemeMode)

    fun observeMealsForDate(dateIso: String): Flow<List<MealEntry>>
    fun observeMealsInRange(startDateIso: String, endDateIso: String): Flow<List<MealEntry>>
    suspend fun getMealById(entryId: Long): MealEntry?
    suspend fun saveMeal(entry: MealEntry): Long
    suspend fun deleteMeal(entryId: Long)

    suspend fun saveCustomFood(food: FoodItem)
    suspend fun saveFoodOverride(food: FoodItem)
    suspend fun toggleFavoriteFood(foodId: String, favorite: Boolean)

    suspend fun exportBackupJson(): String
    suspend fun importBackupJson(rawJson: String)
}

@Singleton
class NutriRepositoryImpl @Inject constructor(
    private val profilePrefs: ProfilePreferencesDataSource,
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val seedLoader: FoodSeedLoader,
    private val backupManager: BackupManager
) : NutriRepository {

    override val profileFlow: Flow<UserProfile> = profilePrefs.profileFlow

    override val foodsFlow: Flow<List<FoodItem>> = foodDao.observeAllFoods().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun initializeDatabase() {
        seedLoader.ensureSeeded()
    }

    override suspend fun saveProfile(profile: UserProfile) {
        profilePrefs.saveProfile(profile)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        profilePrefs.setLanguage(language)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        profilePrefs.setThemeMode(themeMode)
    }

    override fun observeMealsForDate(dateIso: String): Flow<List<MealEntry>> =
        mealDao.observeMealsForDate(dateIso).map { rows -> rows.map { it.toDomain() } }

    override fun observeMealsInRange(startDateIso: String, endDateIso: String): Flow<List<MealEntry>> =
        mealDao.observeMealsInRange(startDateIso, endDateIso).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getMealById(entryId: Long): MealEntry? =
        mealDao.getMealById(entryId)?.toDomain()

    override suspend fun saveMeal(entry: MealEntry): Long {
        val now = System.currentTimeMillis()
        val entity = MealEntryEntity(
            id = entry.id,
            dateIso = entry.dateIso,
            mealType = entry.mealType,
            createdAtMillis = entry.createdAtMillis
        )
        val items = entry.items.map { item ->
            MealItemEntity(
                id = 0,
                mealEntryId = entry.id,
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
        val mealId = mealDao.upsertMealWithItems(entity, items)
        for (item in entry.items) {
            foodDao.recordFoodUsage(item.foodId, now, item.quantity, item.unit)
        }
        return mealId
    }

    override suspend fun deleteMeal(entryId: Long) {
        mealDao.deleteMealEntryById(entryId)
    }

    override suspend fun saveCustomFood(food: FoodItem) {
        foodDao.insertFood(food.copy(isCustom = true, isEstimate = false).toEntity())
    }

    override suspend fun saveFoodOverride(food: FoodItem) {
        foodDao.insertFood(food.copy(isOverride = true, isEstimate = false).toEntity())
    }

    override suspend fun toggleFavoriteFood(foodId: String, favorite: Boolean) {
        foodDao.setFavorite(foodId, favorite)
    }

    override suspend fun exportBackupJson(): String {
        val profile = profilePrefs.profileFlow.first()
        val customFoods = foodDao.getCustomFoodsSnapshot().map { it.toDomain() }
        val meals = mealDao.getAllMealsSnapshot().map { it.toDomain() }
        val backup = NutriMuscleBackup(
            exportedAtMillis = System.currentTimeMillis(),
            profile = profile,
            customFoods = customFoods,
            meals = meals
        )
        return backupManager.encodeBackup(backup)
    }

    override suspend fun importBackupJson(rawJson: String) {
        val backup = backupManager.decodeBackup(rawJson)
        seedLoader.ensureSeeded()
        profilePrefs.saveProfile(backup.profile)
        if (backup.customFoods.isNotEmpty()) {
            foodDao.insertAll(backup.customFoods.map { it.toEntity() })
        }
        mealDao.deleteAllMeals()
        for (meal in backup.meals) {
            saveMeal(meal.copy(id = 0L))
        }
    }
}
