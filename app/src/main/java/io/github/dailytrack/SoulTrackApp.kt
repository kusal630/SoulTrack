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


package io.github.dailytrack

import android.app.Application
import io.github.dailytrack.data.api.QuotesApi
import io.github.dailytrack.data.db.AppDatabase
import io.github.dailytrack.sync.SyncManager
import io.github.dailytrack.sync.SyncRepository
import io.github.dailytrack.worker.DeadlineCheckWorker
import io.github.dailytrack.service.TimerStateStore
import io.github.dailytrack.widget.QuoteWidgetProvider
import io.github.dailytrack.widget.TimerWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoulTrackApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(this, database)
    }

    val syncManager: SyncManager by lazy {
        SyncManager(this, syncRepository)
    }

    override fun onCreate() {
        super.onCreate()
        QuotesApi.init(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { QuotesApi.maybeRefreshQuotes() }
            // Every reload surfaces a fresh flagship quote across both widgets.
            runCatching {
                val quote = QuotesApi.rotateDisplayQuote()
                QuoteWidgetProvider.showQuote(this@SoulTrackApp, quote.text, quote.author)
                TimerWidgetProvider.showQuote(this@SoulTrackApp, quote.text, quote.author)
            }
            // Converge the widget/service/UI on whichever session is really running.
            runCatching {
                TimerStateStore.reconcile(this@SoulTrackApp)
                TimerWidgetProvider.updateWidgets(this@SoulTrackApp)
            }
        }
        DeadlineCheckWorker.schedule(this)
    }
}
