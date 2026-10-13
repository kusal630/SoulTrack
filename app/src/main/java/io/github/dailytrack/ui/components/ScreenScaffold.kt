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


package io.github.dailytrack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.dailytrack.ui.theme.Dimens

/**
 * The one way every screen in the app builds its chrome.
 *
 * A transparent top bar over the AMOLED background, a large (or compact) title,
 * an optional subtitle, back navigation and action slots - so headers never
 * drift screen to screen.
 *
 * @param navController when given and [onBack] is null, the back arrow pops.
 * @param compact use a 64dp bar instead of the collapsing large one, for
 *   content-heavy screens where vertical space matters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navController: NavController? = null,
    onBack: (() -> Unit)? = null,
    compact: Boolean = false,
    topBarContainerColor: Color = Color.Transparent,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val backAction: (() -> Unit)? = onBack ?: navController?.let { controller ->
        ({ controller.popBackStack() })
    }

    val titleText: @Composable (Color) -> Unit = { color ->
        Column {
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (compact) {
                CompactBar(
                    titleText = titleText,
                    backAction = backAction,
                    containerColor = topBarContainerColor,
                    actions = actions
                )
            } else {
                LargeBar(
                    titleText = titleText,
                    backAction = backAction,
                    containerColor = topBarContainerColor,
                    actions = actions
                )
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        content(padding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactBar(
    titleText: @Composable (Color) -> Unit,
    backAction: (() -> Unit)?,
    containerColor: Color,
    actions: @Composable RowScope.() -> Unit
) {
    val scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    TopAppBar(
        title = { titleText(MaterialTheme.colorScheme.onSurface) },
        navigationIcon = { BackIcon(backAction) },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LargeBar(
    titleText: @Composable (Color) -> Unit,
    backAction: (() -> Unit)?,
    containerColor: Color,
    actions: @Composable RowScope.() -> Unit
) {
    val scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    LargeTopAppBar(
        title = { titleText(MaterialTheme.colorScheme.onSurface) },
        navigationIcon = { BackIcon(backAction) },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun BackIcon(backAction: (() -> Unit)?) {
    if (backAction == null) return
    IconButton(onClick = backAction) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * [ScreenScaffold] for list content: horizontal inset, 12dp rhythm and a
 * stable vertical content padding, already applied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenList(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navController: NavController? = null,
    onBack: (() -> Unit)? = null,
    compact: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit
) {
    ScreenScaffold(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        navController = navController,
        onBack = onBack,
        compact = compact,
        actions = actions,
        floatingActionButton = floatingActionButton
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            content()
        }
    }
}

/** A screen header rendered inside existing content rather than as a top bar. */
@Composable
fun InlineHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        action()
    }
}
