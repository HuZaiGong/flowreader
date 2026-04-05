package com.flowreader.app.domain.model

import java.util.Date

data class ReadingStats(
    val id: Long = 0,
    val bookId: Long,
    val date: Date,
    val readDurationMinutes: Int = 0,
    val pagesRead: Int = 0,
    val chaptersRead: Int = 0
)

data class DailyStats(
    val date: String,
    val totalReadTime: Int,
    val totalPages: Int
)

data class BookStats(
    val bookId: Long,
    val bookTitle: String,
    val totalReadTime: Int,
    val totalChapters: Int,
    val totalPages: Int,
    val lastReadDate: Date?
)

enum class AchievementType {
    CHAPTERS_READ_100,
    CHAPTERS_READ_500,
    CHAPTERS_READ_1000,
    READING_TIME_10H,
    READING_TIME_50H,
    READING_TIME_100H,
    BOOKS_FINISHED_1,
    BOOKS_FINISHED_5,
    BOOKS_FINISHED_10,
    BOOKS_FINISHED_50,
    READING_SPEED_10000_PER_HOUR,
    FOCUSED_READING_2H,
    NIGHT_READER_100H,
    EARLY_BIRD_50H,
    CONTINUOUS_READING_7_DAYS,
    CONTINUOUS_READING_30_DAYS
}

enum class ReaderLevel(val displayName: String, val minPoints: Int) {
    NOVICE("初级读者", 0),
    INTERMEDIATE("中级读者", 1000),
    ADVANCED("高级读者", 5000),
    EXPERT("阅读达人", 20000),
    MASTER("阅读大师", 50000)
}

data class Achievement(
    val id: Long = 0,
    val type: AchievementType,
    val title: String,
    val description: String,
    val iconName: String,
    val points: Int,
    val isUnlocked: Boolean = false,
    val unlockedTime: Long? = null
)

data class ReadingGoal(
    val id: Long = 0,
    val goalType: GoalType,
    val targetValue: Int,
    val currentValue: Int = 0,
    val startDate: Long,
    val endDate: Long,
    val isCompleted: Boolean = false
)

enum class GoalType {
    DAILY_READING_TIME,
    WEEKLY_BOOKS_FINISHED,
    MONTHLY_READING_TIME,
    YEARLY_BOOKS_READ
}

data class UserProfile(
    val userId: String = "",
    val totalReadingTimeMinutes: Long = 0,
    val totalBooksFinished: Int = 0,
    val totalChaptersRead: Int = 0,
    val totalWordsRead: Long = 0,
    val readingPoints: Int = 0,
    val level: ReaderLevel = ReaderLevel.NOVICE,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastReadDate: Long? = null
)