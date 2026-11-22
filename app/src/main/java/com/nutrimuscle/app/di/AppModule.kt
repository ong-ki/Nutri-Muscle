package com.nutrimuscle.app.di

import android.content.Context
import androidx.room.Room
import com.nutrimuscle.app.data.local.db.AppDatabase
import com.nutrimuscle.app.data.local.db.FoodDao
import com.nutrimuscle.app.data.local.db.MealDao
import com.nutrimuscle.app.data.repository.NutriRepository
import com.nutrimuscle.app.data.repository.NutriRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nutrimuscle_offline.db"
        ).fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFoodDao(db: AppDatabase): FoodDao = db.foodDao()

    @Provides
    fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNutriRepository(impl: NutriRepositoryImpl): NutriRepository
}
