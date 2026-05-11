package com.campuscast.tvplayer.core.preview

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Base64
import com.campuscast.tvplayer.core.model.PreviewUploadPayload
import com.campuscast.tvplayer.core.model.ScreenshotRequestCommand
import com.campuscast.tvplayer.util.nowIso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference

class ScreenCaptureService {
    private companion object {
        const val DEFAULT_DISPLAY_ID = "main"
        const val DEFAULT_DISPLAY_LABEL = "Main TV Display"
    }

    private var activityRef: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (activityRef?.get() === activity) {
            activityRef?.clear()
            activityRef = null
        }
    }

    suspend fun capturePreview(request: ScreenshotRequestCommand? = null): PreviewUploadPayload {
        return withContext(Dispatchers.Main.immediate) {
            val capturedAt = nowIso()
            val displayId = request?.displayId ?: DEFAULT_DISPLAY_ID
            val displayLabel = DEFAULT_DISPLAY_LABEL
            val requestId = request?.requestId
            val activity = activityRef?.get()
            if (activity == null) {
                return@withContext PreviewUploadPayload(
                    capturedAt = capturedAt,
                    status = "capture_unavailable:no_activity",
                    displayId = displayId,
                    displayLabel = displayLabel,
                    requestId = requestId,
                )
            }

            val rootView = activity.window?.decorView?.rootView
            if (rootView == null || rootView.width <= 0 || rootView.height <= 0) {
                return@withContext PreviewUploadPayload(
                    capturedAt = capturedAt,
                    status = "capture_unavailable:view_not_ready",
                    displayId = displayId,
                    displayLabel = displayLabel,
                    requestId = requestId,
                )
            }

            return@withContext runCatching {
                val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                val output = ByteArrayOutputStream()
                try {
                    rootView.draw(Canvas(bitmap))
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    PreviewUploadPayload(
                        imageBase64 = "data:image/png;base64,$encoded",
                        mimeType = "image/png",
                        capturedAt = capturedAt,
                        width = bitmap.width,
                        height = bitmap.height,
                        status = "ok",
                        displayId = displayId,
                        displayLabel = displayLabel,
                        requestId = requestId,
                    )
                } finally {
                    output.close()
                    bitmap.recycle()
                }
            }.getOrElse { error ->
                PreviewUploadPayload(
                    capturedAt = capturedAt,
                    status = "capture_error:${error.message ?: "unknown"}",
                    displayId = displayId,
                    displayLabel = displayLabel,
                    requestId = requestId,
                )
            }
        }
    }
}
