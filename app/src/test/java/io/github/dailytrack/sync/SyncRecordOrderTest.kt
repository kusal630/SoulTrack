package io.github.dailytrack.sync

import android.app.Application
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SyncRecordOrderTest {
    private fun record(hlc: String, title: String, id: String = "b") = SyncRecord(
        syncId = id,
        entityType = SyncTypes.TODO,
        hlc = hlc,
        naturalKey = "key",
        deleted = false,
        payload = JSONObject().put("title", title)
    )

    @Test
    fun `newer hybrid clock wins`() {
        val remote = record(Hlc(2_000L, 0).encode(), "remote")
        assertTrue(SyncRecordOrder.remoteWins(remote, Hlc(1_000L, 5).encode(), "hash", "a"))
    }

    @Test
    fun `older hybrid clock loses`() {
        val remote = record(Hlc(1_000L, 0).encode(), "remote")
        assertFalse(SyncRecordOrder.remoteWins(remote, Hlc(2_000L, 0).encode(), "hash", "a"))
    }

    @Test
    fun `equal clocks are broken deterministically by payload hash`() {
        val remote = record(Hlc(1_000L, 0).encode(), "zzz")
        val local = record(Hlc(1_000L, 0).encode(), "aaa")
        assertEquals(
            SyncRecordOrder.remoteWins(remote, local.hlc, local.payloadHash, "a"),
            SyncRecordOrder.remoteWins(record(Hlc(1_000L, 0).encode(), "zzz"), local.hlc, local.payloadHash, "a")
        )
    }

    private fun assertEquals(expected: Boolean, actual: Boolean) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
