package io.github.dailytrack.sync

import android.app.Application
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class CanonicalJsonTest {
    @Test
    fun `key order does not change the hash`() {
        val a = JSONObject().apply {
            put("title", "Write report")
            put("priority", 3)
            put("done", false)
        }
        val b = JSONObject().apply {
            put("done", false)
            put("priority", 3)
            put("title", "Write report")
        }
        assertEquals(CanonicalJson.sha256(a), CanonicalJson.sha256(b))
    }

    @Test
    fun `array order matters`() {
        val a = JSONObject().put("items", JSONArray().put("a").put("b"))
        val b = JSONObject().put("items", JSONArray().put("b").put("a"))
        assertNotEquals(CanonicalJson.sha256(a), CanonicalJson.sha256(b))
    }

    @Test
    fun `null is encoded consistently`() {
        val a = JSONObject().put("deadline", JSONObject.NULL)
        val b = JSONObject("""{"deadline":null}""")
        assertEquals(CanonicalJson.of(a), CanonicalJson.of(b))
    }
}
