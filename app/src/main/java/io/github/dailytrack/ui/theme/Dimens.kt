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

import androidx.compose.ui.unit.dp

/**
 * Spacing tokens. Everything snaps to an 8dp grid (with a 4dp half step for
 * tight internals) so rhythm stays consistent across screens.
 */
object Dimens {
    /** One grid unit. */
    val Grid = 8.dp

    /** Screen edge inset. */
    val ScreenPadding = 16.dp

    /** Gap between top level sections of a screen. */
    val SectionGap = 24.dp

    /** Inner padding of a card. */
    val CardPadding = 16.dp

    /** Gap between sibling cards. */
    val CardGap = 16.dp

    /** Gap inside a card between title and body. */
    val TitleGap = 8.dp

    /** Minimum touch target (Material 3). */
    val MinTouchTarget = 48.dp

    /** Comfortable primary button height. */
    val ButtonHeight = 56.dp

    /** Height of the bottom navigation bar. */
    val BottomBarHeight = 80.dp

    /** Hero SoulRings on the Today screen. */
    val HeroRing = 260.dp

    /** Compact SoulRings used in history rows and the widget preview. */
    val MiniRing = 72.dp

    /**
     * Widest a single column of content may grow. On phones the screen is
     * already narrower than this; on tablet portrait it stops task cards and
     * insights from stretching into unreadable full-bleed slabs.
     */
    val ContentMaxWidth = 720.dp
}

/**
 * Animation timing from DESIGN.md.
 */
object Motion {
    const val Micro = 150
    const val Standard = 260
    const val Slow = 420
}
