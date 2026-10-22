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


package io.github.dailytrack.sync

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Deterministic JSON encoding used for change detection and tie-breaks.
 * org.json preserves insertion order in practice, but sorting explicitly means
 * two devices always produce the same bytes for the same record.
 */
object CanonicalJson {
    fun of(value: Any?): String = buildString { appendValue(value) }

    fun sha256(value: Any?): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(of(value).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun StringBuilder.appendValue(value: Any?) {
        when (value) {
            null, JSONObject.NULL -> append("null")
            is JSONObject -> {
                append('{')
                val keys = value.keys().asSequence().toList().sorted()
                keys.forEachIndexed { index, key ->
                    if (index > 0) append(',')
                    append(JSONObject.quote(key))
                    append(':')
                    appendValue(value.opt(key))
                }
                append('}')
            }
            is JSONArray -> {
                append('[')
                for (i in 0 until value.length()) {
                    if (i > 0) append(',')
                    appendValue(value.opt(i))
                }
                append(']')
            }
            is String -> append(JSONObject.quote(value))
            is Boolean -> append(value.toString())
            is Int, is Long, is Short, is Byte -> append(value.toString())
            is Float, is Double -> {
                val number = (value as Number).toDouble()
                if (number.isNaN() || number.isInfinite()) append("null") else append(number.toString())
            }
            is Number -> append(value.toString())
            else -> append(JSONObject.quote(value.toString()))
        }
    }
}
