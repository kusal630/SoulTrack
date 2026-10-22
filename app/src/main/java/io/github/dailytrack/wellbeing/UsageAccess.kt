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


package io.github.dailytrack.wellbeing

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * Whether the app may read on-device app usage.
 *
 * Usage access is the only way to account for time the user never opened
 * Soul Track for. Without it that time simply stays untracked (and therefore
 * counts as waste). Everything read through it stays on this device.
 */
object UsageAccess {
    fun has(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Settings screen that grants usage access. */
    fun settingsIntent(context: Context): Intent {
        val appSpecific = Intent(
            Settings.ACTION_USAGE_ACCESS_SETTINGS,
            android.net.Uri.parse("package:${context.packageName}")
        )
        return if (appSpecific.resolveActivity(context.packageManager) != null) {
            appSpecific
        } else {
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        }
    }
}
