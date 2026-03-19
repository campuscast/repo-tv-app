package com.campuscast.tvplayer.core.network

import com.campuscast.tvplayer.core.model.ActivationCodeResponse
import com.campuscast.tvplayer.core.model.DeviceCredentials
import com.campuscast.tvplayer.core.model.DeviceInfo
import com.campuscast.tvplayer.core.model.DevicePresenceStatus
import com.campuscast.tvplayer.core.model.Release
import com.campuscast.tvplayer.core.model.ReleaseManifest
import com.campuscast.tvplayer.core.model.TelemetryPayload
import com.campuscast.tvplayer.core.storage.appJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class BackendHttpError(
    val statusCode: Int?,
    val responseBody: String,
) : RuntimeException("HTTP ${statusCode ?: "unknown"}: ${responseBody.take(180)}")

class BackendClient(
    private val httpClient: OkHttpClient,
) {
    suspend fun requestActivationCode(apiBaseUrl: String, deviceId: String): ActivationCodeResponse {
        return postJson<Map<String, String>, ActivationCodeResponse>(
            "$apiBaseUrl/enrollment/request-code",
            mapOf("device_id" to deviceId),
            token = null,
        )
    }

    suspend fun pollCredentials(apiBaseUrl: String, deviceId: String, code: String): DeviceCredentials? {
        return try {
            getJson<DeviceCredentials>(
                "$apiBaseUrl/enrollment/credentials?device_id=${deviceId.encode()}&code=${code.encode()}",
                token = null,
            )
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun checkDeviceExists(apiBaseUrl: String, deviceId: String): DevicePresenceStatus {
        return try {
            requestActivationCode(apiBaseUrl, deviceId)
            DevicePresenceStatus.EXISTS
        } catch (error: BackendHttpError) {
            when (error.statusCode) {
                404 -> DevicePresenceStatus.MISSING
                409 -> DevicePresenceStatus.EXISTS
                else -> DevicePresenceStatus.UNKNOWN
            }
        } catch (_: Throwable) {
            DevicePresenceStatus.UNKNOWN
        }
    }

    suspend fun fetchDeviceInfo(apiBaseUrl: String, deviceToken: String, deviceId: String): DeviceInfo {
        return getJson(
            "$apiBaseUrl/player/device-info?device_id=${deviceId.encode()}",
            token = deviceToken,
        )
    }

    suspend fun fetchRelease(apiBaseUrl: String, deviceToken: String, deviceId: String): Release? {
        return try {
            getJson(
                "$apiBaseUrl/player/release?device_id=${deviceId.encode()}",
                token = deviceToken,
            )
        } catch (error: BackendHttpError) {
            if (error.statusCode == 404) null else throw error
        }
    }

    suspend fun fetchManifest(apiBaseUrl: String, deviceToken: String, releaseId: String): ReleaseManifest {
        return getJson(
            "$apiBaseUrl/player/manifest/${releaseId.encode()}",
            token = deviceToken,
        )
    }

    suspend fun sendTelemetry(apiBaseUrl: String, deviceToken: String, payload: TelemetryPayload) {
        postJson<TelemetryPayload, Unit>(
            "$apiBaseUrl/player/telemetry",
            payload,
            token = deviceToken,
            expectBody = false,
        )
    }

    private suspend inline fun <reified T> getJson(url: String, token: String?): T {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .apply {
                    if (!token.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw BackendHttpError(response.code, body)
                }
                if (T::class == Unit::class) {
                    @Suppress("UNCHECKED_CAST")
                    return@use Unit as T
                }
                if (body.isBlank()) {
                    throw BackendHttpError(response.code, "Empty response body")
                }
                return@use appJson.decodeFromString<T>(body)
            }
        }
    }

    private suspend inline fun <reified P, reified T> postJson(
        url: String,
        payload: P,
        token: String?,
        expectBody: Boolean = true,
    ): T {
        return withContext(Dispatchers.IO) {
            val requestBody = appJson.encodeToString(payload)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .apply {
                    if (!token.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw BackendHttpError(response.code, body)
                }
                if (!expectBody || T::class == Unit::class || response.code == 204 || body.isBlank()) {
                    @Suppress("UNCHECKED_CAST")
                    return@use Unit as T
                }
                return@use appJson.decodeFromString<T>(body)
            }
        }
    }
}

private fun String.encode(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
