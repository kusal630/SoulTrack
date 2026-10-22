package io.github.dailytrack.sync

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.dailytrack.data.db.AppDatabase
import io.github.dailytrack.data.db.entity.CategoryEntity
import io.github.dailytrack.data.db.entity.PomodoroSessionEntity
import io.github.dailytrack.data.db.entity.SessionEntity
import io.github.dailytrack.data.db.entity.SubtaskEntity
import io.github.dailytrack.data.db.entity.TodoEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class P2pSyncStoreTest {
    private lateinit var dbA: AppDatabase
    private lateinit var dbB: AppDatabase
    private lateinit var storeA: P2pSyncStore
    private lateinit var storeB: P2pSyncStore

    private fun clock(startWall: Long, saved: Array<String?>): HlcClock = HlcClock(
        load = { saved[0] },
        save = { saved[0] = it },
        wallClock = { startWall }
    )

    @Before
    fun setUp() {
        dbA = Room.inMemoryDatabaseBuilder(context(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dbB = Room.inMemoryDatabaseBuilder(context(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storeA = P2pSyncStore(dbA, clock(1_000L, arrayOfNulls(1)))
        storeB = P2pSyncStore(dbB, clock(1_000L, arrayOfNulls(1)))
    }

    @After
    fun tearDown() {
        dbA.close()
        dbB.close()
    }

    @Test
    fun `first sync with identical rows keeps one row and one id`() = runBlocking {
        dbA.todoDao().insert(TodoEntity(title = "Write report", createdAt = 500L))
        dbB.todoDao().insert(TodoEntity(title = "Write report", createdAt = 500L))

        storeA.reconcileLocal()
        storeB.reconcileLocal()
        val snapshotA = storeA.snapshot()
        val snapshotB = storeB.snapshot()

        storeA.mergeRemote(snapshotB)
        storeB.mergeRemote(snapshotA)
        storeA.reconcileLocal()
        storeB.reconcileLocal()

        val todoA = storeA.snapshot().single { it.entityType == SyncTypes.TODO }
        val todoB = storeB.snapshot().single { it.entityType == SyncTypes.TODO }
        assertEquals(todoA.syncId, todoB.syncId)
        assertEquals(1, dbA.todoDao().getAllTodosSync().size)
        assertEquals(1, dbB.todoDao().getAllTodosSync().size)
    }

    @Test
    fun `newer remote edit wins after identity adoption`() = runBlocking {
        val localId = dbA.todoDao().insert(TodoEntity(title = "Old", createdAt = 500L))
        storeA.reconcileLocal()
        val local = storeA.snapshot().single { it.entityType == SyncTypes.TODO }

        val remoteId = "00000000-0000-0000-0000-000000000001"
        val remote = SyncRecord(
            syncId = remoteId,
            entityType = SyncTypes.TODO,
            hlc = Hlc(2_000L, 0).encode(),
            naturalKey = local.naturalKey,
            deleted = false,
            payload = JSONObject().apply {
                put("title", "New")
                put("description", "")
                put("createdAt", 500L)
                put("updatedAt", 2_000L)
            }
        )
        storeA.mergeRemote(listOf(remote))

        val result = storeA.snapshot().single { it.entityType == SyncTypes.TODO }
        assertEquals(remoteId, result.syncId)
        assertNotEquals(local.syncId, result.syncId)
        assertEquals(1, dbA.todoDao().getAllTodosSync().size)
        assertEquals("New", dbA.todoDao().getTodoById(localId)?.title)
    }

    @Test
    fun `deletion propagates and never resurrects`() = runBlocking {
        val todoA = dbA.todoDao().insert(TodoEntity(title = "Shared", createdAt = 700L))
        dbB.todoDao().insert(TodoEntity(title = "Shared", createdAt = 700L))
        storeA.reconcileLocal()
        storeB.reconcileLocal()
        storeA.mergeRemote(storeB.snapshot())
        storeB.mergeRemote(storeA.snapshot())
        storeA.reconcileLocal()
        storeB.reconcileLocal()

        val todo = dbA.todoDao().getTodoById(todoA) ?: dbA.todoDao().getAllTodosSync().single()
        dbA.todoDao().delete(todo)
        storeA.reconcileLocal()
        storeB.mergeRemote(storeA.snapshot())
        storeB.reconcileLocal()

        assertTrue(storeA.snapshot().single { it.entityType == SyncTypes.TODO }.deleted)
        assertTrue(storeB.snapshot().single { it.entityType == SyncTypes.TODO }.deleted)
        assertEquals(0, dbA.todoDao().getAllTodosSync().size)
        assertEquals(0, dbB.todoDao().getAllTodosSync().size)

        storeA.mergeRemote(storeB.snapshot())
        assertEquals(0, dbA.todoDao().getAllTodosSync().size)
    }

    @Test
    fun `recreated row is not blocked by an older tombstone`() = runBlocking {
        val localId = dbA.categoryDao().insert(
            CategoryEntity(name = "Work", type = "PRODUCTIVE", createdAt = 500L)
        )
        storeA.reconcileLocal()
        val original = storeA.snapshot().single { it.entityType == SyncTypes.CATEGORY }
        dbA.categoryDao().getCategoryById(localId)?.let { dbA.categoryDao().delete(it) }
        storeA.reconcileLocal()
        assertTrue(storeA.snapshot().single { it.entityType == SyncTypes.CATEGORY }.deleted)

        val recreatedId = "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz"
        storeA.mergeRemote(
            listOf(
                SyncRecord(
                    syncId = recreatedId,
                    entityType = SyncTypes.CATEGORY,
                    hlc = Hlc(2_000L, 0).encode(),
                    naturalKey = original.naturalKey,
                    deleted = false,
                    payload = JSONObject().apply {
                        put("name", "Work")
                        put("type", "PRODUCTIVE")
                        put("createdAt", 1_500L)
                    }
                )
            )
        )

        val active = storeA.snapshot().filter {
            it.entityType == SyncTypes.CATEGORY && !it.deleted
        }
        assertEquals(1, active.size)
        assertEquals(recreatedId, active.single().syncId)
    }

    @Test
    fun `every synced entity type keeps its relationships on the other device`() = runBlocking {
        val categoryA = dbA.categoryDao().insert(
            CategoryEntity(name = "Deep work", type = "PRODUCTIVE", createdAt = 500L)
        )
        val todoA = dbA.todoDao().insert(
            TodoEntity(title = "Ship release", categoryId = categoryA, createdAt = 600L)
        )
        dbA.subtaskDao().insert(
            SubtaskEntity(todoId = todoA, title = "Write notes", createdAt = 700L)
        )
        dbA.sessionDao().insert(
            SessionEntity(
                title = "Coding",
                categoryId = categoryA,
                todoId = todoA,
                startTime = 1_000L,
                endTime = 2_000L,
                createdAt = 1_000L
            )
        )
        dbA.pomodoroSessionDao().insert(
            PomodoroSessionEntity(
                todoId = todoA,
                categoryId = categoryA,
                startTime = 3_000L,
                endTime = 4_000L,
                isCompleted = true,
                createdAt = 3_000L
            )
        )
        storeA.reconcileLocal()
        storeB.reconcileLocal()

        storeB.mergeRemote(storeA.snapshot())
        storeB.reconcileLocal()
        storeA.mergeRemote(storeB.snapshot())
        storeA.reconcileLocal()
        storeB.mergeRemote(storeA.snapshot())
        storeB.reconcileLocal()

        val categoryB = dbB.categoryDao().getAllCategoriesSync().single()
        val todoB = dbB.todoDao().getAllTodosSync().single()
        val subtaskB = dbB.subtaskDao().getAllSubtasksSync().single()
        val sessionB = dbB.sessionDao().getAllSessionsSync().single()
        val pomodoroB = dbB.pomodoroSessionDao().getAllPomodorosSync().single()

        assertEquals("Deep work", categoryB.name)
        assertEquals(categoryB.id, todoB.categoryId)
        assertEquals(todoB.id, subtaskB.todoId)
        assertEquals(categoryB.id, sessionB.categoryId)
        assertEquals(todoB.id, sessionB.todoId)
        assertEquals(categoryB.id, pomodoroB.categoryId)
        assertEquals(todoB.id, pomodoroB.todoId)
        assertEquals(5, storeB.snapshot().size)
    }

    @Test
    fun `active timers are not copied until they finish`() = runBlocking {
        val sessionId = dbA.sessionDao().insert(
            SessionEntity(
                title = "Running now",
                startTime = 5_000L,
                isActive = true,
                createdAt = 5_000L
            )
        )
        val pomodoroId = dbA.pomodoroSessionDao().insert(
            PomodoroSessionEntity(
                startTime = 6_000L,
                isCompleted = false,
                createdAt = 6_000L
            )
        )
        storeA.reconcileLocal()
        assertEquals(0, storeA.snapshot().size)

        val session = dbA.sessionDao().getSessionById(sessionId)!!
        dbA.sessionDao().update(
            session.copy(endTime = 8_000L, isActive = false, updatedAt = 8_000L)
        )
        val pomodoro = dbA.pomodoroSessionDao().getPomodoroById(pomodoroId)!!
        dbA.pomodoroSessionDao().update(
            pomodoro.copy(endTime = 9_000L, isCompleted = true)
        )
        storeA.reconcileLocal()

        val types = storeA.snapshot().map { it.entityType }.toSet()
        assertEquals(setOf(SyncTypes.SESSION, SyncTypes.POMODORO), types)
    }

    @Test
    fun `multiple tombstones for unknown rows are retained`() = runBlocking {
        storeA.mergeRemote(
            listOf(
                SyncRecord(
                    syncId = "10000000-0000-0000-0000-000000000001",
                    entityType = SyncTypes.CATEGORY,
                    hlc = Hlc(2_000L, 0).encode(),
                    naturalKey = "one|PRODUCTIVE",
                    deleted = true,
                    payload = null
                ),
                SyncRecord(
                    syncId = "20000000-0000-0000-0000-000000000002",
                    entityType = SyncTypes.CATEGORY,
                    hlc = Hlc(2_001L, 0).encode(),
                    naturalKey = "two|PRODUCTIVE",
                    deleted = true,
                    payload = null
                )
            )
        )

        val tombstones = storeA.snapshot().filter {
            it.entityType == SyncTypes.CATEGORY && it.deleted
        }
        assertEquals(2, tombstones.size)
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()
}
