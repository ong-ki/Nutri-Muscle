package com.nutrimuscle.app.data.backup

import com.nutrimuscle.app.domain.model.FoodItem
import com.nutrimuscle.app.domain.model.MealEntry
import com.nutrimuscle.app.domain.model.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class NutriMuscleBackup(
    val version: Int = 1,
    val exportedAtMillis: Long,
    val profile: UserProfile,
    val customFoods: List<FoodItem>,
    val meals: List<MealEntry>
)

@Singleton
class BackupManager @Inject constructor(
    private val json: Json
) {
    fun encodeBackup(backup: NutriMuscleBackup): String {
        return json.encodeToString(backup)
    }

    fun decodeBackup(rawJson: String): NutriMuscleBackup {
        return json.decodeFromString(rawJson)
    }
}
