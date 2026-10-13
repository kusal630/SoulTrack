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


package io.github.dailytrack.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape tokens. Cards are soft 24dp slabs, every button is a full pill, and
 * touch targets never drop below 48dp (see [Dimens.MinTouchTarget]).
 */
val SoulTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Shape helpers used by buttons, chips and sheets. */
object SoulShapes {
    /** Fully rounded pill - buttons, chips, toggles. */
    val Pill = RoundedCornerShape(50)

    /** Card radius. */
    val Card = RoundedCornerShape(24.dp)

    /** Nested element inside a card (rows, fields). */
    val Inner = RoundedCornerShape(16.dp)

    /** Bottom sheet top corners. */
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}
