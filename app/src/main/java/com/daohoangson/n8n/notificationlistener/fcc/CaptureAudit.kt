package com.daohoangson.n8n.notificationlistener.fcc

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TEMPORARY diagnostic for the multi-bank rollout validation week (#66).
 *
 * Appends one JSON line per processed notification from a **tracked** financial
 * app — whether captured or dropped — to `<externalFilesDir>/fcc-audit.jsonl`, so
 * a week of real usage can be reviewed off-device (`adb pull`). This closes the
 * blind spot that a *dropped* notification is otherwise only a logcat line, lost
 * when the ring buffer rolls over — the one thing we need to catch is a real
 * purchase that was wrongly dropped. Untracked-app drops are deliberately NOT
 * recorded (they are high-volume and privacy-sensitive — every chat, email, etc.).
 *
 * REMOVE once the rollout is validated: delete this file and its two call sites in
 * [com.daohoangson.n8n.notificationlistener.NotificationListenerService]. It lives
 * on the throwaway `fcc/rollout-audit-temp` branch, never on `fcc/ingest-capture`.
 *
 * Pull the log with:
 *   adb pull /sdcard/Android/data/com.daohoangson.n8n.notificationlistener/files/fcc-audit.jsonl
 */
object CaptureAudit {
    private const val TAG = "FccNotificationListener"
    private const val FILE_NAME = "fcc-audit.jsonl"
    private val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

    /** @param outcome "captured" or "dropped". @param detail account+amount summary, or the drop reason. */
    fun record(
        context: Context,
        packageName: String?,
        title: String?,
        text: String?,
        outcome: String,
        detail: String,
    ) {
        try {
            val line = JSONObject()
                .put("at", timestamp.format(Date()))
                .put("package", packageName ?: "")
                .put("outcome", outcome)
                .put("detail", detail)
                .put("title", title ?: "")
                .put("text", text ?: "")
                .toString()
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            File(dir, FILE_NAME).appendText(line + "\n")
        } catch (e: Exception) {
            Log.e(TAG, "audit write failed", e)
        }
    }
}
