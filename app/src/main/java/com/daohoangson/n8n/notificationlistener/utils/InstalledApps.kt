package com.daohoangson.n8n.notificationlistener.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
)

/**
 * The apps the owner can choose to listen to: everything with a launcher entry.
 * Visible through the manifest's launcher-intent `<queries>`, so the app does not
 * need QUERY_ALL_PACKAGES. Bank apps always have a launcher entry.
 *
 * Blocking (PackageManager + icon rasterising): call off the main thread.
 */
object InstalledApps {
    // 40dp at xxxhdpi.
    private const val ICON_PX = 160

    fun load(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0))
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    icon = pm.getApplicationIcon(info).toBitmap(ICON_PX, ICON_PX).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
