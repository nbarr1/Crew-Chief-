package com.example.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.core.data.local.converter.Converters
import com.example.core.data.local.dao.CareerDao
import com.example.core.data.local.entity.CareerProfileEntity
import com.example.core.data.local.entity.GameRecordEntity
import com.example.core.data.local.entity.SnapEvaluationEntity

@Database(
    entities = [
        CareerProfileEntity::class,
        GameRecordEntity::class,
        SnapEvaluationEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CrewChiefDatabase : RoomDatabase() {

    abstract fun careerDao(): CareerDao

    companion object {
        @Volatile
        private var INSTANCE: CrewChiefDatabase? = null

        fun getInstance(context: Context): CrewChiefDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CrewChiefDatabase::class.java,
                    "crew_chief_career.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
