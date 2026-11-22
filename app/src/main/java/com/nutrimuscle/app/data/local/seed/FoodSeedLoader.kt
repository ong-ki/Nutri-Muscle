package com.nutrimuscle.app.data.local.seed

import android.content.Context
import com.nutrimuscle.app.data.local.db.FoodDao
import com.nutrimuscle.app.data.local.db.toEntity
import com.nutrimuscle.app.domain.model.FoodItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodSeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foodDao: FoodDao,
    private val json: Json
) {
    private val seedMutex = Mutex()

    private val categoryAssetFiles = listOf(
        "myanmar_meals.json",
        "myanmar_snacks.json",
        "street_food.json",
        "food_court.json",
        "packaged_99.json",
        "drinks.json",
        "fruits.json",
        "global_foods.json"
    )

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            if (foodDao.countFoods() >= 600) return@withLock
            val allEntities = categoryAssetFiles.flatMap { fileName ->
                runCatching {
                    val raw = context.assets.open(fileName).bufferedReader().use { it.readText() }
                    json.decodeFromString<List<FoodItem>>(raw).map { it.toEntity() }
                }.getOrDefault(emptyList())
            }
            if (allEntities.isNotEmpty()) {
                foodDao.insertAllIgnore(allEntities)
            }
        }
    }
}
