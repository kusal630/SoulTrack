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


package io.github.dailytrack.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.data.db.entity.*
import io.github.dailytrack.data.repository.*
import io.github.dailytrack.engine.*
import io.github.dailytrack.service.PomodoroForegroundService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as SoulTrackApp).database
    private val sessionRepo = SessionRepository(db.sessionDao())
    private val categoryRepo = CategoryRepository(db.categoryDao())
    private val insightRepo = InsightRepository(db.insightDao())
    private val todoRepo = TodoRepository(db.todoDao(), db.subtaskDao())
    private val pomodoroRepo = PomodoroRepository(db.pomodoroSessionDao())
    private val savedQuoteRepo = SavedQuoteRepository(db.savedQuoteDao())
    private val habitRepo = HabitRepository(db.habitDao())
    private val routineEngine = RoutineLoopEngine()

    private val timeEngine = TimeCoverageEngine()
    private val growthEngine = GrowthEngine()
    private val insightEngine = InsightEngine()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _refreshTick = MutableStateFlow(0)
    private var followsToday = true

    private val _activeSession = MutableStateFlow<SessionEntity?>(null)
    val activeSession: StateFlow<SessionEntity?> = _activeSession

    private val _todaySessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val todaySessions: StateFlow<List<SessionEntity>> = _todaySessions

    private val _todayCoverage = MutableStateFlow<TimeCoverage?>(null)
    val todayCoverage: StateFlow<TimeCoverage?> = _todayCoverage

    private val _categories = MutableStateFlow<Map<Long, CategoryEntity>>(emptyMap())
    val categories: StateFlow<Map<Long, CategoryEntity>> = _categories

    private val _growthScore = MutableStateFlow(0.0)
    val growthScore: StateFlow<Double> = _growthScore

    private val _growthResult = MutableStateFlow<GrowthScoreResult?>(null)
    val growthResult: StateFlow<GrowthScoreResult?> = _growthResult

    private val _activeInsights = MutableStateFlow<List<InsightEntity>>(emptyList())
    val activeInsights: StateFlow<List<InsightEntity>> = _activeInsights

    private val _loopStatus = MutableStateFlow<LoopDetectionResult?>(null)
    val loopStatus: StateFlow<LoopDetectionResult?> = _loopStatus

    private val _activeTodos = MutableStateFlow<List<TodoEntity>>(emptyList())
    val activeTodos: StateFlow<List<TodoEntity>> = _activeTodos

    private val _completedTodos = MutableStateFlow<List<TodoEntity>>(emptyList())
    val completedTodos: StateFlow<List<TodoEntity>> = _completedTodos

    private val _activeTodoCount = MutableStateFlow(0)
    val activeTodoCount: StateFlow<Int> = _activeTodoCount

    private val _completedTodoCount = MutableStateFlow(0)
    val completedTodoCount: StateFlow<Int> = _completedTodoCount

    private val _highPriorityCount = MutableStateFlow(0)
    val highPriorityCount: StateFlow<Int> = _highPriorityCount

    private val _overdueCount = MutableStateFlow(0)
    val overdueCount: StateFlow<Int> = _overdueCount

    private val _todayPomodoros = MutableStateFlow<List<PomodoroSessionEntity>>(emptyList())
    val todayPomodoros: StateFlow<List<PomodoroSessionEntity>> = _todayPomodoros

    private val _activePomodoro = MutableStateFlow<PomodoroSessionEntity?>(null)
    val activePomodoro: StateFlow<PomodoroSessionEntity?> = _activePomodoro

    private val _savedQuotes = MutableStateFlow<List<SavedQuoteEntity>>(emptyList())
    val savedQuotes: StateFlow<List<SavedQuoteEntity>> = _savedQuotes

    private val _habits = MutableStateFlow<List<HabitEntity>>(emptyList())
    val habits: StateFlow<List<HabitEntity>> = _habits

    private val _habitCompletions = MutableStateFlow<Map<Long, List<LocalDate>>>(emptyMap())
    val habitCompletions: StateFlow<Map<Long, List<LocalDate>>> = _habitCompletions

    private val _weeklyStats = MutableStateFlow(WeeklyStats())
    val weeklyStats: StateFlow<WeeklyStats> = _weeklyStats

    private val _dailyProductivity = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val dailyProductivity: StateFlow<List<Pair<String, Float>>> = _dailyProductivity

    init {
        viewModelScope.launch {
            categoryRepo.initializeDefaults()
        }

        viewModelScope.launch {
            categoryRepo.getAllCategories().collect { cats ->
                _categories.value = cats.associateBy { it.id }
            }
        }

        viewModelScope.launch {
            sessionRepo.getActiveSession().collect { session ->
                _activeSession.value = session
            }
        }

        viewModelScope.launch {
            todoRepo.getActiveTodos().collect { todos ->
                _activeTodos.value = todos
            }
        }

        viewModelScope.launch {
            todoRepo.getCompletedTodos().collect { todos ->
                _completedTodos.value = todos
            }
        }

        viewModelScope.launch {
            todoRepo.getHighPriorityCount().collect { count ->
                _highPriorityCount.value = count
            }
        }

        viewModelScope.launch {
            combine(_selectedDate, _refreshTick) { date, _ -> date }
                .flatMapLatest { date ->
                    val zone = ZoneId.systemDefault()
                    val (dayStart, dayEnd) = getDayRange(date, zone)
                    combine(
                        sessionRepo.getSessionsForDay(dayStart, dayEnd),
                        pomodoroRepo.getPomodorosForDay(dayStart, dayEnd),
                        todoRepo.getTodayActiveTodoCount(dayStart, dayEnd),
                        todoRepo.getTodayCompletedTodoCount(dayStart, dayEnd),
                        _categories
                    ) { sessions, pomodoros, activeCount, completedCount, cats ->
                        DaySnapshot(date, zone, sessions, pomodoros, activeCount, completedCount, cats)
                    }
                }
                .collect { snapshot ->
                    _todaySessions.value = snapshot.sessions
                    _todayPomodoros.value = snapshot.pomodoros
                    _activeTodoCount.value = snapshot.activeCount
                    _completedTodoCount.value = snapshot.completedCount
                    _todayCoverage.value = timeEngine.calculateCoverage(
                        snapshot.sessions, snapshot.categories, snapshot.date, snapshot.zone
                    )
                    calculateGrowthScore()
                    detectLoop()
                    generateInsights()
                    calculateDailyProductivity()
                    calculateWeeklyStats()
                }
        }

        viewModelScope.launch {
            savedQuoteRepo.getAllSavedQuotes().collect { quotes ->
                _savedQuotes.value = quotes
            }
        }

        viewModelScope.launch {
            habitRepo.getAllHabits().collect { habits ->
                _habits.value = habits.filter { !it.isArchived }
            }
        }

        viewModelScope.launch {
            val today = LocalDate.now()
            habitRepo.getAllCompletionsForDateRange(today.minusDays(364), today).collect { entries ->
                _habitCompletions.value = entries.groupBy(
                    keySelector = { it.habitId },
                    valueTransform = {
                        Instant.ofEpochMilli(it.completedDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                )
            }
        }

        viewModelScope.launch {
            pomodoroRepo.getActivePomodoro().collect { pomodoro ->
                _activePomodoro.value = pomodoro
            }
        }

        viewModelScope.launch {
            tick()
            while (true) {
                kotlinx.coroutines.delay(30_000L)
                tick()
            }
        }
    }

    private suspend fun tick() {
        val today = LocalDate.now()
        if (followsToday && _selectedDate.value != today) {
            _selectedDate.value = today
        }

        val now = System.currentTimeMillis()
        if (_activeSession.value != null || _todaySessions.value.any { it.isActive }) {
            _todayCoverage.value = timeEngine.calculateCoverage(
                _todaySessions.value, _categories.value, _selectedDate.value, ZoneId.systemDefault(), now = now
            )
        }

        val overdueCount = todoRepo.getOverdueCount(now).first()
        _overdueCount.value = overdueCount

        reconcileStalePomodoro(now)
    }

    private suspend fun reconcileStalePomodoro(now: Long) {
        runCatching {
            val active = pomodoroRepo.getActivePomodoroSync() ?: return
            val plannedEnd = active.startTime + active.durationMinutes * 60_000L
            val prefs = getApplication<Application>()
                .getSharedPreferences("pom_widget_prefs", Context.MODE_PRIVATE)
            val serviceRunning = prefs.getBoolean("is_running", false)
            val staleByTime = now > plannedEnd + 30_000L
            val staleByService = !serviceRunning && now > active.startTime + 60_000L
            if (staleByTime || staleByService) {
                pomodoroRepo.completePomodoro(active.id, minOf(now, plannedEnd))
            }
        }
    }

    private data class DaySnapshot(
        val date: LocalDate,
        val zone: ZoneId,
        val sessions: List<SessionEntity>,
        val pomodoros: List<PomodoroSessionEntity>,
        val activeCount: Int,
        val completedCount: Int,
        val categories: Map<Long, CategoryEntity>
    )

    fun selectDate(date: LocalDate) {
        followsToday = date == LocalDate.now()
        _selectedDate.value = date
    }

    fun startSession(title: String, categoryId: Long?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            sessionRepo.stopActiveSession(now)

            val cats = _categories.value
            val category = categoryId?.let { cats[it] }
            val sessionType = when (category?.type) {
                "SLEEP" -> "SLEEP"
                "EXERCISE" -> "EXERCISE"
                "LEARNING" -> "LEARNING"
                "SOCIAL" -> "SOCIAL"
                "RECOVERY" -> "REST"
                else -> "ACTIVITY"
            }

            val session = SessionEntity(
                title = title.ifBlank { category?.name ?: "Active Session" },
                categoryId = if (categoryId == 0L) null else categoryId,
                type = sessionType,
                startTime = now,
                isActive = true,
                source = "TIMER",
                timezoneId = ZoneId.systemDefault().id
            )
            sessionRepo.insert(session)
        }
    }

    fun stopSession() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            sessionRepo.stopActiveSession(now)
        }
    }

    fun reloadAll() {
        _refreshTick.value = _refreshTick.value + 1
    }

    fun addCustomCategory(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val category = CategoryEntity(
                name = name.trim(),
                type = "PRODUCTIVE",
                growthContribution = "MEDIUM",
                color = 0xFF757575,
                icon = "custom"
            )
            val id = categoryRepo.insert(category)
            onCreated(id)
        }
    }

    fun dismissInsight(id: Long) {
        viewModelScope.launch {
            insightRepo.dismiss(id)
        }
    }

    fun addTodo(title: String, description: String, categoryId: Long?, deadline: Long?, estimatedMinutes: Int, priority: Int = 0) {
        viewModelScope.launch {
            val todo = TodoEntity(
                title = title,
                description = description,
                categoryId = categoryId,
                deadline = deadline,
                estimatedMinutes = estimatedMinutes,
                priority = priority
            )
            todoRepo.insert(todo)
        }
    }

    fun completeTodo(id: Long) {
        viewModelScope.launch {
            todoRepo.completeTodo(id)
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoRepo.delete(todo)
            todoRepo.deleteAllSubtasksForTodo(todo.id)
        }
    }

    fun deleteCompletedTodos() {
        viewModelScope.launch {
            val completed = _completedTodos.value
            completed.forEach { todo ->
                todoRepo.delete(todo)
                todoRepo.deleteAllSubtasksForTodo(todo.id)
            }
        }
    }

    fun updateTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoRepo.update(todo)
        }
    }

    fun startPomodoro(
        todoId: Long?,
        categoryId: Long?,
        durationMinutes: Int,
        title: String = "",
        breakDurationMinutes: Int = 5
    ) {
        val app = getApplication<Application>()
        val intent = Intent(app, PomodoroForegroundService::class.java).apply {
            putExtra(PomodoroForegroundService.EXTRA_START_TIME, System.currentTimeMillis())
            putExtra(PomodoroForegroundService.EXTRA_WORK_DURATION, durationMinutes)
            putExtra(PomodoroForegroundService.EXTRA_BREAK_DURATION, breakDurationMinutes)
            putExtra(PomodoroForegroundService.EXTRA_LONG_BREAK_DURATION, 15)
            putExtra(PomodoroForegroundService.EXTRA_TODO_TITLE, title)
            putExtra(PomodoroForegroundService.EXTRA_TODO_ID, todoId ?: -1L)
            putExtra(PomodoroForegroundService.EXTRA_CATEGORY_ID, categoryId ?: -1L)
            putExtra(PomodoroForegroundService.EXTRA_IS_BREAK, false)
            putExtra(PomodoroForegroundService.EXTRA_IS_LONG_BREAK, false)
            putExtra(PomodoroForegroundService.EXTRA_SESSION_NUMBER, 0)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        }
    }

    fun stopPomodoro() {
        val app = getApplication<Application>()
        val intent = Intent(app, PomodoroForegroundService::class.java).apply {
            action = PomodoroForegroundService.ACTION_STOP
        }
        runCatching { app.startService(intent) }
    }

    fun addHabit(name: String, icon: String, color: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            habitRepo.insertHabit(
                HabitEntity(
                    name = name.trim(),
                    icon = icon.ifBlank { "⭐" },
                    color = color.ifBlank { "#10B981" }
                )
            )
        }
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            val completedToday = _habitCompletions.value[habitId]?.contains(LocalDate.now()) == true
            if (completedToday) habitRepo.uncompleteHabit(habitId)
            else habitRepo.completeHabit(habitId)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch { habitRepo.deleteHabit(habit) }
    }

    fun saveQuote(text: String, author: String) {
        viewModelScope.launch {
            val existing = savedQuoteRepo.getQuoteByText(text)
            if (existing == null) {
                savedQuoteRepo.insert(SavedQuoteEntity(text = text, author = author))
            }
        }
    }

    fun deleteSavedQuote(quote: SavedQuoteEntity) {
        viewModelScope.launch {
            savedQuoteRepo.delete(quote)
        }
    }

    fun isQuoteSaved(text: String): Boolean {
        return _savedQuotes.value.any { it.text == text }
    }

    fun getSubtasksForTodo(todoId: Long): Flow<List<SubtaskEntity>> {
        return todoRepo.getSubtasksForTodo(todoId)
    }

    fun addSubtask(todoId: Long, title: String) {
        viewModelScope.launch {
            todoRepo.insertSubtask(
                SubtaskEntity(
                    todoId = todoId,
                    title = title,
                    sortOrder = 0
                )
            )
        }
    }

    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            todoRepo.setSubtaskCompleted(subtask.id, !subtask.isCompleted)
        }
    }

    fun deleteSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            todoRepo.deleteSubtask(subtask)
        }
    }

    private suspend fun calculateGrowthScore() {
        val zone = ZoneId.systemDefault()
        val today = _selectedDate.value
        val (dayStart, dayEnd) = getDayRange(today, zone)
        val now = System.currentTimeMillis()

        val sessions = sessionRepo.getSessionsForDaySync(dayStart, dayEnd)
        val cats = _categories.value

        var learningMinutes = 0.0
        var productiveMinutes = 0.0
        var exerciseMinutes = 0.0
        var socialMinutes = 0.0
        var sleepHours = 0.0

        for (session in sessions) {
            val duration = sessionMinutesWithinDay(session, dayStart, dayEnd, now)
            if (duration <= 0.0) continue
            val cat = session.categoryId?.let { cats[it] }
            val catType = cat?.type ?: session.type

            when (catType) {
                "LEARNING" -> learningMinutes += duration
                "PRODUCTIVE", "ACTIVITY" -> productiveMinutes += duration
                "EXERCISE" -> exerciseMinutes += duration
                "SOCIAL" -> socialMinutes += duration
                "SLEEP" -> sleepHours += duration / 60.0
            }
        }

        var consecutiveDaysActive = 0
        for (i in 0..6) {
            val date = today.minusDays(i.toLong())
            val (dStart, dEnd) = getDayRange(date, zone)
            if (hasTrackedTime(dStart, dEnd, now)) {
                consecutiveDaysActive++
            } else {
                break
            }
        }

        val noveltyScore = when {
            sessions.isEmpty() -> 0.0
            sessions.size > 5 -> 0.9
            sessions.size > 3 -> 0.7
            sessions.size > 1 -> 0.5
            else -> 0.3
        }

        val result = growthEngine.calculateGrowthScore(
            learningMinutes = learningMinutes,
            productiveMinutes = productiveMinutes,
            exerciseMinutes = exerciseMinutes,
            sleepHours = sleepHours,
            socialMinutes = socialMinutes,
            noveltyScore = noveltyScore,
            consecutiveDaysActive = consecutiveDaysActive
        )

        _growthScore.value = result.totalScore
        _growthResult.value = result
    }

    private suspend fun hasTrackedTime(dayStart: Long, dayEnd: Long, now: Long): Boolean {
        val sessions = sessionRepo.getSessionsForDaySync(dayStart, dayEnd)
        val sessionTracked = sessions.any { session ->
            val start = maxOf(session.startTime, dayStart)
            sessionEndWithinDay(session, dayEnd, now) > start
        }
        if (sessionTracked) return true
        return pomodoroRepo.getPomodorosForDaySync(dayStart, dayEnd).any { it.isCompleted }
    }

    private fun sessionEndWithinDay(session: SessionEntity, dayEnd: Long, now: Long): Long = when {
        session.isActive -> minOf(now, dayEnd)
        session.endTime != null -> minOf(session.endTime, dayEnd)
        else -> dayEnd
    }

    private fun sessionMinutesWithinDay(
        session: SessionEntity,
        dayStart: Long,
        dayEnd: Long,
        now: Long
    ): Double {
        val start = maxOf(session.startTime, dayStart)
        val end = sessionEndWithinDay(session, dayEnd, now)
        return ((end - start).coerceAtLeast(0L)) / 60000.0
    }

    private fun sessionMinutesWithinPomodoro(pomodoro: PomodoroSessionEntity): Double {
        val end = pomodoro.endTime ?: (pomodoro.startTime + pomodoro.durationMinutes * 60_000L)
        return ((end - pomodoro.startTime).coerceAtLeast(0L)) / 60000.0
    }

    private suspend fun detectLoop() {
        val zone = ZoneId.systemDefault()
        val today = _selectedDate.value
        val now = System.currentTimeMillis()

        val dailyVectors = mutableListOf<Map<String, Double>>()
        val growthScores = mutableListOf<Double>()
        val noveltyScores = mutableListOf<Double>()
        val recentCategorySets = mutableListOf<Set<Long>>()
        var runningStreak = 0

        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val (dayStart, dayEnd) = getDayRange(date, zone)
            val sessions = sessionRepo.getSessionsForDaySync(dayStart, dayEnd)

            val categoryDurations = mutableMapOf<Long, Long>()
            val dayCategories = mutableSetOf<Long>()
            var learningMinutes = 0.0
            var productiveMinutes = 0.0
            var exerciseMinutes = 0.0
            var socialMinutes = 0.0
            var sleepHours = 0.0

            for (session in sessions) {
                val durationMs = (sessionEndWithinDay(session, dayEnd, now) - maxOf(session.startTime, dayStart))
                    .coerceAtLeast(0L)
                if (durationMs <= 0L) continue
                session.categoryId?.let { catId ->
                    categoryDurations[catId] = (categoryDurations[catId] ?: 0L) + durationMs
                    dayCategories.add(catId)
                }
                val catType = session.categoryId?.let { _categories.value[it]?.type } ?: session.type
                val minutes = durationMs / 60000.0
                when (catType) {
                    "LEARNING" -> learningMinutes += minutes
                    "PRODUCTIVE", "ACTIVITY" -> productiveMinutes += minutes
                    "EXERCISE" -> exerciseMinutes += minutes
                    "SOCIAL" -> socialMinutes += minutes
                    "SLEEP" -> sleepHours += minutes / 60.0
                }
            }

            if (categoryDurations.isNotEmpty() || sessions.isNotEmpty()) runningStreak++ else runningStreak = 0

            val vector = routineEngine.calculateDailyRoutineVector(sessions, categoryDurations)
            dailyVectors.add(vector)

            val totalDuration = categoryDurations.values.sum()
            val dayNovelty = routineEngine.calculateNoveltyScore(
                currentCategories = dayCategories,
                recentCategories = recentCategorySets.toList(),
                currentDuration = totalDuration
            )
            noveltyScores.add(dayNovelty)
            recentCategorySets.add(dayCategories)
            growthScores.add(
                growthEngine.calculateGrowthScore(
                    learningMinutes = learningMinutes,
                    productiveMinutes = productiveMinutes,
                    exerciseMinutes = exerciseMinutes,
                    sleepHours = sleepHours,
                    socialMinutes = socialMinutes,
                    noveltyScore = dayNovelty,
                    consecutiveDaysActive = runningStreak
                ).totalScore
            )
        }

        _loopStatus.value = routineEngine.detectLoop(dailyVectors, growthScores, noveltyScores)
    }

    private suspend fun generateInsights() {
        val zone = ZoneId.systemDefault()
        val today = _selectedDate.value
        val (dayStart, dayEnd) = getDayRange(today, zone)
        val now = System.currentTimeMillis()

        val sessions = sessionRepo.getSessionsForDaySync(dayStart, dayEnd)
        val cats = _categories.value
        val pomodoros = pomodoroRepo.getPomodorosForDaySync(dayStart, dayEnd)

        val hasLearning = sessions.any { session ->
            val catType = session.categoryId?.let { cats[it]?.type }
            catType == "LEARNING" || session.type == "LEARNING"
        }

        val hasExercise = sessions.any { session ->
            val catType = session.categoryId?.let { cats[it]?.type }
            catType == "EXERCISE" || session.type == "EXERCISE"
        }

        var totalProductiveMinutes = 0.0
        var totalWastedMinutes = 0.0
        var totalBreakMinutes = 0.0

        for (session in sessions) {
            val duration = sessionMinutesWithinDay(session, dayStart, dayEnd, now)
            if (duration <= 0.0) continue
            val cat = session.categoryId?.let { cats[it] }
            val catType = cat?.type ?: session.type

            when (catType) {
                "PRODUCTIVE", "LEARNING", "ACTIVITY" -> totalProductiveMinutes += duration
                "WASTED" -> totalWastedMinutes += duration
                "NEUTRAL", "REST", "RECOVERY" -> totalBreakMinutes += duration
            }
        }

        for (pomodoro in pomodoros) {
            if (pomodoro.isCompleted && pomodoro.type == "BREAK") {
                totalBreakMinutes += sessionMinutesWithinPomodoro(pomodoro)
            }
        }

        val yesterday = today.minusDays(1)
        val (yesterdayStart, yesterdayEnd) = getDayRange(yesterday, zone)
        val yesterdaySessions = sessionRepo.getSessionsForDaySync(yesterdayStart, yesterdayEnd)
        var yesterdayWastedMinutes = 0.0
        for (session in yesterdaySessions) {
            val cat = session.categoryId?.let { cats[it] }
            val catType = cat?.type ?: session.type
            if (catType == "WASTED") {
                yesterdayWastedMinutes += sessionMinutesWithinDay(session, yesterdayStart, yesterdayEnd, now)
            }
        }

        val loop = _loopStatus.value
        val growthScore = _growthScore.value

        val templates = insightEngine.generateInsights(
            hasLearningToday = hasLearning,
            growthScoreTrend = listOf(growthScore),
            wastedTimeRatio = if (totalProductiveMinutes > 0) totalWastedMinutes / totalProductiveMinutes else 0.0,
            productiveRatio = if (totalProductiveMinutes + totalWastedMinutes > 0) totalProductiveMinutes / (totalProductiveMinutes + totalWastedMinutes) else 0.0,
            sleepDebtHours = 0.0,
            lowHydration = false,
            lowProteinDays = 0,
            lowFiberDays = 0,
            routineSimilarityDays = loop?.consecutiveDays ?: 0,
            noNewActivitiesDays = if (loop?.isLoopDetected == true) 5 else 0,
            noMovementToday = !hasExercise,
            noReflectionToday = false,
            exerciseMinutes = sessions.filter { it.type == "EXERCISE" || cats[it.categoryId]?.type == "EXERCISE" }
                .sumOf { sessionMinutesWithinDay(it, dayStart, dayEnd, now) },
            sleepHours = sessions.filter { it.type == "SLEEP" || cats[it.categoryId]?.type == "SLEEP" }
                .sumOf { sessionMinutesWithinDay(it, dayStart, dayEnd, now) / 60.0 },
            fatigueLevel = 2,
            severeSymptoms = emptyList(),
            maintenanceMode = false
        )

        val smartInsights = generateSmartInsights(
            totalProductiveMinutes = totalProductiveMinutes,
            totalWastedMinutes = totalWastedMinutes,
            totalBreakMinutes = totalBreakMinutes,
            yesterdayWastedMinutes = yesterdayWastedMinutes,
            activeTodoCount = _activeTodoCount.value,
            completedTodoCount = _completedTodoCount.value,
            pomodoroCount = pomodoros.count { it.type == "WORK" && it.isCompleted }
        )

        for (template in templates + smartInsights) {
            val insight = insightEngine.createInsightEntity(template)
            insightRepo.insertIfNotCoolingDown(insight)
        }
    }

    private fun generateSmartInsights(
        totalProductiveMinutes: Double,
        totalWastedMinutes: Double,
        totalBreakMinutes: Double,
        yesterdayWastedMinutes: Double,
        activeTodoCount: Int,
        completedTodoCount: Int,
        pomodoroCount: Int
    ): List<InsightEngine.InsightTemplate> {
        val insights = mutableListOf<InsightEngine.InsightTemplate>()

        if (totalWastedMinutes > totalProductiveMinutes && totalProductiveMinutes > 0) {
            insights.add(InsightEngine.InsightTemplate(
                title = "Time Waste Alert",
                message = "You've wasted ${totalWastedMinutes.toInt()} minutes today, which is more than your productive time (${totalProductiveMinutes.toInt()} min). Try to refocus on your goals.",
                severity = "CRITICAL",
                category = "PRODUCTIVITY",
                actionLabel = "Start a productive session"
            ))
        }

        if (totalWastedMinutes > yesterdayWastedMinutes && yesterdayWastedMinutes > 0) {
            insights.add(InsightEngine.InsightTemplate(
                title = "Increasing Waste Pattern",
                message = "Today's wasted time (${totalWastedMinutes.toInt()} min) is higher than yesterday (${yesterdayWastedMinutes.toInt()} min). Consider adjusting your routine.",
                severity = "WARNING",
                category = "PRODUCTIVITY",
                actionLabel = "Review your schedule"
            ))
        }

        if (activeTodoCount > 5) {
            insights.add(InsightEngine.InsightTemplate(
                title = "Too Many Pending Tasks",
                message = "You have $activeTodoCount pending tasks. Focus on completing a few before adding more.",
                severity = "CAUTION",
                category = "TODO",
                actionLabel = "Complete a task"
            ))
        }

        if (completedTodoCount == 0 && activeTodoCount > 0) {
            insights.add(InsightEngine.InsightTemplate(
                title = "No Tasks Completed Today",
                message = "You haven't completed any tasks today. Try to finish at least one task.",
                severity = "WARNING",
                category = "TODO",
                actionLabel = "Complete a task"
            ))
        }

        if (pomodoroCount >= 8) {
            insights.add(InsightEngine.InsightTemplate(
                title = "Great Focus Today!",
                message = "You've completed $pomodoroCount pomodoros today. Keep up the excellent work!",
                severity = "INFO",
                category = "PRODUCTIVITY",
                actionLabel = "Continue working"
            ))
        } else if (pomodoroCount < 3 && totalProductiveMinutes > 60) {
            insights.add(InsightEngine.InsightTemplate(
                title = "Try Pomodoro Technique",
                message = "Consider using the Pomodoro timer to structure your work. It can help improve focus.",
                severity = "INFO",
                category = "PRODUCTIVITY",
                actionLabel = "Start a pomodoro"
            ))
        }

        if (totalBreakMinutes > totalProductiveMinutes && totalProductiveMinutes > 0) {
            insights.add(InsightEngine.InsightTemplate(
                title = "Too Much Break Time",
                message = "You've spent ${totalBreakMinutes.toInt()} minutes on breaks, which is more than your productive time. Try to balance better.",
                severity = "WARNING",
                category = "PRODUCTIVITY",
                actionLabel = "Get back to work"
            ))
        }

        return insights
    }

    private suspend fun calculateWeeklyStats() {
        val zone = ZoneId.systemDefault()
        val today = _selectedDate.value
        val now = System.currentTimeMillis()

        var totalProductive = 0
        var totalWasted = 0
        var totalFree = 0
        var totalSleep = 0
        var totalSocial = 0
        var totalRecovery = 0
        var totalCompletedTodos = 0

        for (i in 0..6) {
            val date = today.minusDays(i.toLong())
            val (dayStart, dayEnd) = getDayRange(date, zone)
            val sessions = sessionRepo.getSessionsForDaySync(dayStart, dayEnd)
            val cats = _categories.value

            for (session in sessions) {
                val duration = sessionMinutesWithinDay(session, dayStart, dayEnd, now)
                if (duration <= 0.0) continue
                val cat = session.categoryId?.let { cats[it] }
                val catType = cat?.type ?: session.type
                val minutes = duration.toInt()

                when (catType) {
                    "PRODUCTIVE", "LEARNING", "ACTIVITY", "EXERCISE" -> totalProductive += minutes
                    "WASTED" -> totalWasted += minutes
                    "SLEEP" -> totalSleep += minutes
                    "SOCIAL" -> totalSocial += minutes
                    "RECOVERY", "REST" -> totalRecovery += minutes
                    else -> totalFree += minutes
                }
            }

            totalCompletedTodos += todoRepo.getCompletedCountInRange(dayStart, dayEnd)
        }

        _weeklyStats.value = WeeklyStats(
            totalProductiveMinutes = totalProductive,
            totalWastedMinutes = totalWasted,
            totalFreeMinutes = totalFree,
            totalSleepMinutes = totalSleep,
            totalSocialMinutes = totalSocial,
            totalRecoveryMinutes = totalRecovery,
            completedTodos = totalCompletedTodos,
            activeTodos = _activeTodoCount.value,
            averageProductivePerDay = totalProductive / 7
        )
    }

    private suspend fun calculateDailyProductivity() {
        val zone = ZoneId.systemDefault()
        val today = _selectedDate.value
        val cats = _categories.value
        val now = System.currentTimeMillis()
        val dailyData = mutableListOf<Pair<String, Float>>()

        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val (dayStart, dayEnd) = getDayRange(date, zone)
            val sessions = sessionRepo.getSessionsForDaySync(dayStart, dayEnd)

            var productiveMinutes = 0f

            for (session in sessions) {
                val duration = sessionMinutesWithinDay(session, dayStart, dayEnd, now)
                if (duration <= 0.0) continue
                val cat = session.categoryId?.let { cats[it] }
                val catType = cat?.type ?: session.type
                if (catType in listOf("PRODUCTIVE", "LEARNING", "ACTIVITY", "EXERCISE")) {
                    productiveMinutes += duration.toFloat()
                }
            }

            val dayOfWeek = date.dayOfWeek.value % 7
            val label = dayLabels[(dayOfWeek + 6) % 7]
            dailyData.add(label to productiveMinutes)
        }

        _dailyProductivity.value = dailyData
    }

    private fun getDayRange(date: LocalDate, zone: ZoneId, dayStartHour: Int = 0): Pair<Long, Long> {
        val start = date.atStartOfDay(zone).plusHours(dayStartHour.toLong()).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).plusHours(dayStartHour.toLong()).toInstant().toEpochMilli()
        return start to end
    }
}

data class WeeklyStats(
    val totalProductiveMinutes: Int = 0,
    val totalWastedMinutes: Int = 0,
    val totalFreeMinutes: Int = 0,
    val totalSleepMinutes: Int = 0,
    val totalSocialMinutes: Int = 0,
    val totalRecoveryMinutes: Int = 0,
    val completedTodos: Int = 0,
    val activeTodos: Int = 0,
    val averageProductivePerDay: Int = 0
) {
    val totalTrackedMinutes: Int
        get() = totalProductiveMinutes + totalWastedMinutes + totalFreeMinutes +
            totalSleepMinutes + totalSocialMinutes + totalRecoveryMinutes
}
