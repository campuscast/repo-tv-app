package com.campuscast.tvplayer.core.storage

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.campuscast.tvplayer.BuildConfig
import com.campuscast.tvplayer.core.model.CrashLogInfo
import com.campuscast.tvplayer.util.nowIso
import java.io.File
import kotlin.system.exitProcess

private const val CRASH_DIR_NAME = "crash-logs"
private const val LATEST_CRASH_FILE_NAME = "latest-crash.txt"
private const val TAG = "CrashLogStore"

class CrashLogStore(private val context: Context) {
    private val appContext = context.applicationContext

    fun installHandler() {
        val current = Thread.getDefaultUncaughtExceptionHandler()
        if (current is CrashLoggingExceptionHandler) return
        Thread.setDefaultUncaughtExceptionHandler(
            CrashLoggingExceptionHandler(
                crashLogStore = this,
                delegate = current,
            ),
        )
    }

    fun readLatestCrash(): CrashLogInfo? {
        val file = latestCrashFile() ?: return null
        val content = runCatching { file.readText() }.getOrNull() ?: return null
        val capturedAtIso = content
            .lineSequence()
            .firstOrNull { it.startsWith("Timestamp: ") }
            ?.removePrefix("Timestamp: ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: java.time.Instant.ofEpochMilli(file.lastModified()).toString()
        val summary = content
            .lineSequence()
            .firstOrNull { it.startsWith("Exception: ") }
            ?.removePrefix("Exception: ")
            ?.trim()
            ?: "Unknown crash"
        val preview = content
            .lineSequence()
            .take(18)
            .joinToString("\n")
            .trim()

        return CrashLogInfo(
            filePath = file.absolutePath,
            capturedAtIso = capturedAtIso,
            summary = summary,
            preview = preview,
        )
    }

    internal fun persistCrash(thread: Thread, throwable: Throwable) {
        val timestamp = nowIso()
        val safeTimestamp = timestamp.replace(':', '-')
        val content = buildCrashText(timestamp, thread, throwable)

        writeCrashFile(File(appContext.filesDir, CRASH_DIR_NAME), safeTimestamp, content)
        appContext.getExternalFilesDir(CRASH_DIR_NAME)?.let { dir ->
            writeCrashFile(dir, safeTimestamp, content)
        }
    }

    private fun latestCrashFile(): File? {
        val external = appContext.getExternalFilesDir(CRASH_DIR_NAME)
            ?.let { File(it, LATEST_CRASH_FILE_NAME) }
            ?.takeIf(File::exists)
        if (external != null) return external

        return File(appContext.filesDir, "$CRASH_DIR_NAME/$LATEST_CRASH_FILE_NAME")
            .takeIf(File::exists)
    }

    private fun writeCrashFile(rootDir: File, safeTimestamp: String, content: String) {
        runCatching {
            if (!rootDir.exists()) {
                rootDir.mkdirs()
            }
            File(rootDir, "crash-$safeTimestamp.txt").writeText(content)
            File(rootDir, LATEST_CRASH_FILE_NAME).writeText(content)
        }.onFailure {
            Log.e(TAG, "Failed to write crash log to ${rootDir.absolutePath}", it)
        }
    }

    private fun buildCrashText(timestamp: String, thread: Thread, throwable: Throwable): String {
        return buildString {
            appendLine("Timestamp: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine("Exception: ${throwable::class.java.name}: ${throwable.message ?: "no message"}")
            appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Package: ${BuildConfig.APPLICATION_ID}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Brand/Product: ${Build.BRAND} / ${Build.PRODUCT}")
            appendLine()
            appendLine("Stacktrace:")
            appendLine(Log.getStackTraceString(throwable))
        }
    }
}

private class CrashLoggingExceptionHandler(
    private val crashLogStore: CrashLogStore,
    private val delegate: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            crashLogStore.persistCrash(thread, throwable)
        }
        delegate?.uncaughtException(thread, throwable) ?: run {
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}
