package io.github.dailytrack.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HlcTest {
    @Test
    fun `same millisecond gets an increasing counter`() {
        val saved = arrayOfNulls<String>(1)
        val clock = HlcClock(
            load = { saved[0] },
            save = { saved[0] = it },
            wallClock = { 1_000L }
        )
        val first = clock.next()
        val second = clock.next()

        assertEquals(1_000L, first.wall)
        assertEquals(0, first.counter)
        assertEquals(1, second.counter)
        assertTrue(second > first)
    }

    @Test
    fun `remote clock is observed and never moves backwards`() {
        val saved = arrayOfNulls<String>(1)
        val clock = HlcClock(
            load = { saved[0] },
            save = { saved[0] = it },
            wallClock = { 500L }
        )
        clock.observe(Hlc(9_000L, 3))
        val next = clock.next()

        assertEquals(9_000L, next.wall)
        assertEquals(4, next.counter)
    }

    @Test
    fun `encoding round trips`() {
        val original = Hlc(1_700_000_000_000L, 42)
        assertEquals(original, Hlc.parse(original.encode()))
        assertEquals(Hlc.ZERO, Hlc.parse("nonsense"))
    }
}
