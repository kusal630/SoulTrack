package io.github.dailytrack.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String = "",
    val type: String = "TIME", // TIME, LOCATION, HABIT, STREAK
    val timeHour: Int = 9,
    val timeMinute: Int = 0,
    val daysOfWeek: String = "1,2,3,4,5", // Comma-separated day numbers (1=Monday)
    val isEnabled: Boolean = true,
    val locationName: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val habitId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
