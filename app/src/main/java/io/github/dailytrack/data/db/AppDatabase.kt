/*
 * Copyright 2024 Soul Track Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.github.dailytrack.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.dailytrack.data.db.dao.*
import io.github.dailytrack.data.db.entity.*

@Database(
    entities = [
        CategoryEntity::class,
        SessionEntity::class,
        TagEntity::class,
        SessionTagCrossRef::class,
        FoodEntryEntity::class,
        DrinkEntryEntity::class,
        BodyMetricEntity::class,
        SymptomEntryEntity::class,
        MoodCheckInEntity::class,
        JournalEntryEntity::class,
        LifeEventEntity::class,
        InsightEntity::class,
        UserProfileEntity::class,
        TodoEntity::class,
        PomodoroSessionEntity::class,
        SavedQuoteEntity::class,
        SubtaskEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        GoalEntity::class,
        AchievementEntity::class,
        UserStatsEntity::class,
        ReminderEntity::class,
        ParkedThoughtEntity::class
    ],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun sessionDao(): SessionDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun drinkEntryDao(): DrinkEntryDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun symptomEntryDao(): SymptomEntryDao
    abstract fun moodCheckInDao(): MoodCheckInDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun lifeEventDao(): LifeEventDao
    abstract fun insightDao(): InsightDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun todoDao(): TodoDao
    abstract fun pomodoroSessionDao(): PomodoroSessionDao
    abstract fun savedQuoteDao(): SavedQuoteDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun achievementDao(): AchievementDao
    abstract fun reminderDao(): ReminderDao
    abstract fun parkedThoughtDao(): ParkedThoughtDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS subtasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        todoId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        icon TEXT NOT NULL DEFAULT '⭐',
                        color TEXT NOT NULL DEFAULT '#10B981',
                        frequency TEXT NOT NULL DEFAULT 'DAILY',
                        targetCount INTEGER NOT NULL DEFAULT 1,
                        currentStreak INTEGER NOT NULL DEFAULT 0,
                        bestStreak INTEGER NOT NULL DEFAULT 0,
                        totalCompletions INTEGER NOT NULL DEFAULT 0,
                        lastCompletedDate INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_completions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitId INTEGER NOT NULL,
                        completedDate INTEGER NOT NULL,
                        notes TEXT NOT NULL DEFAULT ''
                    )
                """)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT 'PRODUCTIVE',
                        targetType TEXT NOT NULL DEFAULT 'WEEKLY',
                        targetHours REAL NOT NULL DEFAULT 10.0,
                        currentHours REAL NOT NULL DEFAULT 0.0,
                        startDate INTEGER NOT NULL DEFAULT 0,
                        endDate INTEGER,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        isArchived INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS achievements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        category TEXT NOT NULL,
                        targetValue INTEGER NOT NULL,
                        currentValue INTEGER NOT NULL DEFAULT 0,
                        isUnlocked INTEGER NOT NULL DEFAULT 0,
                        unlockedAt INTEGER,
                        xpReward INTEGER NOT NULL DEFAULT 100
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_stats (
                        id INTEGER PRIMARY KEY NOT NULL,
                        totalXp INTEGER NOT NULL DEFAULT 0,
                        level INTEGER NOT NULL DEFAULT 1,
                        totalHoursTracked REAL NOT NULL DEFAULT 0.0,
                        totalHabitsCompleted INTEGER NOT NULL DEFAULT 0,
                        totalGoalsAchieved INTEGER NOT NULL DEFAULT 0,
                        longestStreak INTEGER NOT NULL DEFAULT 0,
                        achievementsUnlocked INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL DEFAULT '',
                        type TEXT NOT NULL DEFAULT 'TIME',
                        timeHour INTEGER NOT NULL DEFAULT 9,
                        timeMinute INTEGER NOT NULL DEFAULT 0,
                        daysOfWeek TEXT NOT NULL DEFAULT '1,2,3,4,5',
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        locationName TEXT,
                        locationLatitude REAL,
                        locationLongitude REAL,
                        habitId INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Task linkage: lets switch detection tell "same work" from "new task".
                db.execSQL("ALTER TABLE sessions ADD COLUMN todoId INTEGER")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Phone-usage rows were auto-inserted as if they were tracked
                // work, which is what produced the phantom "41h" days. Only
                // time the user actually started from the app may count, so the
                // legacy usage rows are removed. The live wellbeing summary is
                // recomputed from UsageStatsManager and needs no stored rows.
                db.execSQL("DELETE FROM sessions WHERE source = 'USAGE'")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Implementation-intention ("if-then") planning on tasks.
                db.execSQL("ALTER TABLE todos ADD COLUMN triggerText TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE todos ADD COLUMN triggerHour INTEGER NOT NULL DEFAULT -1")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Parking lot for distractions the user offloads during focus.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS parked_thoughts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        text TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        resolvedAt INTEGER
                    )
                """)
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Coping plan: what to do when the cue meets friction.
                db.execSQL("ALTER TABLE todos ADD COLUMN obstacleText TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soultrack.db"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
