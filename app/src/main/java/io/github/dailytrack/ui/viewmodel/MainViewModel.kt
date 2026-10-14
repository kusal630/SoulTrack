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
import io.github.dailytrack.service.FocusDndManager
import io.github.dailytrack.service.TimerStateStore
import io.github.dailytrack.service.FocusSettings
import io.github.dailytrack.service.NoiseMonitor
import io.github.dailytrack.ui.components.weekStartFor
import io.github.dailytrack.ui.settings.AppFeatures
import io.github.dailytrack.ui.settings.ProductTargets
import io.github.dailytrack.widget.TimerWidgetProvider
import io.github.dailytrack.wellbeing.AndroidUsageCollector
import io.github.dailytrack.wellbeing.SharedPreferencesClassifierStore
import io.github.dailytrack.wellbeing.UsageAccess
import io.github.dailytrack.wellbeing.UsageClassifier
import io.github.dailytrack.wellbeing.UsageGapFiller
import io.github.dailytrack.wellbeing.UsageKind
import io.github.dailytrack.wellbeing.WellbeingSummary
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
    private val goalRepo = GoalRepository(db.goalDao())
    private val routineEngine = RoutineLoopEngine()

    private val timeEngine = TimeCoverageEngine()
    private val growthEngine = GrowthEngine()
    private val insightEngine = InsightEngine()
    private val brainLayer = BrainLayer()
    private val focusabilityEngine = FocusabilityEngine()
    private val switchEngine = SwitchCostEngine()

    private var behavior: BehaviorProfile = BehaviorProfile.coldStart()
    private var behaviorLoadedAt = 0L
    private var lastBrainRefresh = 0L

    private val _brainResult = MutableStateFlow<BrainResult?>(null)
    val brainResult: StateFlow<BrainResult?> = _brainResult

    private val _quietness = MutableStateFlow<Int?>(null)
    val quietness: StateFlow<Int?> = _quietness

    private val _goals = MutableStateFlow<List<GoalEntity>>(emptyList())
    val goals: StateFlow<List<GoalEntity>> = _goals

    private val _reminders = MutableStateFlow<List<ReminderEntity>>(emptyList())
    val reminders: StateFlow<List<ReminderEntity>> = _reminders

    private val _achievements = MutableStateFlow<List<AchievementEntity>>(emptyList())
    val achievements: StateFlow<List<AchievementEntity>> = _achievements

    private val _userStats = MutableStateFlow<UserStatsEntity?>(null)
    val userStats: StateFlow<UserStatsEntity?> = _userStats

    private val _weeklyReport = MutableStateFlow<Map<LocalDate, TimeCoverage>>(emptyMap())
    val weeklyReport: StateFlow<Map<LocalDate, TimeCoverage>> = _weeklyReport

    private val _productivityScore = MutableStateFlow(0f)
    val productivityScore: StateFlow<Float> = _productivityScore

    /**
     * Seconds of focus the day is measured against. Taken from the user's own
     * target when they set one, otherwise calibrated as the median of their
     * last tracked days - never a number we invented.
     */
    private val _focusGoalSeconds = MutableStateFlow(0L)
    val focusGoalSeconds: StateFlow<Long> = _focusGoalSeconds

    /** Session id -> the task a timer created for it, closed when the timer stops. */
    private val autoTaskBySession = java.util.concurrent.ConcurrentHashMap<Long, Long>()

    /**
     * Digital wellbeing: what the phone itself says about the hours Soul Track
     * never saw. Classified on-device; nothing is uploaded.
     */
    private val wellbeingClassifier by lazy {
        UsageClassifier(SharedPreferencesClassifierStore(getApplication()))
    }
    private val _wellbeing = MutableStateFlow<WellbeingSummary?>(null)
    val wellbeing: StateFlow<WellbeingSummary?> = _wellbeing
    private var lastWellbeingSyncAt = 0L

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

    /** Task-switch ("hallucination") report for the selected day. */
    private val _switchReport = MutableStateFlow(SwitchReport.EMPTY)
    val switchReport: StateFlow<SwitchReport> = _switchReport

    /** The individual jumps behind [switchReport], oldest first. */
    private val _switchEventsToday = MutableStateFlow<List<SwitchEvent>>(emptyList())
    val switchEventsToday: StateFlow<List<SwitchEvent>> = _switchEventsToday

    /** Switch count for each day of the current week, Monday first. */
    private val _weeklySwitchCounts = MutableStateFlow<List<Int>>(emptyList())
    val weeklySwitchCounts: StateFlow<List<Int>> = _weeklySwitchCounts

    /** Body-system cards derived from the user's own records. */
    private val _bodyCards = MutableStateFlow<List<BodySystemCard>>(emptyList())
    val bodyCards: StateFlow<List<BodySystemCard>> = _bodyCards

    /** Growth score for the last seven days, oldest first (the real trend). */
    private val _growthTrend = MutableStateFlow<List<Double>>(emptyList())
    val growthTrend: StateFlow<List<Double>> = _growthTrend

    /** Today's novelty on the 0..1 scale the growth engine expects. */
    private val _noveltyToday = MutableStateFlow(0.0)
    val noveltyToday: StateFlow<Double> = _noveltyToday
    private var cachedGrowthTargets: GrowthTargets? = null
    private var targetsCalibratedAt = 0L

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
        val app = getApplication<Application>()
        AppFeatures.load(app)
        ProductTargets.load(app)

        viewModelScope.launch {
            categoryRepo.initializeDefaults()
        }

        viewModelScope.launch {
            insightRepo.getActiveInsights().collect { _activeInsights.value = it }
        }

        viewModelScope.launch {
            categoryRepo.getAllCategories().collect { cats ->
                _categories.value = cats.associateBy { it.id }
            }
        }

        viewModelScope.launch {
            // First sync as soon as the app is open; the session collector
            // will pick the new rows up like any other tracked time.
            syncWellbeing()
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
                    detectLoop()
                    calculateGrowthScore()
                    detectSwitches()
                    generateInsights()
                    calculateDailyProductivity()
                    calculateWeeklyStats()
                    calculateWeeklyReport()
                    if (AppFeatures.healthEnabled) refreshBodyCards()
                }
        }

        viewModelScope.launch {
            savedQuoteRepo.getAllSavedQuotes().collect { quotes ->
                _savedQuotes.value = quotes
            }
        }

        viewModelScope.launch {
            goalRepo.getAllGoals().collect { _goals.value = it }
        }

        viewModelScope.launch {
            db.reminderDao().getAllReminders().collect { _reminders.value = it }
        }

        viewModelScope.launch {
            db.achievementDao().getAllAchievements().collect { _achievements.value = it }
        }

        viewModelScope.launch {
            db.achievementDao().getUserStats().collect { _userStats.value = it }
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

        // Brain layer: build the personal behaviour model, then keep the
        // recommendation fresh as tasks, focus and the environment change.
        viewModelScope.launch {
            rebuildBehavior()
            refreshBrain(force = true)
        }
        viewModelScope.launch {
            NoiseMonitor.reading.collect { reading ->
                _quietness.value = reading?.quietness
                refreshBrain()
            }
        }
        viewModelScope.launch {
            _activeTodos.collect { refreshBrain(force = true) }
        }
        viewModelScope.launch {
            _categories.collect { if (it.isNotEmpty()) refreshBrain(force = true) }
        }
    }

    /** Rebuilds the "how does this person behave at which hour" model. */
    private suspend fun rebuildBehavior() {
        val now = System.currentTimeMillis()
        val cutoff = now - 45L * 24 * 60 * 60 * 1000
        runCatching {
            val sessions = db.sessionDao().getSessionsBetweenSync(cutoff, now)
            val todos = db.todoDao().getCompletedTodosSince(cutoff) + _activeTodos.value
            behavior = BehaviorProfiler.build(
                sessions = sessions,
                todos = todos,
                categories = _categories.value,
                now = now
            )
            behaviorLoadedAt = now
            refreshBrain(force = true)
        }
    }

    private suspend fun refreshBrain(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastBrainRefresh < 30_000L) return
        if (!FocusSettings.isRecommendationsEnabled(getApplication())) {
            _brainResult.value = null
            return
        }
        lastBrainRefresh = now
        if (now - behaviorLoadedAt > 10 * 60_000L) {
            viewModelScope.launch { rebuildBehavior() }
        }

        val context = buildFocusContext(now)
        val focusability = focusabilityEngine.evaluate(context, behavior)
        val candidates = _activeTodos.value.map { it.toCandidate() }
        _brainResult.value = brainLayer.recommend(
            BrainInputs(
                now = now,
                candidates = candidates,
                focusability = focusability,
                behavior = behavior,
                mood = context.mood,
                energy = context.energy,
                activeTaskId = _activeSession.value?.todoId,
                abandonedTaskIds = _switchReport.value.abandonedTodoIds
            )
        )
    }

    private suspend fun buildFocusContext(now: Long): FocusContext {
        val zone = ZoneId.systemDefault()
        val active = _activeSession.value
        val completedToday = _todaySessions.value.filter { !it.isActive }
        val lastEnd = completedToday.mapNotNull { it.endTime }.maxOrNull()
        val minutesInCurrentSession = active?.let { ((now - it.startTime) / 60_000L).toInt() }
        val minutesSinceBreak = when {
            active != null && lastEnd != null -> ((now - lastEnd) / 60_000L).toInt()
            active != null -> minutesInCurrentSession
            lastEnd != null -> ((now - lastEnd) / 60_000L).toInt()
            else -> null
        }
        val recentMood = runCatching {
            getApplication<SoulTrackApp>().database.moodCheckInDao()
                .getMoodCheckInsInRange(now - 24L * 60 * 60 * 1000, now)
                .first()
                .maxByOrNull { it.timestamp }
        }.getOrNull()

        return FocusContext(
            hourOfDay = Instant.ofEpochMilli(now).atZone(zone).hour,
            dayOfWeek = Instant.ofEpochMilli(now).atZone(zone).dayOfWeek.value,
            minutesSinceBreak = minutesSinceBreak?.coerceAtLeast(0),
            minutesInCurrentSession = minutesInCurrentSession?.coerceAtLeast(0),
            quietness = NoiseMonitor.quietness(getApplication()),
            mood = recentMood?.mood,
            energy = recentMood?.energy
        )
    }

    /** Starts an unfinished task by id - used by the jump attention card. */
    fun startTodo(todoId: Long) {
        _activeTodos.value.firstOrNull { it.id == todoId }
            ?.let { startTask(it.toCandidate()) }
    }

    private fun TodoEntity.toCandidate() = TaskCandidate(
        id = id,
        title = title,
        priority = priority,
        deadline = deadline,
        estimatedMinutes = estimatedMinutes,
        categoryType = _categories.value[categoryId]?.type ?: "ACTIVITY",
        categoryId = categoryId,
        createdAt = createdAt
    )

    /** Starts a tracked, Do-Not-Disturb focus session for a recommended task. */
    fun startTask(task: TaskCandidate) {
        val fallbackCategoryId = _categories.value.values
            .firstOrNull { it.type == "PRODUCTIVE" && !it.archived }?.id
        val categoryId = _categories.value[task.categoryId]?.id ?: fallbackCategoryId
        startSession(
            title = task.title,
            categoryId = categoryId,
            autoDnd = task.requiresDeepFocus,
            todoId = task.id
        )
    }

    /** Marks a recommended task done and refreshes the next pick. */
    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            runCatching { todoRepo.completeTodo(taskId) }
            refreshBrain(force = true)
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
        syncWellbeing(now)
        refreshBrain()
    }

    /**
     * Fills the gaps in the day with what the phone itself recorded.
     *
     * Every stretch of foreground usage that is not already tracked becomes a
     * session, classified on-device as productive, neutral or waste. Time the
     * user never opened Soul Track for stops being a blind spot - and, by the
     * app's own rule, stops counting as unrecorded waste.
     */
    private suspend fun syncWellbeing(now: Long = System.currentTimeMillis()) {
        val app = getApplication<Application>()
        if (!UsageAccess.has(app)) {
            _wellbeing.value = null
            return
        }
        // Throttle: usage stats are not free, and the day does not change
        // shape every minute.
        if (now - lastWellbeingSyncAt < 15 * 60_000L) return
        lastWellbeingSyncAt = now

        runCatching {
            val zone = ZoneId.systemDefault()
            val windowStart = now - 24 * 60 * 60_000L
            val intervals = AndroidUsageCollector.collect(app, windowStart, now)
            if (intervals.isEmpty()) {
                _wellbeing.value = null
                return@runCatching
            }

            val tracked = sessionRepo.getSessionsInRangeSync(windowStart, now)
                .mapNotNull { session ->
                    val end = if (session.isActive) now else session.endTime ?: return@mapNotNull null
                    if (end <= session.startTime) return@mapNotNull null
                    java.time.Instant.ofEpochMilli(session.startTime) to
                        java.time.Instant.ofEpochMilli(end)
                }

            val labels = intervals.map { it.packageName to AndroidUsageCollector.describe(app, it.packageName).first }
                .toMap()

            val segments = UsageGapFiller.fillGaps(
                intervals = intervals,
                tracked = tracked,
                classifier = wellbeingClassifier,
                labels = labels
            )

            val alreadyTracked = tracked.sumOf { (a, b) ->
                java.time.Duration.between(a, b).toMillis().coerceAtLeast(0L)
            }
            val windowMillis = now - windowStart
            _wellbeing.value = WellbeingSummary.from(segments, windowMillis, alreadyTracked)

            val selfPackage = app.packageName
            for (segment in segments) {
                val pkg = intervals.firstOrNull {
                    it.start <= segment.start && segment.end <= it.end
                }?.packageName ?: continue
                if (pkg == selfPackage) continue

                // Never write over time that is already recorded.
                val overlaps = tracked.any { (a, b) ->
                    segment.start < b && a < segment.end
                }
                if (overlaps) continue

                val type = when (segment.kind) {
                    UsageKind.PRODUCTIVE -> "PRODUCTIVE"
                    UsageKind.WASTED -> "WASTED"
                    UsageKind.NEUTRAL -> "NEUTRAL"
                }
                sessionRepo.insert(
                    SessionEntity(
                        title = segment.label,
                        categoryId = null,
                        todoId = null,
                        type = type,
                        startTime = segment.start.toEpochMilli(),
                        endTime = segment.end.toEpochMilli(),
                        isActive = false,
                        source = "USAGE",
                        timezoneId = zone.id
                    )
                )
            }
        }
    }

    /** Re-labels one app from the wellbeing card; the model learns from it. */
    fun relabelUsageApp(packageName: String, label: UsageKind) {
        viewModelScope.launch {
            wellbeingClassifier.setOverride(packageName, label)
            lastWellbeingSyncAt = 0L
            syncWellbeing()
        }
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

    /**
     * Starts a tracked session and the foreground timer service from a single
     * start timestamp so the app, the widget and the notification all count the
     * exact same instant.
     */
    fun startSession(
        title: String,
        categoryId: Long?,
        autoDnd: Boolean = false,
        source: String = "TIMER",
        todoId: Long? = null
    ) {
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

            // Every recorded block is a piece of work, so a timer started
            // without a task still becomes one - it shows up in Tasks, counts
            // toward the ring and can be reviewed later.
            val linkedTodoId = todoId ?: autoCreateTaskFor(title, categoryId, now)

            val session = SessionEntity(
                title = title.ifBlank { category?.name ?: "Active Session" },
                categoryId = if (categoryId == 0L) null else categoryId,
                todoId = linkedTodoId,
                type = sessionType,
                startTime = now,
                isActive = true,
                source = source,
                timezoneId = ZoneId.systemDefault().id
            )
            val sessionId = sessionRepo.insert(session)
            if (todoId == null && linkedTodoId != null && linkedTodoId > 0L) {
                autoTaskBySession[sessionId] = linkedTodoId
            }

            val state = TimerStateStore.TimerState(
                isRunning = true,
                startTime = now,
                sessionId = sessionId,
                title = session.title,
                categoryName = category?.name ?: "",
                categoryType = category?.type ?: "",
                source = source,
                autoDnd = autoDnd || TimerStateStore.isFocusType(category?.type)
            )
            val app = getApplication<Application>()
            TimerStateStore.publish(app, state)
            TimerStateStore.startTimerService(app, state)
        }
    }

    /**
     * Stops the running session everywhere at once: service, widget,
     * notification, Do Not Disturb and the database.
     */
    fun stopSession() {
        val app = getApplication<Application>()
        val current = TimerStateStore.load(app)
        val sessionId = current.sessionId
        val isPomodoro = current.isPomodoro
        viewModelScope.launch {
            val accepted = TimerStateStore.requestStop(app, sessionId, isPomodoro)
            // Close immediately so the UI reacts without waiting for the service.
            if (sessionId >= 0L) {
                sessionRepo.stopSessionById(sessionId)
                autoTaskBySession.remove(sessionId)?.let { todoId ->
                    runCatching { todoRepo.completeTodo(todoId) }
                }
            } else {
                sessionRepo.stopActiveSession(System.currentTimeMillis())
            }
            if (!accepted) {
                FocusDndManager.restore(app)
                TimerStateStore.clear(app)
                runCatching { TimerWidgetProvider.updateWidgets(app) }
            }
        }
    }

    /**
     * Turns a bare timer title into a real task. Placeholder titles stay
     * task-less on purpose - a session called "Active Session" is not work
     * worth reviewing.
     */
    private suspend fun autoCreateTaskFor(title: String, categoryId: Long?, now: Long): Long? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.lowercase() in GENERIC_SESSION_TITLES) return null
        return todoRepo.insert(
            TodoEntity(
                title = trimmed,
                categoryId = categoryId?.takeIf { it > 0L },
                createdAt = now,
                updatedAt = now
            )
        )
    }

    /**
     * Backfilled work: a block the user already finished and only now wrote
     * down. Creates the task *and* the recorded time in one step so the ring,
     * growth, loops and insights all see it exactly like a tracked session.
     */
    fun logCompletedWork(title: String, minutes: Int, categoryId: Long?, startTimeMillis: Long? = null) {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || minutes <= 0) return
        viewModelScope.launch {
            val end = System.currentTimeMillis()
            val start = startTimeMillis ?: (end - minutes * 60_000L)
            val cat = categoryId?.let { _categories.value[it] }
            val type = when (cat?.type) {
                "SLEEP" -> "SLEEP"
                "EXERCISE" -> "EXERCISE"
                "LEARNING" -> "LEARNING"
                "SOCIAL" -> "SOCIAL"
                "RECOVERY" -> "REST"
                "PRODUCTIVE" -> "PRODUCTIVE"
                else -> "PRODUCTIVE"
            }
            val todoId = todoRepo.insert(
                TodoEntity(
                    title = trimmed,
                    categoryId = categoryId?.takeIf { it > 0L },
                    estimatedMinutes = minutes,
                    actualMinutes = minutes,
                    isCompleted = true,
                    completedAt = end
                )
            )
            sessionRepo.insert(
                SessionEntity(
                    title = trimmed,
                    categoryId = categoryId?.takeIf { it > 0L },
                    todoId = todoId,
                    type = type,
                    startTime = start,
                    endTime = end,
                    isActive = false,
                    source = "MANUAL",
                    timezoneId = ZoneId.systemDefault().id
                )
            )
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
        val minutes = minutesFor(sessions, _categories.value, dayStart, dayEnd, now)

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

        val result = growthEngineForToday().calculateGrowthScore(
            learningMinutes = minutes.learning,
            productiveMinutes = minutes.productive,
            exerciseMinutes = minutes.exercise,
            sleepHours = minutes.sleepHours,
            socialMinutes = minutes.social,
            // detectLoop() runs first and leaves today's novelty on the 0..1 scale.
            noveltyScore = _noveltyToday.value,
            consecutiveDaysActive = consecutiveDaysActive,
            targets = growthTargets(now)
        )

        _growthScore.value = result.totalScore
        _growthResult.value = result
    }

    companion object {
        private val GENERIC_SESSION_TITLES = setOf(
            "active session", "focus session", "pomodoro focus", "timer", "break"
        )
    }

    private data class DayMinutes(
        val learning: Double,
        val productive: Double,
        val exercise: Double,
        val social: Double,
        val sleepHours: Double
    ) {
        companion object {
            val EMPTY = DayMinutes(0.0, 0.0, 0.0, 0.0, 0.0)
        }
    }

    /** Minutes per growth component inside one day - real sessions only. */
    private fun minutesFor(
        sessions: List<SessionEntity>,
        cats: Map<Long, CategoryEntity>,
        dayStart: Long,
        dayEnd: Long,
        now: Long
    ): DayMinutes {
        var learning = 0.0
        var productive = 0.0
        var exercise = 0.0
        var social = 0.0
        var sleep = 0.0

        for (session in sessions) {
            val duration = sessionMinutesWithinDay(session, dayStart, dayEnd, now)
            if (duration <= 0.0) continue
            when (cats[session.categoryId]?.type ?: session.type) {
                "LEARNING" -> learning += duration
                "PRODUCTIVE", "ACTIVITY" -> productive += duration
                "EXERCISE" -> exercise += duration
                "SOCIAL" -> social += duration
                "SLEEP" -> sleep += duration / 60.0
            }
        }
        return DayMinutes(learning, productive, exercise, social, sleep)
    }

    /**
     * Growth weights for the layer the user actually enabled. With health off
     * the sleep/exercise/social share is redistributed across the
     * productivity components instead of silently scoring zeros.
     */
    private fun growthEngineForToday(): GrowthEngine =
        GrowthEngine(GrowthEngine.weightsFor(AppFeatures.healthEnabled))

    /**
     * Resolves what "enough" means for each growth component, in this order:
     * a target the user set in Settings, else the 75th percentile of their own
     * last 14 days (needs at least 3 tracked days), else the cold-start prior.
     */
    private suspend fun growthTargets(now: Long): GrowthTargets {
        cachedGrowthTargets?.let { cached ->
            if (now - targetsCalibratedAt < 6 * 3_600_000L) return cached
        }

        val zone = ZoneId.systemDefault()
        val learning = mutableListOf<Double>()
        val productive = mutableListOf<Double>()
        val exercise = mutableListOf<Double>()
        val social = mutableListOf<Double>()
        val sleep = mutableListOf<Double>()

        val (historyStart, _) = getDayRange(LocalDate.now().minusDays(13L), zone)
        val (historyEnd, _) = getDayRange(LocalDate.now(), zone)
        val byDay = sessionRepo.getSessionsInRangeSync(historyStart, historyEnd)
            .groupBy { session ->
                java.time.Instant.ofEpochMilli(session.startTime)
                    .atZone(zone).toLocalDate()
            }

        for (i in 0 until 14) {
            val (dayStart, dayEnd) = getDayRange(LocalDate.now().minusDays(i.toLong()), zone)
            val sessions = byDay[LocalDate.now().minusDays(i.toLong())].orEmpty()
            if (sessions.isEmpty()) continue
            val minutes = minutesFor(sessions, _categories.value, dayStart, dayEnd, now)
            learning += minutes.learning
            productive += minutes.productive
            exercise += minutes.exercise
            social += minutes.social
            if (minutes.sleepHours > 0.0) sleep += minutes.sleepHours
        }

        val userSleep = ProductTargets.sleepHours.toDouble()
        val sleepBand = when {
            userSleep > 0.0 -> (userSleep - 1.0)..(userSleep + 1.0)
            else -> medianOr(sleep, 7.5)?.let { (it - 1.0)..(it + 1.0) } ?: 6.0..9.0
        }

        val targets = GrowthTargets(
            learningMinutes = ProductTargets.learningMinutes.takeIf { it > 0 }?.toDouble()
                ?: percentileOr(learning, GrowthEngine.TARGET_LEARNING_MINUTES),
            productiveMinutes = ProductTargets.productiveMinutes.takeIf { it > 0 }?.toDouble()
                ?: percentileOr(productive, GrowthEngine.TARGET_PRODUCTIVE_MINUTES),
            exerciseMinutes = ProductTargets.exerciseMinutes.takeIf { it > 0 }?.toDouble()
                ?: percentileOr(exercise, GrowthEngine.TARGET_EXERCISE_MINUTES),
            socialMinutes = percentileOr(social, GrowthEngine.TARGET_SOCIAL_MINUTES),
            sleepBand = sleepBand
        )
        cachedGrowthTargets = targets
        targetsCalibratedAt = now
        return targets
    }

    /** 75th percentile of the days the user actually tracked, or [fallback]. */
    private fun percentileOr(values: List<Double>, fallback: Double): Double {
        val usable = values.filter { it > 0.0 }
        if (usable.size < 3) return fallback
        val sorted = usable.sorted()
        val index = ((sorted.size - 1) * 0.75).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index].coerceAtLeast(5.0)
    }

    private fun medianOr(values: List<Double>, fallback: Double): Double? {
        val usable = values.filter { it > 0.0 }
        if (usable.size < 3) return null
        val sorted = usable.sorted()
        return if (sorted.size % 2 == 1) sorted[sorted.size / 2]
        else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
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

        // One range read for the whole week instead of seven day reads - the
        // loop engine runs on every session change, so this is the hot path.
        val (weekStart, _) = getDayRange(today.minusDays(6L), zone)
        val (_, todayEnd) = getDayRange(today, zone)
        val weekSessions = sessionRepo.getSessionsInRangeSync(weekStart, todayEnd)
            .groupBy { session ->
                java.time.Instant.ofEpochMilli(session.startTime)
                    .atZone(zone).toLocalDate()
            }

        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val (dayStart, dayEnd) = getDayRange(date, zone)
            val sessions = weekSessions[date].orEmpty()

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

            // The loop engine speaks 0..100, the growth engine 0..1 - keep both.
            val dayNovelty01 = (dayNovelty / 100.0).coerceIn(0.0, 1.0)
            if (i == 0) _noveltyToday.value = dayNovelty01

            growthScores.add(
                growthEngineForToday().calculateGrowthScore(
                    learningMinutes = learningMinutes,
                    productiveMinutes = productiveMinutes,
                    exerciseMinutes = exerciseMinutes,
                    sleepHours = sleepHours,
                    socialMinutes = socialMinutes,
                    noveltyScore = dayNovelty01,
                    consecutiveDaysActive = runningStreak,
                    targets = growthTargets(now)
                ).totalScore
            )
        }

        _loopStatus.value = routineEngine.detectLoop(dailyVectors, growthScores, noveltyScores)
        _growthTrend.value = growthScores
    }

    /**
     * Finds every jump between work contexts today ("hallucination"
     * detection) and counts the jumps across the current week.
     */
    private suspend fun detectSwitches() {
        val zone = ZoneId.systemDefault()
        val today = _selectedDate.value
        val now = System.currentTimeMillis()

        val (dayStart, dayEnd) = getDayRange(today, zone)
        val sessions = sessionRepo.getSessionsForDaySync(dayStart, dayEnd)
        val blocks = sessions.map { switchBlockOf(it, zone, dayStart, dayEnd, now) }
        val events = switchEngine.withResumeTimes(switchEngine.detectSwitches(blocks), blocks)

        val completedToday = _completedTodos.value.filter { todo ->
            todo.completedAt?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today
            } == true
        }.mapNotNull { it.id }.toSet()

        _switchEventsToday.value = events
        _switchReport.value = switchEngine.summarize(
            events = events,
            blocks = blocks,
            trackedMillis = blocks.sumOf { it.durationMillis },
            completedTodoIds = completedToday,
            dayStartMillis = dayStart,
            dayEndMillis = minOf(dayEnd, now)
        )

        val monday = weekStartFor(today)
        _weeklySwitchCounts.value = (0 until 7).map { offset ->
            val date = monday.plusDays(offset.toLong())
            val (dStart, dEnd) = getDayRange(date, zone)
            val daySessions = sessionRepo.getSessionsForDaySync(dStart, dEnd)
            val dayBlocks = daySessions.map { switchBlockOf(it, zone, dStart, dEnd, now) }
            switchEngine.detectSwitches(dayBlocks).size
        }
    }

    /** One stretch of work, clipped to the day it is being judged in. */
    private fun switchBlockOf(
        session: SessionEntity,
        zone: ZoneId,
        dayStart: Long,
        dayEnd: Long,
        now: Long
    ): SwitchBlock {
        val start = maxOf(session.startTime, dayStart)
        val end = when {
            session.isActive -> minOf(now, dayEnd)
            session.endTime != null -> minOf(session.endTime!!, dayEnd)
            else -> dayEnd
        }
        return SwitchBlock(
            todoId = session.todoId,
            contextKey = switchEngine.contextKeyOf(session.todoId, session.categoryId, session.title),
            startMillis = start,
            endMillis = maxOf(end, start),
            type = _categories.value[session.categoryId]?.type ?: session.type
        )
    }

    private suspend fun generateInsights() {
        val zone = ZoneId.systemDefault()
        val today = _selectedDate.value
        val (dayStart, dayEnd) = getDayRange(today, zone)
        val now = System.currentTimeMillis()

        val sessions = sessionRepo.getSessionsForDaySync(dayStart, dayEnd)
        val cats = _categories.value
        val pomodoros = pomodoroRepo.getPomodorosForDaySync(dayStart, dayEnd)
        val healthOn = AppFeatures.healthEnabled

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
            val catType = cats[session.categoryId]?.type ?: session.type

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
            val catType = cats[session.categoryId]?.type ?: session.type
            if (catType == "WASTED") {
                yesterdayWastedMinutes += sessionMinutesWithinDay(session, yesterdayStart, yesterdayEnd, now)
            }
        }

        // Everything below is read from the user's own records; a missing
        // record means "no claim", never a borrowed default.
        val loop = _loopStatus.value
        val recent = recentSessions(now, zone)
        val sleepDebt = if (healthOn) sleepDebtFrom(recent, zone) else 0.0
        val waterTodayMl = if (healthOn && ProductTargets.waterMl > 0) {
            db.drinkEntryDao().getTotalWaterMlForDay(dayStart, dayEnd).first()
        } else null
        val lowHydration = healthOn && ProductTargets.waterMl > 0 &&
            (waterTodayMl ?: 0.0) < ProductTargets.waterMl * 0.5
        val (lowProteinDays, lowFiberDays) =
            if (healthOn) nutritionDeficitDays(zone) else 0 to 0
        val noNewActivitiesDays = daysWithoutNewCategory(recent, zone)
        val noReflectionToday = reflectionMissing(zone, now)
        val fatigueLevel = currentFatigue(now)
        val severeSymptoms = if (healthOn) {
            db.symptomEntryDao().getSevereSymptoms(dayStart).map { it.symptomType }
        } else emptyList()

        val minutes = minutesFor(sessions, cats, dayStart, dayEnd, now)

        val templates = insightEngine.generateInsights(
            hasLearningToday = hasLearning,
            growthScoreTrend = _growthTrend.value,
            wastedTimeRatio = if (totalProductiveMinutes > 0) totalWastedMinutes / totalProductiveMinutes else 0.0,
            productiveRatio = if (totalProductiveMinutes + totalWastedMinutes > 0) totalProductiveMinutes / (totalProductiveMinutes + totalWastedMinutes) else 0.0,
            sleepDebtHours = sleepDebt,
            lowHydration = lowHydration,
            lowProteinDays = lowProteinDays,
            lowFiberDays = lowFiberDays,
            routineSimilarityDays = loop?.consecutiveDays ?: 0,
            noNewActivitiesDays = noNewActivitiesDays,
            noMovementToday = healthOn && !hasExercise,
            noReflectionToday = noReflectionToday,
            exerciseMinutes = minutes.exercise,
            sleepHours = minutes.sleepHours,
            fatigueLevel = fatigueLevel,
            severeSymptoms = severeSymptoms,
            maintenanceMode = ProductTargets.maintenanceMode,
            switchReport = _switchReport.value.takeIf { it.switchCount > 0 }
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

        val all = templates + smartInsights
        val healthCategories = setOf("SLEEP", "HYDRATION", "NUTRITION", "EXERCISE", "SAFETY")
        for (template in if (healthOn) all else all.filter { it.category !in healthCategories }) {
            val insight = insightEngine.createInsightEntity(template)
            insightRepo.insertIfNotCoolingDown(insight)
        }
    }

    /** Fourteen days of sessions in one query - the base for every trend below. */
    private suspend fun recentSessions(now: Long, zone: ZoneId): List<SessionEntity> {
        val from = now - 14L * 24 * 60 * 60 * 1000
        return db.sessionDao().getSessionsBetweenSync(from, now)
    }

    private fun sessionsOn(sessions: List<SessionEntity>, date: LocalDate, zone: ZoneId): List<SessionEntity> {
        val key = date.toString()
        return sessions.filter {
            Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate().toString() == key
        }
    }

    /**
     * Sleep debt against the user's own sleep target, or their own median
     * night when no target is set. Needs three tracked nights to say anything.
     */
    /** Sleep hours per night inside the [recent] window, most recent first. */
    private fun recentNights(recent: List<SessionEntity>, zone: ZoneId, depth: Int = 7): List<Double> {
        val cats = _categories.value
        val now = System.currentTimeMillis()
        return (1..depth).mapNotNull { daysAgo ->
            val date = LocalDate.now().minusDays(daysAgo.toLong())
            val (dayStart, dayEnd) = getDayRange(date, zone)
            minutesFor(sessionsOn(recent, date, zone), cats, dayStart, dayEnd, now)
                .sleepHours
                .takeIf { it > 0.0 }
        }
    }

    private fun sleepDebtFrom(recent: List<SessionEntity>, zone: ZoneId): Double {
        val nights = recentNights(recent, zone)
        if (nights.size < 3) return 0.0
        val target = ProductTargets.sleepHours.takeIf { it > 0f }?.toDouble()
            ?: medianOr(nights, 0.0)
            ?: return 0.0
        if (target <= 0.0) return 0.0
        return SleepEngine()
            .calculateSleepDebt(lastNightHours = nights.first(), recentNightHours = nights, targetHours = target)
            .debtHours
            .coerceAtLeast(0.0)
    }

    /** Days in the last week where food was logged below the user's own goal. */
    private suspend fun nutritionDeficitDays(zone: ZoneId): Pair<Int, Int> {
        val proteinTarget = ProductTargets.proteinG
        val fiberTarget = ProductTargets.fiberG
        if (proteinTarget <= 0 && fiberTarget <= 0) return 0 to 0

        val from = LocalDate.now().minusDays(6)
        val (fromMillis, toMillis) = getDayRange(from, zone)
        val entries = db.foodEntryDao().getFoodEntriesInRange(fromMillis, toMillis)
        val byDay = entries.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }

        var lowProtein = 0
        var lowFiber = 0
        for (daysAgo in 0..6) {
            val dayEntries = byDay[LocalDate.now().minusDays(daysAgo.toLong())] ?: continue
            if (proteinTarget > 0 && dayEntries.sumOf { it.proteinG } < proteinTarget * 0.75) lowProtein++
            if (fiberTarget > 0 && dayEntries.sumOf { it.fiberG } < fiberTarget * 0.75) lowFiber++
        }
        return lowProtein to lowFiber
    }

    /**
     * Consecutive days (ending today) that introduced no category the user had
     * not already used before. Pure history: 0 means something new happened.
     */
    private fun daysWithoutNewCategory(recent: List<SessionEntity>, zone: ZoneId): Int {
        val seen = mutableSetOf<Long>()
        val introducesNew = BooleanArray(14)
        var newest = -1

        for (daysAgo in 13 downTo 0) {
            val date = LocalDate.now().minusDays(daysAgo.toLong())
            val dayCategories = sessionsOn(recent, date, zone).mapNotNull { it.categoryId }.toSet()
            if (dayCategories.isNotEmpty()) {
                newest = 13 - daysAgo
                introducesNew[13 - daysAgo] = dayCategories.any { it !in seen }
                seen += dayCategories
            }
        }
        if (newest < 0) return 0

        var streak = 0
        for (i in newest downTo 0) {
            if (introducesNew[i]) break
            streak++
        }
        return streak
    }

    /** True only for someone who journals: three entries in the last fortnight. */
    private suspend fun reflectionMissing(zone: ZoneId, now: Long): Boolean {
        val from = now - 14L * 24 * 60 * 60 * 1000
        val entries = db.journalEntryDao().getJournalEntriesInRange(from, now).first()
        if (entries.size < 3) return false
        val today = LocalDate.now()
        return entries.none {
            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == today
        }
    }

    /** 0 when no check-in exists, otherwise 1..5 from stress and energy. */
    private suspend fun currentFatigue(now: Long): Int {
        val checkIn = db.moodCheckInDao()
            .getMoodCheckInsInRange(now - 24L * 60 * 60 * 1000, now)
            .first()
            .maxByOrNull { it.timestamp }
            ?: return 0
        val stress = checkIn.stress.coerceIn(1, 5)
        val energy = checkIn.energy.coerceIn(1, 5)
        return ((stress + (6 - energy)) / 2.0).toInt().coerceIn(1, 5)
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

    /**
     * Builds the body-system cards from what the user actually recorded in the
     * last fortnight. A domain with no records reads "Not tracked yet" instead
     * of a guess, so the screen never invents a health claim.
     */
    private suspend fun refreshBodyCards() {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = _selectedDate.value
        val (dayStart, dayEnd) = getDayRange(today, zone)
        val cats = _categories.value
        val recent = recentSessions(now, zone)
        val weekAgo = now - 7L * 24 * 60 * 60 * 1000

        val food = db.foodEntryDao().getFoodEntriesInRange(weekAgo, now)
        val drinks = db.drinkEntryDao().getDrinkEntriesInRange(weekAgo, now)
        val symptoms = db.symptomEntryDao().getSymptomsInRangeSync(weekAgo, now)
        val moods = db.moodCheckInDao().getMoodCheckInsInRange(weekAgo, now).first()

        val typeOf: (SessionEntity) -> String = { cats[it.categoryId]?.type ?: it.type }
        val sleepCount = recent.count { typeOf(it) == "SLEEP" }
        val exerciseCount = recent.count { typeOf(it) == "EXERCISE" }
        val todayMinutes = minutesFor(sessionsOn(recent, today, zone), cats, dayStart, dayEnd, now)

        val nights = recentNights(recent, zone)
        val sleepDebt = sleepDebtFrom(recent, zone)
        val sleepBand = ProductTargets.sleepHours.takeIf { it > 0f }
            ?.let { (it - 1f)..(it + 1f) } ?: 6f..9f
        val goodSleep = nights.firstOrNull()?.let { it in sleepBand } ?: false

        var noMovementDays = 0
        for (daysAgo in 0 until 21) {
            val date = today.minusDays(daysAgo.toLong())
            val has = sessionsOn(recent, date, zone).any { typeOf(it) == "EXERCISE" }
            if (has) break
            noMovementDays++
        }

        val byDay = food.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        val loggedDays = byDay.values.filter { it.isNotEmpty() }
        val sums: (List<FoodEntryEntity>) -> Triple<Double, Double, Double> = { day ->
            Triple(
                day.sumOf { it.caloriesKcal },
                day.sumOf { it.sugarG },
                day.sumOf { it.sodiumMg }
            )
        }
        val dayTotals = loggedDays.map { sums(it) }
        val caloriesToday = byDay[today]?.let { sums(it).first } ?: 0.0
        val sugarToday = byDay[today]?.let { sums(it).second } ?: 0.0
        val sodiumToday = byDay[today]?.let { sums(it).third } ?: 0.0

        val medianCalories = medianOr(dayTotals.map { it.first }, 0.0) ?: 0.0
        val medianSugar = medianOr(dayTotals.map { it.second }, 0.0) ?: 0.0
        val medianSodium = medianOr(dayTotals.map { it.third }, 0.0) ?: 0.0
        // "Below usual" only when the user logged at least three days themselves.
        val nutrientBelow: ((FoodEntryEntity) -> Double) -> Boolean = { getter ->
            val median = medianOr(loggedDays.map { day -> day.sumOf(getter) }, 0.0) ?: 0.0
            val todayValue = byDay[today]?.sumOf(getter) ?: 0.0
            median > 0 && todayValue > 0 && todayValue < median * 0.7
        }

        var wastedToday = 0.0
        var focusedToday = 0.0
        for (session in sessionsOn(recent, today, zone)) {
            val duration = sessionMinutesWithinDay(session, dayStart, dayEnd, now)
            if (duration <= 0.0) continue
            when (typeOf(session)) {
                "WASTED" -> wastedToday += duration
                "PRODUCTIVE", "LEARNING", "ACTIVITY" -> focusedToday += duration
            }
        }
        val wastedShare = if (wastedToday + focusedToday > 0) {
            wastedToday / (wastedToday + focusedToday)
        } else 0.0

        val waterTodayMl = db.drinkEntryDao().getTotalWaterMlForDay(dayStart, dayEnd).first() ?: 0.0
        val medianWater = medianOr(
            (0 until 7).mapNotNull { d ->
                val (s, e) = getDayRange(today.minusDays(d.toLong()), zone)
                val dayDrinks = drinks.filter { it.timestamp in s until e }
                if (dayDrinks.isEmpty()) null else dayDrinks.sumOf { it.volumeMl }
            },
            0.0
        ) ?: 0.0

        val painSymptom = symptoms.any { it.symptomType.contains("pain", ignoreCase = true) }
        val constipation = symptoms.any { it.symptomType.contains("constip", ignoreCase = true) }
        val latestMoods = moods.sortedByDescending { it.timestamp }.take(3)
        val persistentLowMood = latestMoods.size == 3 && latestMoods.all { it.mood <= 2 }
        val highStress = latestMoods.firstOrNull()?.let { it.stress >= 4 } ?: false

        val lowCalories = caloriesToday > 0 && medianCalories > 0 && caloriesToday < medianCalories * 0.7
        val highSugar = sugarToday > 0 && medianSugar > 0 && sugarToday > medianSugar * 1.5
        val highSodium = sodiumToday > 0 && medianSodium > 0 && sodiumToday > medianSodium * 1.3
        val lowWater = if (ProductTargets.waterMl > 0) {
            waterTodayMl < ProductTargets.waterMl * 0.5
        } else medianWater > 0 && waterTodayMl < medianWater * 0.7
        val lowFiber = if (ProductTargets.fiberG > 0) {
            food.sumOf { it.fiberG } < ProductTargets.fiberG * 0.75
        } else false

        val cards = BodySystemEngine().generateCards(
            sleepDebtHours = sleepDebt,
            lowRecovery = painSymptom && exerciseCount > 0,
            lowCalorieIntake = lowCalories,
            highSugarIntake = highSugar,
            lowMovement = noMovementDays >= 3,
            highRestingHR = false,
            goodExercise = todayMinutes.exercise >= 30.0,
            goodSleep = goodSleep,
            painAfterExercise = painSymptom,
            noMovementDays = noMovementDays,
            lowFiber = lowFiber,
            lowWater = lowWater,
            highSodium = highSodium,
            hasConstipation = constipation,
            lowIron = nutrientBelow { it.ironMg },
            lowCalcium = nutrientBelow { it.calciumMg },
            lowVitD = nutrientBelow { it.vitaminDMcg },
            lowB12 = nutrientBelow { it.vitaminB12Mcg },
            lowFolate = nutrientBelow { it.folateMcg },
            persistentLowMood = persistentLowMood,
            highStress = highStress,
            lowSleepHighWasted = sleepDebt > 8.0 && wastedShare > 0.3,
            goodLearning = todayMinutes.learning >= 30.0
        )

        val known = buildSet {
            if (sleepCount > 0) add("Sleep & Recovery")
            if (food.isNotEmpty()) add("Energy & Metabolism")
            if (exerciseCount > 0) add("Cardiovascular & Fitness")
            if (exerciseCount > 0) add("Musculoskeletal & Movement")
            if (food.isNotEmpty() || drinks.isNotEmpty()) add("Digestive & Hydration")
            if (food.isNotEmpty()) add("Nutrition & Micronutrients")
            if (moods.isNotEmpty()) add("Mood & Stress")
            if (recent.isNotEmpty()) add("Cognitive Focus")
        }

        _bodyCards.value = cards.map { card ->
            if (card.name in known) card
            else card.copy(
                status = "Unknown",
                contributingFactors = listOf("Not tracked yet"),
                trendArrow = "→",
                suggestedAction = "Track this area for a week to see it here."
            )
        }
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

    private suspend fun calculateWeeklyReport() {
        val zone = ZoneId.systemDefault()
        val today = _selectedDate.value
        val now = System.currentTimeMillis()
        val cats = _categories.value
        val report = LinkedHashMap<LocalDate, TimeCoverage>()
        var productiveSeconds = 0L
        var elapsedSeconds = 0L

        // One range query for the whole week; each day clips the same list.
        val (weekStart, _) = getDayRange(today.minusDays(6L), zone)
        val (_, todayEnd) = getDayRange(today, zone)
        val weekSessions = sessionRepo.getSessionsInRangeSync(weekStart, todayEnd)

        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val coverage = timeEngine.calculateCoverage(weekSessions, cats, date, zone, now = now)
            report[date] = coverage
            productiveSeconds += coverage.totalProductiveSeconds
            elapsedSeconds += coverage.elapsedSeconds
        }

        _weeklyReport.value = report
        // Score is against the part of the week that already happened, so
        // tonight never dilutes today.
        _productivityScore.value = if (elapsedSeconds > 0) {
            (productiveSeconds.toDouble() / elapsedSeconds).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }
        _focusGoalSeconds.value = calibrateFocusGoalSeconds(report)
    }

    private fun calibrateFocusGoalSeconds(report: Map<LocalDate, TimeCoverage>): Long {
        val userTarget = ProductTargets.productiveMinutes
        if (userTarget > 0) return userTarget * 60L
        val tracked = report.values
            .filter { it.trackedSeconds > 0 }
            .map { it.totalProductiveSeconds }
            .sorted()
        if (tracked.isEmpty()) return 0L
        return tracked[tracked.size / 2]
    }

    fun addGoal(title: String, description: String, category: String, targetHours: Float) {
        viewModelScope.launch {
            runCatching {
                goalRepo.insertGoal(
                    GoalEntity(
                        title = title,
                        description = description,
                        category = category,
                        targetHours = targetHours
                    )
                )
            }
        }
    }

    fun updateGoalProgress(goalId: Long, hoursToAdd: Float) {
        viewModelScope.launch { runCatching { goalRepo.updateGoalProgress(goalId, hoursToAdd) } }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch { runCatching { goalRepo.deleteGoal(goal) } }
    }

    fun addReminder(title: String, message: String, type: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            runCatching {
                db.reminderDao().insertReminder(
                    ReminderEntity(
                        title = title,
                        message = message,
                        type = type,
                        timeHour = hour,
                        timeMinute = minute
                    )
                )
            }
        }
    }

    fun toggleReminder(id: Long, enabled: Boolean) {
        viewModelScope.launch { runCatching { db.reminderDao().toggleReminder(id, enabled) } }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch { runCatching { db.reminderDao().deleteReminder(reminder) } }
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
